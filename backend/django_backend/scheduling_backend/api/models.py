from django.db import models

# Create your models here.
class Teacher(models.Model):
    canon = models.CharField(max_length=100)
    non_canon = models.CharField(max_length=100)

    def __str__(self):
        return self.canon
    