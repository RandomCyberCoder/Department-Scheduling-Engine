package org.acme.schooltimetabling.domain.lesson;

import java.util.Comparator;

public class LessonComparator implements Comparator<Lesson> {
    public int compare(Lesson ls1, Lesson ls2){
        //descending order
        //we want things with more lab hours to have scheduling priority
        return Integer.compare(ls2.getLabActHours(), ls1.getLabActHours());
    }
}
