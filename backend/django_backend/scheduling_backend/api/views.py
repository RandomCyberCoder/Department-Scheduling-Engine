from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .models import Teacher
from .serializer import TeacherSerializer
from django.shortcuts import render

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