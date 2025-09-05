package org.acme.schooltimetabling.solver.justifications;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import org.acme.schooltimetabling.domain.Lesson;

public record WrongHoursAmountJustification(Lesson lesson1, String description) implements ConstraintJustification{
    public WrongHoursAmountJustification(Lesson lesson1){
        this(lesson1,
                "Lesson name %s; Lesson has lab or act %B; timeslot has only lesson %B"
                        .formatted(lesson1.getCourseName(), lesson1.isHasLabAct(), lesson1.getTimeslot().isOnlyLec()
                        ));
    }
}
