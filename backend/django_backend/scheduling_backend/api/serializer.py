from rest_framework import serializers
from .models import Teacher

# convert and properly transport you data into json your api can use?
class TeacherSerializer(serializers.ModelSerializer):
    class Meta:
        model = Teacher
        fields = '__all__'

class TeacherFileUpload(serializers.Serializer):
    file = serializers.FileField()