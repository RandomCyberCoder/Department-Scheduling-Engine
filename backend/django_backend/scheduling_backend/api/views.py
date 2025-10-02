from rest_framework.decorators import api_view, parser_classes
from rest_framework.response import Response
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework import status
from .models import Teacher
from .serializer import TeacherSerializer, TeacherFileUpload
from django.shortcuts import render
import pandas as pd

@api_view(['GET'])
def get_users(request):
    # return serialized data
    teachers = Teacher.objects.all()
    serializer = TeacherSerializer(teachers, many=True)
    return Response(serializer.data)

@api_view(['POST'])
def create_user(request):
    # serialize data
    serializer = TeacherSerializer(data=request.data)
    # check if its valid
    if serializer.is_valid():
        # if so then save it
        serializer.save()
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(['POST'])
@parser_classes([FormParser, MultiPartParser])
def users_file_upload(request):
    #we allow for csv files or excel files; various different excel extensions
    valid_extensions = ['csv', 'xlsx', 'xlsm', 'xlsb']
    serializer = TeacherFileUpload(data=request.data)
    if serializer.is_valid():
        print("valid payload")
        try:
            file = serializer.validated_data['file']
            extension = file.name.split(".")[-1]
            if not extension in valid_extensions:
                return Response(
                    {"error": f"Invalid file type. Allowed file types are: {valid_extensions}"}, 
                    status=status.HTTP_400_BAD_REQUEST
                )
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
