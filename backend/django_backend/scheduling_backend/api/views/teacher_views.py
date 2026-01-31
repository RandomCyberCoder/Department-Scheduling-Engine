from rest_framework.decorators import api_view, parser_classes
from rest_framework.response import Response
from rest_framework.parsers import FormParser, MultiPartParser, JSONParser
from rest_framework import status
from rest_framework.exceptions import APIException
from django.db.models import Q
from django.db import transaction
from typing import Tuple
import pandas as pd
from ..models import Teacher
from ..serializer import TeacherSerializer, FileUploadSerializer
from .helper.history_helpers import history_save_name
from .helper.file_reader import file_to_df
from .helper.query_helpers import generate_Q_objects
from django.core import serializers

VALID_DEPARTMENTS = ["csc", "cpe"]



@api_view(['GET'])
def get_teachers(request):
    """
    GET endpoint for retrienveing all teachers are teachers within just the CPE or CSC department 
    
    :param request: payload request
    """
    department = request.query_params.get("department")
    # faculty = request.query_param.get("faculty")
    # all_flag = request.query_param.get("all_flag")
    if department is not None:
        if department.lower() not in VALID_DEPARTMENTS:
            return Response({"error": f"valid departments are: {VALID_DEPARTMENTS}"},
                            status.HTTP_400_BAD_REQUEST)
        
        filter_obj = Q(csc="True") if department == "csc" else Q(cpe="True")
        teachers = Teacher.objects.filter(filter_obj)
    else: 
        teachers = Teacher.objects.all()

    serializer = TeacherSerializer(teachers, many=True)

    return Response(serializer.data)




@api_view(['POST'])
def create_teacher(request):
    """
    Creates a Teacher object(s) in the data base. Will return 400 if payload has a duplicate within or 
    duplicates a unique filed in the DB. Each teacher created will also have two entries in the History
    table. One for their canon and another for their non_canon name
    
    :param request: payload request
    """
    
    # check if upload is in bulk or single instance
    is_list = isinstance(request.data, list)

    # serialize data
    serializer = TeacherSerializer(data=request.data, many=is_list)
    # check if its valid
    if serializer.is_valid():
        # if so then save it
        print("got here")
        # return Response(serializer.data, status=status.HTTP_201_CREATED)
        teachers = serializer.save()
        
        #normalize data
        if not is_list:
            teachers = [teachers]
        
        #store names in History table
        for teacher in teachers:
            canon_name = teacher.canon
            non_canon_name = teacher.non_canon
            history_save_name(teacher, canon_name, True)
            history_save_name(teacher, non_canon_name, False)
 
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)



