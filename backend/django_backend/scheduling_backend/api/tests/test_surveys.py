from django.test import TestCase
from ..models import Teacher, Survey
from ..serializer import SurveySerializer
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from django.contrib.auth.models import User, Permission
from django.core.files.uploadedfile import SimpleUploadedFile


class SurveyTests(APITestCase):
    def setUp(self):
        # create a user with the permission required by the view
        self.user = User.objects.create_user(username='testuser', password='pass')
        survey_add_perm = Permission.objects.get(codename='add_survey')
        survey_update_perm = Permission.objects.get(codename='change_survey')
        survey_view_perm = Permission.objects.get(codename='view_survey')
        teacher_add_perm = Permission.objects.get(codename='add_teacher')
        self.user.user_permissions.add(survey_add_perm, survey_update_perm, survey_view_perm,
                                        teacher_add_perm)
        self.client.force_authenticate(user=self.user)

        #insert teachers that will be used for survey uploads
        Teacher.objects.create(
            canon="Perez, Sergio",
            non_canon="Mexican Minister of Defense"
        )


    def test_bad_request(self):
        """Test a bad payload is rejected"""

        url = reverse('survey_file_upload')
        
        with open("./api/tests/test_files/test_survey.csv", "rb") as f:
            # This is the data in the file
            # contains 3 unique teachers and one teach has two entires to be overwritten
            file = SimpleUploadedFile(
                "test_survey.csv",
                f.read(),
                content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
    
        response = self.client.post(url, {'file': file}, format='multipart')
        #we use bit AND operation to check resulting status codes
        self.assertEqual(response.status_code & status.HTTP_400_BAD_REQUEST, status.HTTP_400_BAD_REQUEST)

    def test_survey_upload(self):
        """Test a valid survey upload"""

        url = reverse('survey_file_upload')
        
        #upload current term
        with open("./api/tests/test_files/test_survey.csv", "rb") as f:
            # This is the data in the file
            # contains 3 unique teachers and one teach has two entires to be overwritten
            file = SimpleUploadedFile(
                "test_survey.csv",
                f.read(),
                content_type="text/csv"
            )
    
        response = self.client.post(url, {'file': file, "cur_term": 2268, "prev_term": 2264}, format='multipart')
        self.assertEqual(len(response.data["success"]), 1)
        self.assertEqual(Survey.objects.count(), 1)
        self.assertEqual(response.status_code & status.HTTP_200_OK, status.HTTP_200_OK)

        #upload the next term bleeding forward
        with open("./api/tests/test_files/test_survey_next.csv", "rb") as f:
            # This is the data in the file
            # contains 3 unique teachers and one teach has two entires to be overwritten
            file2 = SimpleUploadedFile(
                "test_survey_next.csv",
                f.read(),
                content_type="text/csv"
            )
    
        response2 = self.client.post(url, {'file': file2, "cur_term": 2272, "prev_term": 2268}, format='multipart')
        self.assertEqual(len(response.data["success"]), 1)
        self.assertEqual(Survey.objects.count(), 2)
        self.assertEqual(response.status_code & status.HTTP_200_OK, status.HTTP_200_OK)

        #make sure data was ovewritten correctly

        url = reverse('base_survey_endpoint')
        query_bled = "?term=2268&name=Mexican Minister of Defense"
        query_cur = "?term=2272&name=Mexican Minister of Defense"

        #retrieve the two surveys via endpoint; note that a term and name pair must be unique
        survey_bled = self.client.get(url + query_bled).data[0]
        survey_cur = self.client.get(url + query_cur).data[0]
        #the next survey has different fields from those in bled, but they should be overwritten
        #also the bled survey has one field blank that should default to conflict
        for field in ["M_7_AM", "M_8_AM", "M_9_AM"]:
            self.assertEqual(survey_bled[field], survey_cur[field])

    
           