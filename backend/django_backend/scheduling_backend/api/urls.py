from django.urls import path
from .views.teacher_views import (
    get_teachers, 
    create_teacher,
    teachers_file_upload,
    update_teacher,
    set_faculty,
    teacher_bulk_update,
)

urlpatterns = [
    #route for api
    path('teachers/', get_teachers, name='get_user'),
    path('teachers/create/', create_teacher, name='create_user'),
    path('teachers/<int:pk>/', update_teacher, name='update_teacher'),
    path('teachers/file/', teachers_file_upload, name='users_file_upload'),
    path('teachers/faculty/file/', set_faculty, name='set_faculty'),
    path('teachers/update/bulk/', teacher_bulk_update, name="teacher_bulk_update"),
]