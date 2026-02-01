from rest_framework.decorators import api_view, parser_classes
from rest_framework.response import Response
from rest_framework.parsers import FormParser, MultiPartParser, JSONParser
from rest_framework import status
from rest_framework.exceptions import APIException
from rest_framework.request import Request
from django.db.models import Q
from django.core.exceptions import ObjectDoesNotExist
import pandas as pd
from ..models import History
from ..serializer import HistorySerializer
from .helper.file_reader import file_to_df
from .helper.survey_helpers import find_teacher, find_survey

@api_view(["GET"])
def historyRetByName(request: Request) -> Response:
    """A get request that take in a 'name' query param (must be present) and will search the History
    table for a teacher that has had that name in the past

    Args:
        request (Request): request payload

    Returns:
        Response: teacher object if otherwise it return a 4XX status code
    """
    PARAM_SERACH_NAME = request.query_params.get("name", None)
    if PARAM_SERACH_NAME is None:
        return Response({"msg": "endpoint requires the query parameter 'name'"},
                        status=status.HTTP_400_BAD_REQUEST)
    
    try:
        history_query = History.objects.get(name=PARAM_SERACH_NAME)
        record_serialized = HistorySerializer(history_query)
    except ObjectDoesNotExist as e:
        return Response({
            "msg": f"No record in the DB matching the query exists"
        },
        status=status.HTTP_404_NOT_FOUND)
        
    except Exception as e:
        return Response({
            "msg": f"Dev error: {e}"
        },
        status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    return Response({
        "msg": "History record matching query found",
        "data": record_serialized.data 
    },
    status=status.HTTP_200_OK)