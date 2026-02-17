from django.db import models
from .model_validators.survey_validators import avail_vailidator, pref_validator, term_checker
from django.db.models import UniqueConstraint

class Teacher(models.Model):
    canon = models.CharField(max_length=100, unique=True)
    non_canon = models.CharField(max_length=100, unique=True)
    email = models.EmailField(unique=True, null=True, blank=True)
    cpe = models.BooleanField(default="False")
    csc = models.BooleanField(default="False")
    faculty = models.BooleanField(default="False")

    def __str__(self):
        return self.canon

class History(models.Model):
    # Note: in the DB this field is named teacher_id
    teacher = models.ForeignKey('Teacher', on_delete=models.CASCADE)
    is_canon = models.BooleanField()
    name = models.CharField(max_length=100, unique=True)

    def __str__(self):
        return f"name: {self.name}; canon: {self.is_canon}; teacher_fk: {self.teacher}"

class Survey(models.Model):
    class Meta:
        constraints = [
            UniqueConstraint(fields=["teacher", "cur_term"], name="teacher_term_unique")
        ]

    AVAILABILITY_CHOICES = {
        "Preferred": "Preferred",
        "Conflict": "Conflict",
        "Acceptable": "Acceptable",
        "": "Conflict",
    }

    AGREE_CHOICES = {
        "Agree": "Agree",
        "Disagree": "Disagree",
        "Neutral": "Neutral",
        "": "Disagree",
    }
    #add an id in the serializer
    #start

    #fields for relationships
    #TODO not sure if we actually want this to be null. Maybe no teacher object?
    teacher = models.ForeignKey('Teacher', on_delete=models.CASCADE)
    cur_term = models.CharField(validators=[term_checker])
    prev_term = models.CharField(validators=[term_checker])

    #survey realted fields
    email = models.EmailField(null=True)
    name = models.CharField(max_length=50)
    use_old = models.BooleanField(blank=True)

    # Fields grouped by day (chronological Mon -> Fri)
    # Monday
    M_7_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_8_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_9_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_10_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_11_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_12_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_1_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_2_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_3_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_4_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_5_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_6_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_7_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_8_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    M_9_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Tuesday
    T_7_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_8_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_9_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_10_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_11_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_12_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_1_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_2_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_3_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_4_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_5_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_6_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_7_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_8_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    T_9_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Wednesday
    W_7_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_8_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_9_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_10_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_11_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_12_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_1_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_2_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_3_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_4_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_5_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_6_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_7_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_8_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    W_9_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Thursday
    R_7_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_8_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_9_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_10_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_11_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_12_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_1_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_2_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_3_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_4_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_5_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_6_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_7_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_8_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    R_9_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Friday
    F_7_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_8_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_9_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_10_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_11_AM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_12_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_1_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_2_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_3_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_4_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_5_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_6_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_7_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_8_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    F_9_PM = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")


    # Preference fields (replaced old mwf/tr grouped prefs)
    pref_minDays = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    pref_5days = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    pref_TPD = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    back_to_back = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    gap = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    lecAct_1hrLec = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    lecAct_2hrAct = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    lecAct_noPref = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    lecAct_notSure = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator], default="Disagree")
    
    #doesn't enforce text length
    constraint = models.TextField(blank=True)
    require = models.TextField(blank=True)
    pref = models.TextField(blank=True)
    comment = models.TextField(blank=True)
    stars = models.TextField(blank=True)

