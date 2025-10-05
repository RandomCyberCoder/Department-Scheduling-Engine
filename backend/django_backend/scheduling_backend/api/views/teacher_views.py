from rest_framework.decorators import api_view, parser_classes
from rest_framework.response import Response
from rest_framework.parsers import FormParser, MultiPartParser, JSONParser
from rest_framework import status
from django.shortcuts import render
from django.db.models import Q
from ..models import Teacher
from ..serializer import TeacherSerializer, FileUploadSerializer
import pandas as pd


# helper classes 
def update_teacher_helper(serializer: TeacherSerializer, successStatusCode: int) -> Response:
    if serializer.is_valid():
        serializer.save()
        return Response({"success": "teacher obj has been updated"},
                        successStatusCode)
    return Response({"error": f"{serializer.errors}",
                     "msg" :"Couldn't update the object"},
                     status.HTTP_406_NOT_ACCEPTABLE)


def valid_file_extension(file_name: str, extensions: list) -> bool:
    extension = file_name.split(".")[-1]
    if extension  in extensions:
        return True
    return False


# view functions
@api_view(['GET'])
def get_teachers(request):
    # return serialized data
    teachers = Teacher.objects.all()
    serializer = TeacherSerializer(teachers, many=True)
    return Response(serializer.data)

@api_view(['POST'])
def create_teacher(request):
    # check if upload is in bulk or single instance
    is_list = isinstance(request.data, list)

    # serialize data
    serializer = TeacherSerializer(data=request.data, many=is_list)
    # check if its valid
    if serializer.is_valid():
        # if so then save it
        serializer.save()
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
        print("put endpoint")
        serializer = TeacherSerializer(teacher, data=request.data)
        return update_teacher_helper(serializer, status.HTTP_200_OK)
    elif request.method == 'PATCH':
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
