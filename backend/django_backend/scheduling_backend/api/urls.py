from django.urls import path
from .views import (
    get_users, 
    create_user,
    users_file_upload

)

urlpatterns = [
    #route for api
    path('users/', get_users, name='get_user'),
    path('users/create/', create_user, name='create_user'),
    path('users/file', users_file_upload, name='users_file_upload'),
]