#TODO add the possibility for a facutly field as well. it will be easier to use I think if so
@api_view(['POST'])
@parser_classes([FormParser, MultiPartParser])
def teachers_file_upload(request):
    """Will attempt to create an object if one does not exist. If one exists it will be updated.
    This will only consider the teacher entity fields 'canon', 'non_canon', and 'email'

    Args:
        request (_type_): request payload

    Raises:
        Exception: exceptions raised are handled by the function

    Returns:
        _type_: _description_
    """
    #we allow for csv files or excel files; various different excel extensions
    VALID_EXTENSIONS = ['tsv', 'csv', 'xlsx', 'xlsm', 'xlsb']
    FACULTY_COLUMN = "faculty"
    serializer = FileUploadSerializer(data=request.data)
    if serializer.is_valid():
        FILE = serializer.validated_data["file"]
        try:
            df = file_to_df(FILE, VALID_EXTENSIONS)
        except APIException as e:
            return Response({
                "msg": f"{e}"},
            status=status.HTTP_406_NOT_ACCEPTABLE)
        except Exception as e:
            return Response({"error": f"problem reading the file {FILE.name}",
                             "msg": f"{e}"}, status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        #allow for canon name to be titled either as 'name' or 'non_canon'
        if (("name" not in df.columns and "non_canon" not in df.columns) 
            #prevent both being present
            or ("name" in df.columns and "non_canon" in df.columns) 
            or "canon" not in df.columns):
            return Response(
                {"error": ("Name column(s) not found. A column with the name 'non_canon' or 'name' and a column"
                 " with the name 'canon' must exist")}, 
                status=status.HTTP_400_BAD_REQUEST
            )  
            
        non_canon_col = "name"
        email_present = False
        if "non_canon" in df.columns:
            non_canon_col = "non_canon"
        if "email" in df.columns:
            email_present = True

        #normalize pandas data frame
        for col in df.columns:
            if col == FACULTY_COLUMN:
                continue
            df[col] = (
                df[col]
                .fillna("")
                .astype(str)
                .str.strip()
            )

        #figure out what updates/creations succeed and which didn't
        success = []
        failed = []
        for _, row in df.iterrows():
            entry = {"canon": row["canon"], "non_canon": row[non_canon_col]}
            if email_present and row["email"] != "":
                entry["email"] = row["email"]
            if FACULTY_COLUMN in df.columns and row[FACULTY_COLUMN] != "":
                is_faculty = row[FACULTY_COLUMN]
                if isinstance(is_faculty, str):
                    is_faculty = is_faculty.strip().lower()
                if is_faculty == 0 or is_faculty == "false":
                    entry["faculty"] = False
                elif is_faculty == 1 or is_faculty == "true":
                    entry["faculty"] = True
                    
            try:
                query  = generate_Q_objects(entry)
                teacher = Teacher.objects.filter(query)
                if teacher.count() > 1:
                    raise APIException(detail="Multiple teacher entities with given fields", code=status.HTTP_409_CONFLICT)
                else:
                    teacher = teacher.first()
                created = False
                
                if teacher:
                    teacher_serializer = TeacherSerializer(teacher, data=entry, partial=True)
                else:
                    teacher_serializer = TeacherSerializer(data=entry)
                    created = True
                
                teacher_serializer.is_valid(raise_exception=True)
                with transaction.atomic():
                    teacher = teacher_serializer.save()
                    history_save_name(teacher, entry["canon"], True)
                    history_save_name(teacher, entry["non_canon"], False)
                #add canon and non_cannon names to history table
                success.append({"creation_status": created,
                                "teacher": teacher_serializer.data})
            except Exception as e:
                failed.append({"msg": f"{e}",
                                "attempt": entry})

        return Response({"msg": "finished trying to update teachers",
                        "success": success,
                        "failed": failed},
                        status.HTTP_200_OK)

    return Response(serializer.errors, status.HTTP_400_BAD_REQUEST) 



#NOTE I only support the GET and DELETE methods, the other two haven't been devoloped enough or have been replaced by other endpoints
@api_view(['GET', 'PUT', 'PATCH', 'DELETE'])
@parser_classes([JSONParser])
def update_teacher(request, pk):

    def update_teacher_helper(serializer: TeacherSerializer, successStatusCode: int) -> Response:
        if serializer.is_valid():
            serializer.save()
            return Response({"success": "teacher obj has been updated"},
                            successStatusCode)
        return Response({"error": f"{serializer.errors}",
                        "msg" :"Couldn't update the object"},
                        status.HTTP_406_NOT_ACCEPTABLE)

    #check if a teacher entry with the given primary key exists
    try:
        teacher = Teacher.objects.get(pk=pk)
    except Exception as e:
        return Response({"error": f"{e}",
                         "msg": f"Couldn't find teacher object with primary key {pk}"},
                        status.HTTP_404_NOT_FOUND)
    
    if request.method == 'GET':
        serializer = TeacherSerializer(teacher)
        return Response({"success": "Teacher object retrieved",
                         "data": serializer.data},
                         status.HTTP_200_OK)
    elif request.method == 'PUT':
        return({"msg": "METHOD not implemented"}, status.HTTP_501_NOT_IMPLEMENTED)
        #TODO Make sure this is idempotent
        #should this take all the fields
        #it also seems that a put request should add 
        #note a put request should also be able to create
        #for fields that don't exists they should just default
        serializer = TeacherSerializer(teacher, data=request.data)
        return update_teacher_helper(serializer, status.HTTP_200_OK)
    elif request.method == 'PATCH':
        return({"msg": "METHOD not implemented"}, status.HTTP_501_NOT_IMPLEMENTED)
        #TODO if a either version of a teacher's name is changed then store it in the history table
        serializer = TeacherSerializer(teacher, data=request.data, partial=True)
        return update_teacher_helper(serializer, status.HTTP_200_OK)
    elif request.method == 'DELETE':
        teacher.delete()
        return Response({"success": f"teacher object with pk '{pk}' has been deleted",
                         "object": serializers.serialize("json", [teacher])},
                        status=status.HTTP_204_NO_CONTENT)
    else:
        return Response({"error": "Unsupported HTTP method for endpoint"},
                        status.HTTP_405_METHOD_NOT_ALLOWED)




'''
Endpoint function for uploading a file to update teachers
It will scan for what teachers are faculty members. Try to find their teacher object
int the DB and update them if they could be find.

TODO if we set up a history of names for a person we could also pull from that which could help
'''
@api_view(["PATCH"])
@parser_classes([FormParser, MultiPartParser])
def set_faculty(request):
    MINIMUM_EXPECTED_COLUMNS = ["name", "title", "email"]
    

    def invalid_df(df: pd.DataFrame) -> bool:
        columns = df.columns.to_list()
        for column in MINIMUM_EXPECTED_COLUMNS:
            if column not in columns:
                return True
        
        return False
        


    VALID_EXTENSIONS = ['tsv', 'csv', 'xlsx', 'xlsm', 'xlsb']
    serializer = FileUploadSerializer(data=request.data)

    if not serializer.is_valid():
            Response({"error": f"{serializer.errors}",
                    "msg": "Invalid data"}, status.HTTP_400_BAD_REQUEST)

    if request.method == 'PATCH':
        FILE = serializer.validated_data["file"]   
        
        try:
            faculty_df = file_to_df(FILE, VALID_EXTENSIONS)
        except APIException as e:
            return Response({"error": f"{e}"}, 
                status=status.HTTP_406_BAD_REQUEST)
        except Exception as e:
            return Response({"error": f"problem reading the file {FILE.name}",
                             "msg": f"{e}"}, status.HTTP_500_INTERNAL_SERVER_ERROR)

        updates = []
        updates_teacher = []
        failed = []


        if invalid_df(faculty_df):
            return Response({"error": f"header of file should include: {MINIMUM_EXPECTED_COLUMNS}"},
                                status.HTTP_406_NOT_ACCEPTABLE) 

        for index, row in faculty_df.iterrows():
            try:
                if "professor" in row["title"].lower():
                    non_canon_name = row["name"]
                    email = row["email"]
                    teacher = Teacher.objects.get(Q(non_canon=non_canon_name) | Q(email=email))
                    updates.append(teacher.non_canon)
                    updates_teacher.append(teacher)
            
            except Exception as _:
                failed.append(non_canon_name) 

        update_faculty_dict = {"faculty": "True"}
        for teacher in updates_teacher:                
            serializer = TeacherSerializer(teacher, data=update_faculty_dict, partial=True)
            if serializer.is_valid():
                serializer.save()
            else:
                # I don't think we should ever be able to get here but leaving it here just in case
                print(f"log we failed to update, but this shouldn't be possible {teacher.non_canon}")
                failed.append(teacher.non_canon)
                updates.remove(teacher.non_canon)

        return Response({"msg": "updated teachers",
                "updated": updates,
                "failed": failed
                },
                status.HTTP_202_ACCEPTED)


    return Response({"error": "Unsupported HTTP method for endpoint"}, 
                status.HTTP_405_METHOD_NOT_ALLOWED)




@api_view(["PATCH"])
@parser_classes([JSONParser])
def teacher_bulk_update(request):
    """Bulk updates teachers. Allows a teacher to be identified by email, canon_name, non_canon_name or their id (primary key).
    If multiple teachers found for a lookup then they are skipped. Once the teacher is identified, their fields can be updated.

    Args:
        request (_type_): request
    """

    #valid keys for nested dictionaries
    teacher_fields = {f.name for f in Teacher._meta.get_fields()}
    lookup_options = {"email", "canon", "non_canon", "id"}

    def validate_req(data: any) -> Tuple[bool, str]:
        """Validate payload request to make sure that at least one valid field is present for the lookup and updating.
        Any fields that can't be used for looking up a teacher or updating a teacher will be ignored

        Args:
            data (any): request payload

        Returns:
            Tuple[bool, str]: _description_
        """

        if not isinstance(data, list):
            return False, "data must given a list"

        for index, update_obj in enumerate(data):
            if not isinstance(update_obj, dict):
                return False, "list elements must be in a dictionary"

            if "lookup" not in update_obj or "update_data" not in update_obj:
                return False, f"update payload at index '{index}' must include the fields 'lookup' and 'update_data'"
            
            lookup_keys = set(update_obj["lookup"].keys())
            update_data_keys = set(update_obj["update_data"].keys()) 

            #check for invalid keys in respective dictionaries using set theory
            lookup_diff = lookup_keys.difference(lookup_options)
            update_data_diff = update_data_keys.difference(teacher_fields)

            len_lookup = len(lookup_keys)
            len_update = len(update_data_keys)
            #make sure at least on valid field is present and that data needed is present
            if len(lookup_diff) == len_lookup or len(update_data_diff) == len_update or len_lookup == 0 or len_update == 0:
                return False, (f"no lookup option found in 'lookup' or no data in 'update_data' for object at index {index}. "
                                f"Valid keys for objects respectively are {lookup_options} and {teacher_fields}. ")

        return True, "all data valid"
    

    req_data = request.data
    valid, msg = validate_req(req_data)
    
    if not valid:
        return Response({"error": msg},
                        status.HTTP_400_BAD_REQUEST)
    
    # store what teacher entities where updated successfully or failed to update
    success = []
    failed = []
    for update_obj in req_data:
        try:
            update_data = update_obj["update_data"]
            teacher = Teacher.objects.get(generate_Q_objects(update_obj["lookup"]))
            serializer = TeacherSerializer(teacher, data=update_data, partial=True)
            
            if serializer.is_valid():
                serializer.save()
                success.append({"msg": f"success updating teacher with lookup: {update_obj['lookup']}",
                                "teacher_data": serializer.data})
                #save new names in history table
                if "canon" in update_data:
                    history_save_name(teacher, update_data["canon"], True)
                if "non_canon" in update_obj["update_data"]:
                    history_save_name(teacher, update_data["non_canon"], False)
            else:
                failed.append({"msg": f"failed to update teacher with lookup: {update_obj['lookup']}",
                        "error": f"{serializer.errors}"})
        except Exception as e:
            failed.append({"msg": f"failed to update teacher with lookup: {update_obj['lookup']}",
                        "error": f"{e}"})

    return Response({"msg": "finished trying to update teachers",
                "success": success,
                "failed": failed},
                status.HTTP_200_OK)
        