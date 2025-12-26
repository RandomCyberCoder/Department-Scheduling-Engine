from django.db.models import Q
import pandas as pd
import traceback


def valid_file_extension(file_name: str, extensions: list) -> bool:
    extension = file_name.split(".")[-1]
    if extension  in extensions:
        return True
    return False



#TODO switch this to no needing this and use pandas csv reader to read tsv files
def tsv_to_df(file) -> pd.DataFrame:
    """convert a .tsv file to a pandas data frame

    Args:
        file (_type_): _description_

    Returns:
        pd.DataFrame: pandas data frame representation of the tsv file 
    """
    captured_header = False
    parsed_lines = []

    for line in file:
        parsed_line = line.decode("utf-8").split("\t")
        normalized_line = [value.strip() for value in parsed_line]

        if not captured_header:
            header = [value.lower() for value in normalized_line]
            captured_header = True
            continue

        parsed_lines.append(parsed_line)

    df = pd.DataFrame(data=parsed_lines, columns=header)
    
    return df




def generate_Q_objects(lookup_obj: dict) -> Q:
    """Takes a dictionary with lookup fields to query for a teacher entity. It will OR all the lookup fields together.

    Args:
        lookup_obj (dict): dictionary of lookup fields. Available lookup fields are 'email',
        'canon_name', 'non_canon_name', and 'id'.

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