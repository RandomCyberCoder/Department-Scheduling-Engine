from django.db import models

# Create your models here.
class Teacher(models.Model):
    canon = models.CharField(max_length=100, unique=True)
    non_canon = models.CharField(max_length=100, unique=True)
    email = models.EmailField(blank=True)
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

class Survey(models.Model):
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

    complete = models.CharField(max_length=100) # models.DateTimeField(db_default=Now())
    name = models.CharField(max_length=50)
    bleed_forward = models.CharField(max_length=150)
    mwf_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    mwf_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    
    tr_7_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_8_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_9_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_10_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_11_am = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_12_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_1_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_2_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_3_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_4_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_5_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_6_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_7_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_8_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)
    tr_9_pm = models.CharField(max_length=20, choices=AVAILABILITY_CHOICES)

    mwf_1 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    tr_1 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mwf_2 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mwf_tr = models.CharField(max_length=20, choices=AGREE_CHOICES)
    tr_2 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mwf_3 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mwf_2_tr_1 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mwf_1_tr_2 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    tr_3 = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mwrf = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mtwr = models.CharField(max_length=20, choices=AGREE_CHOICES)
    mw = models.CharField(max_length=20, choices=AGREE_CHOICES)
    tr = models.CharField(max_length=20, choices=AGREE_CHOICES)
    back_to_back = models.CharField(max_length=20, choices=AGREE_CHOICES)
    gap = models.CharField(max_length=20, choices=AGREE_CHOICES)
    
    constraint = models.TextField()
    require = models.TextField()
    pref = models.TextField()
    comment = models.TextField()
    stars = models.TextField()

