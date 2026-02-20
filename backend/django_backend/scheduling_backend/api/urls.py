from django.urls import path

from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)
from .views.teacher_views import (
    get_teachers, 
    create_teacher,
    teachers_file_upload,
    update_teacher,
    set_faculty,
    teacher_bulk_update,
)
from .views.survey_views import (
    survey_file_upload,
    survey_base_endpoint,
    delete_survey_instance
)
from .views.history_views import(
    historyRetByName
)

#routes for api
urlpatterns = [
    #authentication endpoints
    path('token/', TokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    
    #teacher endpoints
    path('teachers/', get_teachers, name='get_user'),
    path('teachers/create/', create_teacher, name='create_teacher'),
    path('teachers/<int:pk>/', update_teacher, name='update_teacher'),
    path('teachers/file/', teachers_file_upload, name='users_file_upload'),
    path('teachers/faculty/file/', set_faculty, name='set_faculty'),
    path('teachers/update/bulk/', teacher_bulk_update, name="teacher_bulk_update"),
    
    # survey endpoints
    path('surveys/file/', survey_file_upload, name="survey_file_upload"),
    path('surveys/', survey_base_endpoint, name="base_survey_endpoint"),
    path('surveys/remove/<int:pk>/', delete_survey_instance, name="del_surv_inst"),

    # history endpoints
    path('history/', historyRetByName, name="retrieve_by_name")
]