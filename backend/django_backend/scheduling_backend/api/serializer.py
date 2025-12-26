from rest_framework import serializers
from .models import Teacher, Survey, History


class TeacherListSerializer(serializers.ListSerializer):

    def validate(self, data):
        unique_canon = set()
        unique_non = set()

        #go through all Teacher items in the list
        for item in data:
            canon = item["canon"]
            non_canon = item["non_canon"]

            if canon in unique_canon:
                raise serializers.ValidationError(f"Payload found to have duplicate canon key '{canon}'")
            if non_canon in unique_non:
                raise serializers.ValidationError(f"Payload found to have duplicate non-canon key '{non_canon}'")

            unique_canon.add(canon)
            unique_non.add(non_canon)

        return data

# convert and properly transport you data into json your api can use?
class TeacherSerializer(serializers.ModelSerializer):
    class Meta:
        model = Teacher
        fields = '__all__'
        read_only_fields = ['id']
        list_serializer_class = TeacherListSerializer

    def validate_canon(self, value):
        return value.strip()
    
    def validate_non_canon(self, value):
        return value.strip()

class FileUploadSerializer(serializers.Serializer):
    file = serializers.FileField()

class DepartmentFileUpload(serializers.Serializer):
    file = serializers.FileField()
    department = serializers.CharField(max_length=3)

class SurveySerializer(serializers.ModelSerializer):
    class Meta:
        model = Survey
        fields = '__all__'
        read_only_fields = ['id']

class HistorySerializer(serializers.ModelSerializer):
    class Meta:
        model = History
        fields = '__all__'
        read_only_fields = ['id']
