from rest_framework.decorators import api_view, parser_classes
from rest_framework.response import Response
from rest_framework.parsers import FormParser, MultiPartParser, JSONParser
from rest_framework import status
from django.shortcuts import render
from django.db.models import Q
from ..models import Teacher, History
from ..serializer import TeacherSerializer, FileUploadSerializer, HistorySerializer
from typing import List, Tuple
import pandas as pd
import traceback
from ..types.teacherData import TeacherData
from .history_helpers import history_save_name

VALID_DEPARTMENTS = ["csc", "cpe"]

# helper classes 
def valid_file_extension(file_name: str, extensions: list) -> bool:
    extension = file_name.split(".")[-1]
    if extension  in extensions:
        return True
    return False

# view functions
@api_view(['GET'])
def get_teachers(request):
    department = request.query_params.get("department")
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


#TODO TEST THIS. I SHOULD HAVE WRITTEN ALL LOGIC NOW
@api_view(['POST'])
def create_teacher(request):
    # check if upload is in bulk or single instance
    is_list = isinstance(request.data, list)

    # serialize data
    serializer = TeacherSerializer(data=request.data, many=is_list)
    # check if its valid
    print("before validation")
    print(request.data)
    if serializer.is_valid():
        # if so then save it
        print("got here")
        # return Response(serializer.data, status=status.HTTP_201_CREATED)
        teachers = serializer.save()
        
        #store the names in the history table
        # lst_data = serializer.data
        if not is_list:
            teachers = [teachers]
        
        for teacher in teachers:
            # new_pk = teacher["id"]
            canon_name = teacher.canon
            non_canon_name = teacher.non_canon
            history_save_name(teacher, canon_name, True)
            history_save_name(teacher, non_canon_name, False)
 
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

 
@api_view(['POST'])
@parser_classes([FormParser, MultiPartParser])
def teachers_file_upload(request):
    #we allow for csv files or excel files; various different excel extensions
    valid_extensions = ['csv', 'xlsx', 'xlsm', 'xlsb']
    serializer = FileUploadSerializer(data=request.data)
    if serializer.is_valid():
        print("valid payload")
        try:
            file = serializer.validated_data["file"]

            if not valid_file_extension(file.name, valid_extensions):
                return Response(
                    {"error": f"Invalid file type. Allowed file types are: {valid_extensions}"}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
            
            extension = file.name.split(".")[-1]
            if extension == "CSV":
                df = pd.read_csv(file)
            else:
                df = pd.read_excel(file)
            if "name" not in df.columns or "canon" not in df.columns:
                return Response(
                    {"error": f"File : {valid_extensions}"}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
            print(df)

            #create Teacher entries for upload to DB
            entries = []
            for index, row in df.iterrows():
                data = {"canon": row["canon"], "non_canon": row["name"]}
                entries.append(data)

            # TODO update to serialize individual to check which are valid and which aren't and return this information in the response    
            teacher_serializer = TeacherSerializer(data=entries, many=True)

            try:
                if not teacher_serializer.is_valid():
                    raise Exception("error creating Teacher objects for uploading to DB", status.HTTP_500_INTERNAL_SERVER_ERROR)
                teacher_serializer.save()
                return Response({"success": "Teachers have been created or updated", "data": teacher_serializer.data},
                                status.HTTP_201_CREATED)
                    
            except Exception as e:
                return Response({"error": f"{e}"}, status.HTTP_500_INTERNAL_SERVER_ERROR)
            
        except Exception as e:
            print(f"problem parsing file. error:\n{e}")
            return Response({"error": f"problem reading the file {file.name}"}, status.HTTP_500_INTERNAL_SERVER_ERROR)
    
    return Response(serializer.errors, status.HTTP_400_BAD_REQUEST) 


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
        print(teacher)
    except Exception as e:
        print(type(status.HTTP_404_NOT_FOUND))
        return Response({"error": f"{e}",
                         "msg": f"Couldn't find teacher object with primary key {pk}"},
                        status.HTTP_404_NOT_FOUND)
    
    if request.method == 'GET':
        print("get endpoint")
        serializer = TeacherSerializer(teacher)
        return Response({"success": "Teacher object retrieved",
                         "data": serializer.data},
                         status.HTTP_200_OK)
    elif request.method == 'PUT':
        #TODO Make sure this is idempotent
        print("put endpoint")
        serializer = TeacherSerializer(teacher, data=request.data)
        return update_teacher_helper(serializer, status.HTTP_200_OK)
    elif request.method == 'PATCH':
        #TODO if a either version of a teacher's name is changed then store it in the history table
        serializer = TeacherSerializer(teacher, data=request.data, partial=True)
        print("patch endpoint")
        return update_teacher_helper(serializer, status.HTTP_200_OK)
    elif request.method == 'DELETE':
        print("deleting endpoint")
        teacher.delete()
        return Response({"success": f"teacher object with pk '{pk}' has been deleted"},
                        status=status.HTTP_204_NO_CONTENT)
    else:
        print("illegal method")
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

    def tsv_to_df(file) -> pd.DataFrame:
        captured_header = False
        parsed_lines = []

        for line in file:
            parsed_line = line.decode("utf-8").split("\t")
            normalized_line = [value.strip() for value in parsed_line]

            if not captured_header:
                header = [value.lower() for value in normalized_line]
                captured_header = True
                continue

            parsed_lines.append(parsed_line)

        df = pd.DataFrame(data=parsed_lines, columns=header)
     
        return df
    

    def invalid_df(df: pd.DataFrame) -> bool:
        columns = df.columns.to_list()
        for column in MINIMUM_EXPECTED_COLUMNS:
            if column not in columns:
                return True
        
        return False
        


    valid_extensions = ["csv", "tsv"]
    serializer = FileUploadSerializer(data=request.data)

    if not serializer.is_valid():
            Response({"error": f"{serializer.errors}",
                    "msg": "Invalid data"}, status.HTTP_400_BAD_REQUEST)

    if request.method == 'PATCH':
        file = serializer.validated_data["file"]
 
        if not valid_file_extension(file.name, valid_extensions):
            return Response({"error": f"Invalid file type. Allowed file types are: {valid_extensions}"}, 
                status=status.HTTP_400_BAD_REQUEST
            )

        extension = file.name.split(".")[-1]        
        updates = []
        updates_teacher = []
        failed = []

        if extension == "csv":
            faculty_df = pd.read_csv(file)
        elif extension == "tsv":
            faculty_df = tsv_to_df(file)

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

    '''
    Validates if the structure of the request is valid
    '''
    def validate_req(data: any) -> Tuple[bool, str]:
        #valid keys for nested dictionaries
        teacher_fields = {f.name for f in Teacher._meta.get_fields()}
        lookup_options = {"email", "canon_name", "non_canon_name", "pk"}

        if not isinstance(data, list):
            return False, "data must given a list"

        for index, update_obj in enumerate(data):
            if not isinstance(update_obj, dict):
                return False, "list elements must be in a dictionary"

            print(update_obj)
            if "lookup" not in update_obj or "update_data" not in update_obj:
                return False, f"update payload at index '{index}' must include the fields 'lookup' and 'update_data'"
            
            lookup_keys = set(update_obj["lookup"].keys())
            update_data_keys = set(update_obj["update_data"].keys()) 

            #check for invalid keys in respective dictionaries
            lookup_diff = lookup_keys.difference(lookup_options)
            update_data_diff = update_data_keys.difference(teacher_fields)

            if lookup_diff or update_data_diff or len(lookup_keys) == 0:
                return False, (f"no lookup option found or invalid key(s) found in 'lookup' or 'update_data' for object at index {index}. "
                                f"Valid keys for objects respectively are {lookup_options} and {teacher_fields}. ")

        return True, "all data valid"
    

    def generate_Q_objects(lookup_obj: dict) -> Q:
        cur_Q = None

        for k, v in lookup_obj.items():
            if k == "email":
                Q_to_add = Q(email=v)
            elif k == "canon_name":
                Q_to_add = Q(canon=v)
            elif k == "non_canon_name":
                Q_to_add = Q(non_canon=v)
            elif k == "pk":
                Q_to_add = Q(id=v)

            if cur_Q is None:
                cur_Q = Q_to_add
            else: 
                cur_Q |= Q_to_add

        return cur_Q


    req_data = request.data
    valid, msg = validate_req(req_data)
    
    if not valid:
        return Response({"error": msg},
                        status.HTTP_400_BAD_REQUEST)
    
    success = []
    failed = []
    for update_obj in req_data:
        try:
            teacher = Teacher.objects.get(generate_Q_objects(update_obj["lookup"]))
            serializer = TeacherSerializer(teacher, data=update_obj["update_data"], partial=True)
            if serializer.is_valid():
                success.append({"msg": f"success updating teacher with lookup: {update_obj['lookup']}"})
                serializer.save()
            else:
                failed.append({"msg": f"failed to update teacher with lookup: {update_obj['lookup']}",
                        "error": f"{serializer.errors}"})
        except Exception as e:
            print("--------------------------------------------------------------------------------")
            print(traceback.format_exc())
            failed.append({"msg": f"failed to update teacher with lookup: {update_obj['lookup']}",
                        "error": f"{e}"})

    return Response({"msg": "finished trying to update teachers",
                "success": success,
                "failed": failed},
                status.HTTP_200_OK)
        
    
    
    # [
    #     {
    #         "lookup": {
    #             #options but at least one should be present
    #             "email": "example@example.com",
    #             "canon_name": "Eman",
    #             "non_canon_name": "Edog",
    #             "pk": <int>
    #         },
    #         #this is the data that will be passed in
    #         "update_data":{ 
    #             "csc": "True"
    #         }
    #     }
    # ]
    
