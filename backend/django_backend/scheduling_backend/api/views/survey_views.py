from rest_framework.decorators import api_view, parser_classes
from rest_framework.response import Response
from rest_framework.parsers import FormParser, MultiPartParser, JSONParser
from rest_framework import status
from rest_framework.exceptions import APIException
from rest_framework.request import Request
from django.db.models import Q
from django.core.exceptions import ObjectDoesNotExist
import pandas as pd
from ..models import Survey
from ..serializer import TeacherSerializer, SurveyFileSerializer, SurveySerializer
from .helper.file_reader import file_to_df
from .helper.survey_helpers import find_teacher, find_survey

@api_view(["GET"])
@parser_classes([JSONParser])
def survey_base_endpoint(request: Request) -> Response:
    """Retrives suvery instances based on given query parameters. If no query
    parameters are in the url. Than all survey instance are retrived.
    Query parameters are: name, term, email, and all_flag.
    Survey instances will be retrived if they have any of the query params are valid. If all
    query params but be true for the retrived survey instances then the all_flag param should
    have the value "true"

    Args:
        request (Request): request payload. Ignored in this func

    Returns:
        Response: retrived survey instances (0+)
    """

    def merge_Qs(q_obj: Q, add_q: Q, all: str) -> Q:
        """Merges together two ``Q`` objects 

        Args:
            q_obj (Q): ``Q`` object we are adding to
            add_q (Q): ``Q`` object to be added
            all (bool): if we want to OR or AND the ``q_obj`` with ``add_q``

        Returns:
            Q: returns the merged result of ``q_obj`` and ``add_q``
        """
        if q_obj is None:
                return add_q
        if all == "true":
            
            return q_obj & add_q
        else:
            return q_obj | add_q


    PARM_NAME = request.query_params.get("name")
    PARM_TERM = request.query_params.get("term")
    PARM_EMAIL = request.query_params.get("email")
    #if all query params should be true
    PARAM_ALL_FLAG = request.query_params.get("all_flag", "false").lower()

    query = Q()
    if PARM_TERM is not None:
        query = merge_Qs(query, Q(cur_term=PARM_TERM), PARAM_ALL_FLAG)
    if PARM_NAME is not None:
        query = merge_Qs(query, Q(name=PARM_NAME), PARAM_ALL_FLAG)
    if PARM_EMAIL is not None:
        query = merge_Qs(query, Q(email=PARM_EMAIL), PARAM_ALL_FLAG)

    query_res = Survey.objects.filter(query)
    res_serialized = SurveySerializer(query_res, many=True)
    
    return Response(res_serialized.data, status.HTTP_200_OK)




@api_view(["DELETE"])
def delete_survey_instance(request, pk) -> Response:
    """Attempts to delete a survey with the given primary key in the url parameter

    Args:
        request (_type_): payload; ignored in function
        pk (_type_): primary key of survey instance trying to be deleted

    Returns:
        Response: returns a json object with msg and boolean indicating wether
        the instance was deleted
    """
    try:
        del_survey = Survey.objects.get(id=pk)
        del_survey.delete()
        return Response({"msg": f"Deleted survey with primary key '{pk}'",
                         "deleted": True},
                            status.HTTP_200_OK)
    except ObjectDoesNotExist as e:
        return Response({"msg": f"{e}",
                         "deleted": False}, status.HTTP_404_NOT_FOUND)
    except Exception as e:
        return Response({"msg": "Probably a server error",
                         "dev_msg": f"{e}",
                         "deleted": False},
                         status.HTTP_500_INTERNAL_SERVER_ERROR)




