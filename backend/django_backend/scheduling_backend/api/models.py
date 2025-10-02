from django.db import models

# Create your models here.
class Teacher(models.Model):
    canon = models.CharField(max_length=100, unique=True)
    non_canon = models.CharField(max_length=100, unique=True)
    cpe = models.BooleanField(default="False")
    csc = models.BooleanField(default="False")
    faculty = models.BooleanField(default="False")

    def __str__(self):
        return self.canon
    