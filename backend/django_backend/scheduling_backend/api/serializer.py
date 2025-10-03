from rest_framework import serializers
from .models import Teacher

# convert and properly transport you data into json your api can use?
class TeacherSerializer(serializers.ModelSerializer):
    class Meta:
        model = Teacher
        fields = '__all__'

class FileUpload(serializers.Serializer):
    file = serializers.FileField()

class DepartmentFileUpload(serializers.Serializer):
    file = serializers.FileField()
    department = serializers.CharField(max_length=3)

