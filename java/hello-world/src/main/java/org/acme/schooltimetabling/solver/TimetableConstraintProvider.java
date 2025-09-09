package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.Teacher;
import org.acme.schooltimetabling.solver.justifications.*;

import java.time.Duration;
import java.util.BitSet;

public class TimetableConstraintProvider implements ConstraintProvider {
    private static final float FLOAT_TIME_DELTA = 0.01f;


    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                sameClassSameDays(constraintFactory),
                teacherLessonConflict(constraintFactory),
                lessonConflict(constraintFactory),
                labActRoomConflict(constraintFactory),
                wrongHoursAmount(constraintFactory),
                wrongRoomType(constraintFactory)

                // Soft constraints
        };
    }
//
//    Constraint roomConflict(ConstraintFactory constraintFactory) {
//        // A room can accommodate at most one lesson at the same time.
//        return constraintFactory
//                // Select each pair of 2 different lessons ...
//                .forEachUniquePair(Lesson.class,
//                        // ... in the same timeslot ...
//                        Joiners.equal(Lesson::getTimeslot),
//                        // ... in the same room ...
//                        Joiners.equal(Lesson::getRoom))
//                // ... and penalize each pair with a hard weight.
//                .penalize(HardSoftScore.ONE_HARD)
//                .justifyWith((lesson1, lesson2, score) -> new RoomConflictJustification(lesson1.getRoom(), lesson1, lesson2))
//                .asConstraint("Room conflict");
//    }
//
//    Constraint teacherConflict(ConstraintFactory constraintFactory) {
//        // A teacher can teach at most one lesson at the same time.
//        return constraintFactory
//                .forEachUniquePair(Lesson.class,
//                        Joiners.equal(Lesson::getTimeslot),
//                        Joiners.equal(Lesson::getTeacher))
//                .penalize(HardSoftScore.ONE_HARD)
//                .justifyWith(
//                        (lesson1, lesson2, score) -> new TeacherConflictJustification(lesson1.getTeacher(), lesson1, lesson2))
//                .asConstraint("Teacher conflict");
//    }
//
//    Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
//        // A student can attend at most one lesson at the same time.
//        return constraintFactory
//                .forEachUniquePair(Lesson.class,
//                        Joiners.equal(Lesson::getTimeslot),
//                        Joiners.equal(Lesson::getStudentGroup))
//                .penalize(HardSoftScore.ONE_HARD)
//                .justifyWith((lesson1, lesson2, score) -> new StudentGroupConflictJustification(lesson1.getStudentGroup(), lesson1, lesson2))
//                .asConstraint("Student group conflict");
//    }
//
//    Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
//        // A teacher prefers to teach in a single room.
//        return constraintFactory
//                .forEachUniquePair(Lesson.class,
//                        Joiners.equal(Lesson::getTeacher))
//                .filter((lesson1, lesson2) -> lesson1.getRoom() != lesson2.getRoom())
//                .penalize(HardSoftScore.ONE_SOFT)
//                .justifyWith((lesson1, lesson2, score) -> new TeacherRoomStabilityJustification(lesson1.getTeacher(), lesson1, lesson2))
//                .asConstraint("Teacher room stability");
//    }
//
//    Constraint teacherTimeEfficiency(ConstraintFactory constraintFactory) {
//        // A teacher prefers to teach sequential lessons and dislikes gaps between lessons.
//        return constraintFactory
//                .forEach(Lesson.class)
//                .join(Lesson.class, Joiners.equal(Lesson::getTeacher),
//                        Joiners.equal((lesson) -> lesson.getTimeslot().getDayOfWeek()))
//                .filter((lesson1, lesson2) -> {
//                    Duration between = Duration.between(lesson1.getTimeslot().getEndTime(),
//                            lesson2.getTimeslot().getStartTime());
//                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
//                })
//                .reward(HardSoftScore.ONE_SOFT)
//                .justifyWith((lesson1, lesson2, score) -> new TeacherTimeEfficiencyJustification(lesson1.getTeacher(), lesson1, lesson2))
//                .asConstraint("Teacher time efficiency");
//    }
//
//    Constraint studentGroupSubjectVariety(ConstraintFactory constraintFactory) {
//        // A student group dislikes sequential lessons on the same subject.
//        return constraintFactory
//                .forEach(Lesson.class)
//                .join(Lesson.class,
//                        Joiners.equal(Lesson::getSubject),
//                        Joiners.equal(Lesson::getStudentGroup),
//                        Joiners.equal((lesson) -> lesson.getTimeslot().getDayOfWeek()))
//                .filter((lesson1, lesson2) -> {
//                    Duration between = Duration.between(lesson1.getTimeslot().getEndTime(),
//                            lesson2.getTimeslot().getStartTime());
//                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
//                })
//                .penalize(HardSoftScore.ONE_SOFT)
//                .justifyWith((lesson1, lesson2, score) -> new StudentGroupSubjectVarietyJustification(lesson1.getStudentGroup(), lesson1, lesson2))
//                .asConstraint("Student group subject variety");
//    }

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
     * @return constraint
     */
    Constraint sameClassSameDays(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                    Joiners.equal(lesson -> lesson.getTeacherObj().getId()),
                    Joiners.equal(Lesson::getCourseID))
                .filter((lesson, lesson2) -> (
                        lesson.getTimeslot().isLecMonday() != lesson2.getTimeslot().isLecMonday() ||
                        lesson.getTimeslot().isLecTuesday() != lesson2.getTimeslot().isLecTuesday() ||
                        lesson.getTimeslot().isLecWednesday() != lesson2.getTimeslot().isLecWednesday() ||
                        lesson.getTimeslot().isLecThursday() != lesson2.getTimeslot().isLecThursday() ||
                        lesson.getTimeslot().isLecFriday() != lesson2.getTimeslot().isLecFriday()
                ))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher has same course on same days");
    }
    /**
     * <p>This constraint checks if the lesson timeslot overlaps with the instructors conflict
     * bitset. If it does it will be penalized with ONE_HARD</p>
     *
     * <p>NOTE: need to think about this more now that a timeslot can have space between... maybe
     * add an xor for faculty that need the dead space that can be possible or make this constraint
     * smarter and check the lab/lec timeslots individually; maybe or the lab&lec timeslot and the AND
     * it with alltimeslot? ... idk</p>
     *
     * @param constraintFactory constraint factory
     * @return constraint
     */
    Constraint teacherLessonConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    BitSet bitset = new BitSet();
                    /*TODO: this bitset takes into account gaps in the bitset
                    *  that are built in. We might need a lecture and lab bitset
                    *  instead and take into account the gap in this constraint or
                    *  another somehow*/
                    bitset.or(lesson.getTimeslot().allTimesBitSet);
                    bitset.and(lesson.getTeacherObj().getConflict());
                    return (bitset.cardinality() > 0);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("TimeSlot has inadequate hours");
    }

    /*make a bit mask for constraints in these next two comments .... might actually not be needed*/

    /*make a constraint for teachers checking if they have a conflict */

    /**
     * <p>The constraint checks that a teacher isn't teaching two classes at the same time.
     * We just intersect the timeslot bitsets.</p>
     *
     * Personal NOTE: Currently I am checking for allTimeBitset which can include breaks. i.e
     * lec+lab time may only be 6hrs but the timeslot has 7 hours. Imagine the hour gap on tuesdays
     * and thursdays.
     * Revisit this later. I'm pretty sure the way right now is okay, but good to note
     *
     *
     *
     */
    Constraint lessonConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacherObj))
                .filter((lesson, lesson2) -> {
                    BitSet bs1 = lesson.getTimeslot().getAllTimesBitSet();
                    BitSet bs2 = lesson2.getTimeslot().getAllTimesBitSet();

                    return bs1.intersects(bs2);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher found teaching more than one course at the same time");
    }

    /*consider making a constraint where a non lab/activity courses have the general room
    * assigned to them OR will this be handled by the other constraints OR make a constraint
    * where we make sure lessons are given the right room. i.e. lecture only courses have the
    * general room and the lab/activity rooms have appropriate room*/

    /*make a constraint for room conflicts; each day should be a seperate event*/

    //problem being solved: check if two lessons are in the same room at the same time.
        //check if on the same day. If yes check if they overlap

    //what we have: a act/lab bitset
    //question: do we need to filter out timeslots that don't hav lab
    Constraint labActRoomConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                //for each lesson
                .forEachUniquePair(Lesson.class,
                        //in the same room
                        Joiners.equal(Lesson::getRoom),
                        //that have a lab/activity
                        Joiners.filtering((lesson, lesson2) -> {
                            //make sure that the lesson requires a lab/activity room
                            return lesson.hasLabAct && lesson2.hasLabAct;
                        }))
                .filter((lesson, lesson2) -> {
                    Timeslot t1 = lesson.getTimeslot();
                    Timeslot t2 = lesson2.getTimeslot();

                    return t1.getLabActBitSet().intersects(t2.getLabActBitSet());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab or Activity room conflict");
    }


    /*make sure that a lesson that needs a lab/activity is in the right room*/



    /*CONSTRAINT check that the course is in the right room type. onlyLec -> general room
    * and lab/act is in the right room(s)*/

    /**
     * <p>This constraint makes sure that the course a lesson represents has been given a timeslot
     * that has the exact amount of hours for the lecture and/or lab/activity the course requires.
     * i.e. a lesson that require 3 lecture hours and 3 lab hours should have a time slot that has
     * no more or less than this amount of lecture and lab hours allocated.</p>
     *
     * <p>implicitly checks that a course with lab/activity is given a timeslot that accommodates for this</p>
     *
     * <p>NOTE FOR FUTURE CHANGE currently doesn't include the new time slot stuff. just working on it getting to work
     * as if we were in the quarter system</p>
     * @param constraintFactory constraint factory
     * @return Constraint
     */
    Constraint wrongHoursAmount(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {

                    /* TODO currently assuming that we can only have lec and then
                        lab/activity on the same day with the same amount of time as currently;
                        Change after MVP is done
                     */
                    int numDays = 0;
                    if(lesson.getTimeslot().lecMonday) numDays++;
                    if(lesson.getTimeslot().lecTuesday) numDays++;
                    if(lesson.getTimeslot().lecWednesday) numDays++;
                    if(lesson.getTimeslot().lecThursday) numDays++;
                    if(lesson.getTimeslot().lecFriday) numDays++;

                    //ts -> timeslot
                    float tsLecHrs = lesson.getTimeslot().getLecHours();
                    tsLecHrs *= numDays;
                    float tsLabActHrs = lesson.getTimeslot().onlyLec ? 0 : tsLecHrs;
                    tsLabActHrs *= numDays;

                    //return true of too many or not enough lec hours or lab/activity hours in the timeslot
                    return !(Math.abs(lesson.lec_hours - tsLecHrs) < FLOAT_TIME_DELTA
                            || Math.abs(lesson.lab_activity_hours - tsLabActHrs)  <  FLOAT_TIME_DELTA);
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .justifyWith((lesson, score) -> new WrongHoursAmountJustification(lesson))
                .asConstraint("Lesson's timeslot must have exact time needed");
    }

    /*constraint: certain lab courses must be in certain rooms*/
    Constraint wrongRoomType(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    Room room = lesson.getRoom();
                    //if lesson has a lab/act
                    if(lesson.isHasLabAct()){
                        /*TODO I realize that I never included what courses have to be in what rooms.
                         * I might have to make a dummy class in case that we have a course with a lab/act that
                         * that we haven't set up a room for or we need to setup some validation.
                         * We need to also set up the mapping from course to room it needs to be in
                         */
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) == room.getID();
                    }
                    //if lesson is lecture only
                    else{
                        //make lecture only lesson has a lecture only room
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) != room.getID();
                    }
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lesson with wrong room type");
    }

    //constraint: make sure no classes during the same time. i.e. checking that an instructor isn't teaching
    //two classes at the same time.

    //make sure that classes don't conflict with hard time constraints where they aren't available

    /*TODO eventually add the prime time stuff*/

    /*TODO primetime constraint*/


}
