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
