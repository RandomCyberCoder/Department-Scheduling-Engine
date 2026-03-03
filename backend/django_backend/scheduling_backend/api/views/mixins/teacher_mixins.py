from rest_framework.exceptions import ValidationError
from django.db.models import Q

VALID_DEPARTMENTS = ["csc", "cpe"]
VALID_BOOLEAN_STRINGS = ["true", "false"]
VALID_LOGIC_STRINGS = ["and", "or"]

class DepFacFilterMixin:
    def filter_by_dep_fac(self, queryset):
        department = self.request.query_params.get('department')
        faculty = self.request.query_params.get('faculty')
        logic = self.request.query_params.get('logic', 'and').lower()
        
        if logic and logic not in VALID_LOGIC_STRINGS:
            raise ValidationError({
                "message": f"logic querying options are {VALID_LOGIC_STRINGS}"
            })
        if faculty and faculty.lower() not in VALID_BOOLEAN_STRINGS:
            raise ValidationError({
                "message": "faculty querying options are 'true' and 'false'"
            })
        if department and department.lower() not in VALID_DEPARTMENTS:
            raise ValidationError({
                "message": f"valid depeartments are {VALID_DEPARTMENTS}"
            })

        filters = Q()
        if faculty:
            filters &= Q(faculty=True) if faculty.lower() == "true" else Q(faculty=False)
        if department:
            dep_filter = Q(csc=True) if department.lower() == "csc" else Q(cpe=True)
            filters = filters & dep_filter if logic == "and" else filters | dep_filter

        return queryset.filter(filters)