@api_view(["POST"])
@parser_classes([FormParser, MultiPartParser])
def survey_file_upload(request: Request) -> Response:
    """Allows for a file upload (csv, tsv, or excel) and will create a survey instance for survey response in the file. If a 
    survey instance already exists for the a term and teacher name combo, the previous instance will be replaced with the new one. If
    a survey bleeds forward, **only** the time **avaiability** fields will be replaced.

    Args:
        request (Request): _description_

    Raises:
        APIException: _description_
        APIException: _description_

    Returns:
        Response: payload indicating what survey entries where created/updated or failed
    """

    #note that the name in the survey is expected to be the non-canon name
    #field names have the same name as the model field names
    AVAIL_FIELDS = ["mwf_7_am", "mwf_8_am", "mwf_9_am", "mwf_10_am", "mwf_11_am", "mwf_12_pm", "mwf_1_pm", "mwf_2_pm",
                            "mwf_3_pm", "mwf_4_pm", "mwf_5_pm", "mwf_6_pm", "mwf_7_pm", "mwf_8_pm", "mwf_9_pm",
                            "tr_7_am", "tr_8_am", "tr_9_am", "tr_10_am", "tr_11_am", "tr_12_pm", "tr_1_pm", "tr_2_pm", 
                            "tr_3_pm", "tr_4_pm", "tr_5_pm", "tr_6_pm", "tr_7_pm", "tr_8_pm", "tr_9_pm"]
    PREF_FIELDS = ["mwf_1", "tr_1", "mwf_2", "mwf_tr", "tr_2", "mwf_3", "mwf_2_tr_1", "mwf_1_tr_2", "tr_3", "mwrf", "mtwr", "mw",
                   "tr", "back_to_back", "gap"]
    ADDITIONAL_FIELDS = ["constraint", "require", "pref", "comment", "stars"]
    #order of array extension here matters
    EMAIL_FILED = "email"
    NAME_FIELD = "name"
    BLEEDS_FORWARD_FIELD = "use_old"
    REPLACEMENT_FIELDNAMES = ["id", "start", "complete", EMAIL_FILED, NAME_FIELD, BLEEDS_FORWARD_FIELD]
    REPLACEMENT_FIELDNAMES.extend(AVAIL_FIELDS)
    REPLACEMENT_FIELDNAMES.extend(PREF_FIELDS)
    REPLACEMENT_FIELDNAMES.extend(ADDITIONAL_FIELDS)
    DROP_FIELDS = ["id", "start", "complete"]
    DATA_FIELDS = [FIELD for FIELD in REPLACEMENT_FIELDNAMES if not FIELD in DROP_FIELDS]
    #default values are essentially no for pref/avail empty values
    DEFAULT_AVAIL = "Conflict"
    DEFAULT_PREF = "Disagree"
    BLEED_FORWARD_STRING = "Yes, use the same as last term"

    #Read/normalize file
    serializer = SurveyFileSerializer(data=request.data)
    #if invalid it will return a HTTP_4XX status code
    serializer.is_valid(raise_exception=True)
    print(serializer.data)
    print(serializer.validated_data)
    try:
        VALID_EXTENSIONS = ['tsv', 'csv', 'xlsx', 'xlsm', 'xlsb']
        FILE = serializer.validated_data["file"]
        CUR_TERM = serializer.validated_data["cur_term"]
        PREV_TERM = serializer.validated_data["prev_term"]

        df = file_to_df(FILE, VALID_EXTENSIONS)
        
        #check number of columns expected
        if len(df.columns) != len(REPLACEMENT_FIELDNAMES):
            raise APIException(detail=f"Number of columns expected is {len(REPLACEMENT_FIELDNAMES)}; " +
                                f"given {len(df.columns)}")
        #normalize df
        df.columns = REPLACEMENT_FIELDNAMES
        df.drop(DROP_FIELDS, axis=1, inplace=True)
        for drop in DROP_FIELDS:
            REPLACEMENT_FIELDNAMES.remove(drop)
        for col in df.columns:
            #normalize columns
            df[col] = (
                df[col]
                .fillna("")
                .astype(str)
                .str.strip()
            )
        #normalize time availability/pref fields; blank fields marked as a conflict
        for field in AVAIL_FIELDS:
            df[field] = (
                df[field]
                .replace({"": DEFAULT_AVAIL})
            )
            
        for field in PREF_FIELDS:
            df[field] = (
                df[field]
                .replace({"": DEFAULT_PREF})
            )

    except Exception as e:
        return Response({"error": f"problem reading the file {FILE.name}",
                            "msg": f"{e}"}, status.HTTP_500_INTERNAL_SERVER_ERROR)
    
    success = []
    failed = []
    for idx, row in df.iterrows():
        try:
            #package survey entry
            #NOTE garunteed name field
            data = {k: v for k, v in zip(DATA_FIELDS, row)}
            SURVEY_NAME = data[NAME_FIELD].strip()
            BLEED_SURVEY = BLEED_FORWARD_STRING == data[BLEEDS_FORWARD_FIELD]
            if SURVEY_NAME == "":
                failed.append({
                    "msg": "Survey entry had no name",
                    "teacher_file_idx": idx
                })
                continue
            #SKIP generic timeslots
            if "generic" in SURVEY_NAME.lower():
                failed.append({
                    "msg": f"SKIPPED: survey entry with name '{SURVEY_NAME}' skiped to being GENERIC",
                    "teacher_file_idx": idx,
                })
                continue


            #find a teacher entity for given name (assumes name is non_canon)
            query_gen_dict = {"non_canon": SURVEY_NAME}
            #NOTE beard said he would email fields
            if(data["email"] != ""):
                query_gen_dict["email"] = data["email"] 
            #raises an exception if nothing is found
            teacher = find_teacher(**query_gen_dict)
            teacher_serializer = TeacherSerializer(teacher)

            #Add additional survey model fields
            data = {**data, "cur_term": CUR_TERM, "prev_term": PREV_TERM, "use_old": BLEED_SURVEY, "teacher": teacher.id}

            #check teacher has a suvrvey for the term. If one exists then replace it
            potential = find_survey(CUR_TERM, teacher)
            cur_term_survey = (potential,) if potential is not None else ()
            
            #if bleeds forward replace data for "avaialbility" fields
            if BLEED_SURVEY:
                #make func that returns fields we care about and throw error if no prev survey found
                prev_survey = find_survey(PREV_TERM, teacher)
                #skip entry creation if no previous survey was found
                if prev_survey is None:
                    failed.append({"msg": f"suvery bled forward, but no previous survey found for the {PREV_TERM} term",
                                "teacher_file_idx": idx,
                                "teacher": teacher_serializer.data})
                    continue

                prev_serializer = SurveySerializer(prev_survey)
                prev_data = prev_serializer.data
                
                #replace 
                for field in AVAIL_FIELDS:
                    '''
                    TODO here we could add smarter logic perhaps if Beard agrees only fill in fields left empty
                    this would change how fill out empty strings in the df for AVAIL/PREF fields'''
                    data[field] = prev_data[field]

            survey_serializer = SurveySerializer(*cur_term_survey, data=data)
            
            survey_serializer.is_valid(raise_exception=True)
            survey_serializer.save()
            success.append({"msg": "teacher",
                            "suvey_id": survey_serializer.data["id"],
                            "creation": potential is None,
                            "teacher_file_idx": idx,
                            "teacher": teacher_serializer.data})
            
        except APIException as e:
            failed.append({"msg": f"{e}",
                           "teacher_file_idx": idx,
                           "associated_data": query_gen_dict})
        except Exception as e:
            failed.append({"msg": "Most likely server side error handling suvery entry",
                           "dev_error": f"{e}",
                           "teacher_file_idx": idx,
                           })
        
    return Response({"msg": "finish endpoint",
                     "success": success,
                     "failed": failed},
                       status.HTTP_200_OK)
    