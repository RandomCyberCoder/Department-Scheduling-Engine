from django.urls import path
from .views import get_users, create_user

urlpatterns = [
    #route for api
    path('users/', get_users, name='get_user'),
    path('users/create', create_user, name='create_user')
]