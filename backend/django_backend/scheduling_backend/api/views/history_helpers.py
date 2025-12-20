from ..models import History, Teacher

#TODO sure this is. Look into adding this into Teacher custom update/save functions
def history_save_name(teacher: Teacher, teacher_name: str, canon: bool) -> None:
    """
    Stores a name in the history table for the given teacher primary key

    Parameters:
        teacher_pk: primary key for a teacher entry in the Teacher table; will be
            used as foreign key in the History table
        teacher_name: name of teacher
        canon: True if it's a canon name; Otherwise false
    """
    
    base_data = {
        "teacher": teacher,
        "is_canon": canon
    }

    #if a teacher obj is given to 
    obj, created = History.objects.update_or_create(
        name=teacher_name,
        #defaults for updating if entry with teacher_name eixsts
        defaults=base_data,
        #data for new object
        create_defaults={**base_data, "name": teacher_name}
    )

    print(f"The object -> {obj} \nhas creation status: {created}")