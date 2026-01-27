import pandas as pd
from rest_framework import status
from rest_framework.exceptions import APIException

def file_to_df(file, valid_extensions: list[str]) -> pd.DataFrame:
    """Takes a file and converts it into a pandas dataframe. This funcitons can handle
    CSV, TSV, and excel files. Removes white space from column names

    Args:
        file (_type_): file object
        valid_extensions (list[str]): list of extension you want to allow

    Raises:
        APIException: file extension does not exists in `valid_extensions`

    Returns:
        pd.DataFrame: pandas data frame represenation of the file.
    """
    extension = file.name.split(".")[-1].lower()
    if extension not in valid_extensions:
        raise APIException(detail=f"file extension not allowed. Valid extensions are {valid_extensions}",
                            code=status.HTTP_400_BAD_REQUEST)
    if extension == "csv":
        df = pd.read_csv(file)
    elif extension == 'tsv':
        df = pd.read_csv(file, sep="\t")
    else:
        df = pd.read_excel(file)

    df.columns = df.columns.str.strip()

    return df