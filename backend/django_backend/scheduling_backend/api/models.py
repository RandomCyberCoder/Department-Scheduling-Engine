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
    m_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    m_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Tuesday
    t_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    t_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Wednesday
    w_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    w_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Thursday
    r_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    r_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

    # Friday
    f_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")
    f_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator], default="Conflict")

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

