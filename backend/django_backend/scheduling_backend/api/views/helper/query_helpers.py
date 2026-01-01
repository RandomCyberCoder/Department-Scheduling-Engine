from django.db.models import Q

def generate_Q_objects(lookup_obj: dict) -> Q:
    """Takes a dictionary with lookup fields to query for a teacher entity. It will OR all the lookup fields together.

    Args:
        lookup_obj (dict): dictionary of lookup fields. Available lookup fields are 'email',
        'canon', 'non_canon', and 'id'.

    Returns:
        Q: A Q object to query based on lookup field(s)
    """
    cur_Q = None

    for k, v in lookup_obj.items():
        if k == "email":
            Q_to_add = Q(email=v)
        elif k == "canon":
            Q_to_add = Q(canon=v)
        elif k == "non_canon":
            Q_to_add = Q(non_canon=v)
        elif k == "id":
            Q_to_add = Q(id=v)

        if cur_Q is None:
            cur_Q = Q_to_add
        else: 
            cur_Q |= Q_to_add

    return cur_Q