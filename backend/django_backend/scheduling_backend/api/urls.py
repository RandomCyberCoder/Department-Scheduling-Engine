from django.urls import path
from .views.teacher_views import (
    get_users, 
    create_user,
    users_file_upload,
    update_teacher,
    set_faculty,
)

urlpatterns = [
    #route for api
    path('teachers/', get_users, name='get_user'),
    path('teachers/create/', create_user, name='create_user'),
    path('teachers/<int:pk>/', update_teacher, name='update_teacher'),
    path('teachers/file/', users_file_upload, name='users_file_upload'),
    path('teachers/faculty/file/', set_faculty, name='set_faculty'),
]