from typing import TypedDict

class TeacherData(TypedDict):
    id: int
    canon: str
    non_canon: str
    email: str
    cpe: bool
    csc: bool
    faculty: bool
    
