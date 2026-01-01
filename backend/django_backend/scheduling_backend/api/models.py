from django.db import models
from .model_validators.survey_validators import avail_vailidator, pref_validator, term_checker
from django.db.models import UniqueConstraint

#TODO MIGRATE CHANGES
# Create your models here.
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
    teacher = models.ForeignKey('Teacher', on_delete=models.CASCADE)
    cur_term = models.CharField(validators=[term_checker])
    prev_term = models.CharField(validators=[term_checker])
    '''
    TODO not sure about deleting on cascade here. Maybe just insert values of old survey into here
    current implementation just uses the entire old survey som maybe replacing is okay
    '''
    # survey_bled = models.ForeignKey('Survey', on_delete=models.CASCADE)


    #survey realted fields
    email = models.EmailField(null=True)
    name = models.CharField(max_length=50)
    use_old = models.BooleanField(blank=True)

    mwf_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    mwf_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    
    tr_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])
    tr_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES, validators=[avail_vailidator])

    mwf_1 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    tr_1 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mwf_2 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mwf_tr = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    tr_2 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mwf_3 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mwf_2_tr_1 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mwf_1_tr_2 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    tr_3 = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mwrf = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mtwr = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    mw = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    tr = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    back_to_back = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    gap = models.CharField(max_length=20, choices=AGREE_CHOICES, validators=[pref_validator])
    
    #doesn't enforce text length
    constraint = models.TextField(blank=True)
    require = models.TextField(blank=True)
    pref = models.TextField(blank=True)
    comment = models.TextField(blank=True)
    stars = models.TextField(blank=True)

