from django.core.exceptions import ValidationError

def avail_vailidator(value):
    """Makes sure the value is one of the following: **Preferred**, **Acceptable**, or **Conflict**

    Args:
        value (_type_): field value

    Raises:
        ValidationError: invalid value
    """
    valid_values = ["Preferred", "Acceptable", "Conflict"]
    if value not in valid_values:
        raise ValidationError(f"Acceptable values are {valid_values}")

def pref_validator(value):
    """Ensures the value is one of the following: **Agree**, **Neutral**, or **Disagree**

    Args:
        value (_type_): field value

    Raises:
        ValidationError: invalid value
    """
    valid_value = ["Agree", "Disagree", "Neutral"]
    if value not in valid_value:
        raise ValidationError(f"set up pref validator {valid_value}")

def term_checker(value):
    """Does a bit of valiaiton for the term code Assumes the last digit for the term has the following 
    mapping spring -> 2; winter -> 4; "summer -> 6; fall -> 8

    Args:
        value (_type_): field value

    Raises:
        ValidationError: if the not 4 digits or term code is bad
    """
    FORMAT_MSG = "the termfield format is XYYZ where X is the first digit of the" \
        " year, Y is is the last two digits of the year, and Z is the term code. Term code: spring -> 2; winter -> 4; " \
        "summer -> 6; fall -> 8"
    if len(value) != 4 or value[3] not in ["2", "4", "6", "8"]:
        raise ValidationError(FORMAT_MSG)

    