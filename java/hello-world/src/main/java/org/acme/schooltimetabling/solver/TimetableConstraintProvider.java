package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.helperClasses.Teacher;
import org.acme.schooltimetabling.solver.justifications.RoomConflictJustification;
import org.acme.schooltimetabling.solver.justifications.StudentGroupConflictJustification;
import org.acme.schooltimetabling.solver.justifications.StudentGroupSubjectVarietyJustification;
import org.acme.schooltimetabling.solver.justifications.TeacherConflictJustification;
import org.acme.schooltimetabling.solver.justifications.TeacherRoomStabilityJustification;
import org.acme.schooltimetabling.solver.justifications.TeacherTimeEfficiencyJustification;

import java.time.Duration;
import java.util.BitSet;

public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                roomConflict(constraintFactory),
                teacherConflict(constraintFactory),
                studentGroupConflict(constraintFactory),
                // Soft constraints
                teacherRoomStability(constraintFactory),
                teacherTimeEfficiency(constraintFactory),
                studentGroupSubjectVariety(constraintFactory)
        };
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        // A room can accommodate at most one lesson at the same time.
        return constraintFactory
                // Select each pair of 2 different lessons ...
                .forEachUniquePair(Lesson.class,
                        // ... in the same timeslot ...
                        Joiners.equal(Lesson::getTimeslot),
                        // ... in the same room ...
                        Joiners.equal(Lesson::getRoom))
                // ... and penalize each pair with a hard weight.
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((lesson1, lesson2, score) -> new RoomConflictJustification(lesson1.getRoom(), lesson1, lesson2))
                .asConstraint("Room conflict");
    }

    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        // A teacher can teach at most one lesson at the same time.
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.equal(Lesson::getTeacher))
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith(
                        (lesson1, lesson2, score) -> new TeacherConflictJustification(lesson1.getTeacher(), lesson1, lesson2))
                .asConstraint("Teacher conflict");
    }

    Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        // A student can attend at most one lesson at the same time.
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.equal(Lesson::getStudentGroup))
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((lesson1, lesson2, score) -> new StudentGroupConflictJustification(lesson1.getStudentGroup(), lesson1, lesson2))
                .asConstraint("Student group conflict");
    }

    Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        // A teacher prefers to teach in a single room.
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher))
                .filter((lesson1, lesson2) -> lesson1.getRoom() != lesson2.getRoom())
                .penalize(HardSoftScore.ONE_SOFT)
                .justifyWith((lesson1, lesson2, score) -> new TeacherRoomStabilityJustification(lesson1.getTeacher(), lesson1, lesson2))
                .asConstraint("Teacher room stability");
    }

    Constraint teacherTimeEfficiency(ConstraintFactory constraintFactory) {
        // A teacher prefers to teach sequential lessons and dislikes gaps between lessons.
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class, Joiners.equal(Lesson::getTeacher),
                        Joiners.equal((lesson) -> lesson.getTimeslot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> {
                    Duration between = Duration.between(lesson1.getTimeslot().getEndTime(),
                            lesson2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
                })
                .reward(HardSoftScore.ONE_SOFT)
                .justifyWith((lesson1, lesson2, score) -> new TeacherTimeEfficiencyJustification(lesson1.getTeacher(), lesson1, lesson2))
                .asConstraint("Teacher time efficiency");
    }

    Constraint studentGroupSubjectVariety(ConstraintFactory constraintFactory) {
        // A student group dislikes sequential lessons on the same subject.
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal((lesson) -> lesson.getTimeslot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> {
                    Duration between = Duration.between(lesson1.getTimeslot().getEndTime(),
                            lesson2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .justifyWith((lesson1, lesson2, score) -> new StudentGroupSubjectVarietyJustification(lesson1.getStudentGroup(), lesson1, lesson2))
                .asConstraint("Student group subject variety");
    }

    //my stuff
    /**
     * <p>This constraint makes sure if an instructor is teaching multiple instances of a course that
     * they all land on the same day. This is essential because teaching different instances of a course
     * on different schedules is a nightmare for the instructor to plan out.</p>
     *
     * <p>This currently doesn't take into account the possibility of labs being scheduled on different
     * days... should it???</p>
     *
     * @param constraintFactory constraint factory
     * @return constraint factory
     */
    Constraint sameClassSameDays(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                    Joiners.equal((lesson) -> lesson.getTeacherObj().getId()),
                    Joiners.equal(Lesson::getCourseID))
                .filter(((lesson, lesson2) -> {
                    return (lesson.getTimeslot().isLecMonday() != lesson2.getTimeslot().isLecMonday() ||
                            lesson.getTimeslot().isLecTuesday() != lesson2.getTimeslot().isLecTuesday() ||
                            lesson.getTimeslot().isLecWednesday() != lesson2.getTimeslot().isLecWednesday() ||
                            lesson.getTimeslot().isLecThursday() != lesson2.getTimeslot().isLecThursday() ||
                            lesson.getTimeslot().isLecFriday() != lesson2.getTimeslot().isLecFriday());
                }))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher has same course on same days");
    }

    /**
     * <p>This constraint checks if the lesson timeslot overlaps with the instructors conflict
     * bitset. If it does it will be penalized with ONE_HARD</p>
     *
     * <p>need to think about this more now that a timeslot can have space between... maybe
     * add an xor for faculty that need the dead space that can be possible or make this constraint
     * smarter and check the lab/lec timeslots individually; maybe or the lab&lec timeslot and the AND
     * it with alltimeslot? ... idk</p>
     *
     * @param constraintFactory constraint factory
     * @return constraint factory
     */
    Constraint teacherLessonConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter((lesson -> {
                    BitSet bitset = new BitSet();
                    bitset.or(lesson.getTimeslot().allTimesBitSet);
                    bitset.and(lesson.getTeacherObj().getConflict());
                    return (bitset.cardinality() > 0);}))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("");
    }
    //make sure no classes during the same time

    //make sure that classes don't conflict with hard time constraints where thaey aren't available

    /*TODO eventually add the prime time stuff*/

}
