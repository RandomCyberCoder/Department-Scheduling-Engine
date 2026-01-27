from django.db.models import Q
from django.core.exceptions import MultipleObjectsReturned, ObjectDoesNotExist
from rest_framework.exceptions import APIException
from ...models import Teacher, History, Survey
from .query_helpers import generate_Q_objects 

def find_teacher(canon=None, non_canon=None, email=None) -> Teacher:
    """Parameters are used to query ``Teacher`` objects, with the goal of identifying a unique teacher. 
    If multiple arguments are provided, a Teacher is returned when any field matches any of the given values.
    Args:
        canon (_type_, optional): Potential canon name for ``Teacher`` object. Defaults to None.
        non_canon (_type_, optional): Potential non-canon name for ``Teacher`` object. Defaults to None.
        email (_type_, optional): Potential email for ``Teacher`` object. Defaults to None.

    Raises:
        Exception: No unique ``Teacher`` object found

    Returns:
        Teacher: Unique ``Teacher`` object for given parameters
    """
    if canon is None and non_canon is None and email is None:
        raise APIException("A canon/non-canon name or email is required")
    query_gen = {}
    if canon is not None:
        query_gen["canon"] = canon
    if non_canon is not None:
        query_gen["canon"] = canon
        query_gen["non_canon"] = non_canon
    if email is not None:
        query_gen["email"] = email
        
    query = generate_Q_objects(query_gen)
    teachers_found = Teacher.objects.filter(query)

    #check for 2+, 0, or 1 teacher(s)
    query_count = teachers_found.count()
    print(query_count)
    if query_count >= 2:
        raise APIException((f"Multiple teachers found for given search parameters: {query_gen}",
                         f" Teachers found: {teachers_found}"))
    elif query_count == 1:
        return teachers_found.first() 
    else:
        #if not teacher found in the Teacher's table, fall back to the history table
        query = Q()
        for value in [canon, non_canon]:
            if value is not None:
                query |= Q(name=value)
        try:
            #raises and error if nothing was found or multiple found
            return History.objects.get(query).teacher
        except MultipleObjectsReturned as e:
            print("helper mulitple found")
            raise APIException("No teacher found in Teacher's table. Fell back to History table and multiple objects found")
        except ObjectDoesNotExist as e:
            print("helper none found")
            raise APIException("No teacher found in Teacher's table. Fell back to History table and no objects found")
        except Exception as e:
            print(f"{e}")
            raise APIException("No unique teacher in Teacher table. Fell back to history table and failed to find unqiue teacher")



 
def find_survey(term: str, teacher: Teacher) -> Survey | None:
    try:
        #throws error if no unique object is found
        return Survey.objects.get(cur_term=term, teacher=teacher)
    except Exception as _:
        return None