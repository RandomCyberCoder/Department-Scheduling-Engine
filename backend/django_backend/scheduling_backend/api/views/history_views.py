from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status, mixins, generics
from rest_framework.request import Request 
from rest_framework.permissions import DjangoModelPermissions
from django.core.exceptions import ObjectDoesNotExist 
from django.contrib.auth.decorators import permission_required
from ..models import History
from ..serializer import HistorySerializer

@api_view(["GET"])
@permission_required("api.view_history")
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


# This isn't really used at all. Just used it for CBV practice before updating other FBV -> CBV
# class HistorySpecific(APIView):
#     """
#     Retrieve or delete a history instance.
#     """
#     permission_classes = [DjangoModelPermissions]
#     queryset = History.objects.all()

#     def get_object(self, pk):
#         try:
#             return History.objects.get(pk=pk)
#         except History.DoesNotExist:
#             raise Http404

#     def get(self, request, pk, format=None):
#         h_obj = self.get_object(pk)
#         serializer = HistorySerializer(h_obj)
#         return Response(serializer.data)
    
#     def delete(self, request, pk, format=None):
#         h_obj = self.get_object(pk)
#         h_obj.delete()
#         return Response(status=status.HTTP_204_NO_CONTENT)
    
class HistorySpecific(
    mixins.RetrieveModelMixin,
    mixins.DestroyModelMixin,
    generics.GenericAPIView,
):
    permission_classes = [DjangoModelPermissions]
    queryset = History.objects.all()
    serializer_class = HistorySerializer
    

    def get(self, request, *args, **kwargs):
        return self.retrieve(request, *args, **kwargs)

    def delete(self, request, *args, **kwargs):
        return self.destroy(request, *args, **kwargs)


