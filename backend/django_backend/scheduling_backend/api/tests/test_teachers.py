from django.test import TestCase
from ..models import Teacher, History
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from django.contrib.auth.models import User, Permission
from django.core.files.uploadedfile import SimpleUploadedFile



class ModelTesting(TestCase):
    def setUp(self):
        self.teacher = Teacher.objects.create(
            canon="John Doe",
            non_canon="jdoe",
            email="jdoe@example.com",
            cpe=False,
            csc=False,
            faculty=False,
        )

    def test_create_teacher(self):
        t = self.teacher
        self.assertTrue(isinstance(t, Teacher))
        self.assertEqual(t.canon, "John Doe")
        self.assertEqual(t.non_canon, "jdoe")
        self.assertEqual(t.email, "jdoe@example.com")
        self.assertFalse(t.cpe)
        self.assertFalse(t.csc)
        self.assertFalse(t.faculty)


class TeacherTests(APITestCase):
    def setUp(self):
        # create a user with the permission required by the view
        self.user = User.objects.create_user(username='testuser', password='pass')
        add_perm = Permission.objects.get(codename='add_teacher')
        update_perm = Permission.objects.get(codename='change_teacher')
        self.user.user_permissions.add(add_perm, update_perm)
        self.client.force_authenticate(user=self.user)

    def test_create_teacher(self):
        """Ensure we can create a new Teacher object via the endpoint."""
        url = reverse('create_teacher')
        data = {'canon': 'Official Bob Marley', 'non_canon': 'Bob Marley'}
        response = self.client.post(url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Teacher.objects.count(), 1)
        self.assertEqual(Teacher.objects.get().canon, 'Official Bob Marley')
        self.assertEqual(Teacher.objects.get().non_canon, 'Bob Marley')
        self.assertEqual(Teacher.objects.get().email, None)
        self.assertFalse(Teacher.objects.get().cpe)
        self.assertFalse(Teacher.objects.get().csc)
        self.assertFalse(Teacher.objects.get().faculty)

        #won't allow overwrite this teacher must be unique
        data_bad = {'canon': 'Official Bob Marley', 'non_canon': 'Bob Marleay'}
        response = self.client.post(url, data_bad, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        #write all values
        data_all = {'canon': "Jhon Doe", 'non_canon': "jdoe", 'email': "jhondoe@example.com", 'cpe': True, 'csc': True, 'faculty': True}
        response = self.client.post(url, data_all, format='json')
        self.assertEqual(response.status_code & status.HTTP_200_OK, status.HTTP_200_OK)
        self.assertEqual(Teacher.objects.count(), 2)
        self.assertEqual(Teacher.objects.get(canon="Jhon Doe").canon, 'Jhon Doe')
        self.assertEqual(Teacher.objects.get(canon="Jhon Doe").non_canon, 'jdoe')
        self.assertEqual(Teacher.objects.get(canon="Jhon Doe").email, "jhondoe@example.com")
        self.assertTrue(Teacher.objects.get(canon="Jhon Doe").cpe)
        self.assertTrue(Teacher.objects.get(canon="Jhon Doe").csc)
        self.assertTrue(Teacher.objects.get(canon="Jhon Doe").faculty)

    def test_create_teacher_no_permission(self):
        """Ensure we cannot create a new Teacher object without the proper permission."""
        self.client.force_authenticate(user=None)  # Log out the user
        url = reverse('create_teacher')
        data = {'canon': 'Unauthorized User', 'non_canon': 'unauth'}
        response = self.client.post(url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_teacher_bulk(self):
        """Ensure we can bulk update Teacher objects via the endpoint."""
        # Create some initial teachers
        
        
        with open("./api/tests/test_files/test_teacher_upload.xlsx", "rb") as f:
            # This is the data in the file
            # contains 3 unique teachers and one teach has two entires to be overwritten
            file = SimpleUploadedFile(
                "test_teacher_upload.xlsx",
                f.read(),
                content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        url = reverse('users_file_upload')
        response = self.client.post(
            url,
            {"file": file},
            format="multipart"
        )
        #there are four rows in the csv file, 3 are unique one overwrites the canon name.
        self.assertEqual(Teacher.objects.count(), 3)
        self.assertEqual(History.objects.count(), 7)
        self.assertEqual(response.status_code & status.HTTP_200_OK, status.HTTP_200_OK)
        