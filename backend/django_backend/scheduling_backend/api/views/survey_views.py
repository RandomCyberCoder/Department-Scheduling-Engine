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

    query_res = Survey.objects.select_related("teacher").filter(query)
    res_serialized = SurveySerializer(query_res, many=True)
    ret_data = res_serialized.data
    for data in ret_data:
        data["teacher"] = data["teacher"]
    return Response(ret_data, status.HTTP_200_OK)




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
    AVAIL_FIELDS = [
                    "M_7_AM","M_8_AM","M_9_AM","M_10_AM","M_11_AM","M_12_PM",
                    "M_1_PM","M_2_PM","M_3_PM","M_4_PM","M_5_PM","M_6_PM",
                    "M_7_PM","M_8_PM","M_9_PM",

                    "T_7_AM","T_8_AM","T_9_AM","T_10_AM","T_11_AM","T_12_PM",
                    "T_1_PM","T_2_PM","T_3_PM","T_4_PM","T_5_PM","T_6_PM",
                    "T_7_PM","T_8_PM","T_9_PM",

                    "W_7_AM","W_8_AM","W_9_AM","W_10_AM","W_11_AM","W_12_PM",
                    "W_1_PM","W_2_PM","W_3_PM","W_4_PM","W_5_PM","W_6_PM",
                    "W_7_PM","W_8_PM","W_9_PM",

                    "R_7_AM","R_8_AM","R_9_AM","R_10_AM","R_11_AM","R_12_PM",
                    "R_1_PM","R_2_PM","R_3_PM","R_4_PM","R_5_PM","R_6_PM",
                    "R_7_PM","R_8_PM","R_9_PM",

                    "F_7_AM","F_8_AM","F_9_AM","F_10_AM","F_11_AM","F_12_PM",
                    "F_1_PM","F_2_PM","F_3_PM","F_4_PM","F_5_PM","F_6_PM",
                    "F_7_PM","F_8_PM","F_9_PM"
                ]

    PREF_FIELDS = ["pref_minDays", "pref_5days", "pref_TPD", "back_to_back", "gap", "lecAct_1hrLec", "lecAct_2hrAct",
                   "lecAct_noPref", "lecAct_notSure"]
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
    try:
        VALID_EXTENSIONS = ['tsv', 'csv', 'xlsx', 'xlsm', 'xlsb']
        FILE = serializer.validated_data["file"]
        CUR_TERM = serializer.validated_data["cur_term"]
        PREV_TERM = serializer.validated_data["prev_term"]

        df = file_to_df(FILE, VALID_EXTENSIONS)
        #check number of columns expected
        if len(df.columns) != len(REPLACEMENT_FIELDNAMES):
            return Response({
                "msg": f"Number of columns expected is {len(REPLACEMENT_FIELDNAMES)}; given {len(df.columns)}"
            }, status.HTTP_400_BAD_REQUEST)
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
            #drop this field if it has bad data
            if data["email"] in ["anonymous"]:
                data.pop("email")

            SURVEY_NAME = data[NAME_FIELD].strip()
            BLEED_SURVEY = BLEED_FORWARD_STRING == data[BLEEDS_FORWARD_FIELD]
            if SURVEY_NAME == "":
                failed.append({
                    "msg": "Survey entry had no name",
                    "teacher_file_idx": idx
                })
                continue
            #SKIP generic timeslots
            name_normalized = SURVEY_NAME.lower()
            if "generic" in name_normalized or "morning" in name_normalized or "afternoon" in name_normalized:
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
    