package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.solver.justifications.*;

import java.time.Duration;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

public class TimetableConstraintProvider implements ConstraintProvider {
    private static final float FLOAT_TIME_DELTA = 0.01f;

    /*TODO For the linker I think this should be a special case to handle in the constraint */
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                sameClassSameDays(constraintFactory),
                teacherLessonConflict(constraintFactory),
                lessonConflict(constraintFactory),
                labActRoomConflict(constraintFactory),
                wrongHoursAmount(constraintFactory),
                wrongRoomType(constraintFactory),
                studioSpace(constraintFactory),

                // Medium Constraints

                // Soft constraints
                outPrimeTime(constraintFactory),
                inPrimeTime(constraintFactory)
        };
    }


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
                .filter((lesson, lesson2) -> {
                    EnumSet<Days> l1Days = lesson.getTimeslot().getLecDays();
                    EnumSet<Days> l2Days = lesson2.getTimeslot().getLecDays();
                    return l1Days.contains(Days.MONDAY) != l2Days.contains(Days.MONDAY) ||
                            l1Days.contains(Days.TUESDAY) != l2Days.contains(Days.TUESDAY) ||
                            l1Days.contains(Days.WEDNESDAY) != l2Days.contains(Days.WEDNESDAY) ||
                            l1Days.contains(Days.THURSDAY) != l2Days.contains(Days.THURSDAY) ||
                            l1Days.contains(Days.FRIDAY) != l2Days.contains(Days.FRIDAY);
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
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
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("TimeSlot conflicts with teacher's availability (hard no)");
    }

    /*make a bit mask for constraints in these next two comments .... might actually not be needed*/

    /*make a constraint for teachers checking if they have a conflict */

    /**
     * <p>Checks that a teacher isn't teaching two classes at the same time.</p>
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
                .penalize(HardMediumSoftScore.ONE_HARD)
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
                            return lesson.isHasLabAct() && lesson2.isHasLabAct();
                        }))
                .filter((lesson, lesson2) -> {
                    Timeslot t1 = lesson.getTimeslot();
                    Timeslot t2 = lesson2.getTimeslot();

                    return t1.getLabActBitSet().intersects(t2.getLabActBitSet());
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Lab or Activity room conflict");
    }

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
                    EnumSet<Days> lDays = lesson.getTimeslot().getLecDays();
                    EnumSet<Days> nonLDays = lesson.getTimeslot().getNonLecDays();
                    final Timeslot ts = lesson.getTimeslot();

                    float tsLecHrs = ts.getLecHours();
                    tsLecHrs *= lDays.size();
                    float tsLabActHrs = ts.onlyLec ? 0 : ts.getLabActHours();
                    tsLabActHrs *= nonLDays.size();

                    //return true of too many or not enough lec hours or lab/activity hours in the timeslot
                    return !(Math.abs(lesson.lec_hours - tsLecHrs) < FLOAT_TIME_DELTA)
                            || !(Math.abs(lesson.lab_activity_hours - tsLabActHrs)  <  FLOAT_TIME_DELTA);
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
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
                        /*certain labs/acts can only be in certain rooms*/
                        if(Constants.COURSE_TO_ROOMS.containsKey(lesson.getCourseName())){
                            //check that the room the lesson is given is in the list of valid rooms
                            Set<String> validRooms = Constants.COURSE_TO_ROOMS.get(lesson.getCourseName());
                            return !validRooms.contains(room.getName());
                        }

                        /*if the course doesn't have a specific rooms its lab/act should be in then any
                        * lab/act room is valid for it*/
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) == room.getID();
                    }
                    //if lesson is lecture only
                    else{
                        //lecture only course should only have LEC_ONLY room
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) != room.getID();
                    }
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Lesson with wrong room type");
    }

    /*at least 50 percent of the time for scheduled Department courses should be outside Prime Time hours
     * https://content-calpoly-edu.s3.amazonaws.com/registrar/1/universityscheduling/documents/academic/SchedulingTimePattern112017.pdf
     * lets make this a positive score and */
    Constraint outPrimeTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    BitSet lecBitSet = lesson.getTimeslot().getLectureBitSet();
                    BitSet copy = lecBitSet.get(0
                            , lecBitSet.length());
                    copy.and(BitSetHelper.NON_PRIME_TIME_MASK);

                    return copy.cardinality() != 0;
                })
                .reward(HardMediumSoftScore.ONE_SOFT
                        , lesson -> {
                            BitSet lecBitSet = lesson.getTimeslot().getLectureBitSet();
                            BitSet copy = lecBitSet.get(0
                                    , lecBitSet.length());
                            copy.and(BitSetHelper.NON_PRIME_TIME_MASK);

                            return copy.cardinality();
                        })
                .asConstraint("Rewarding for being outside of prime time");
    }


    Constraint inPrimeTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    BitSet lecBitSet = lesson.getTimeslot().getLectureBitSet();
                    BitSet copy = lecBitSet.get(0
                            , lecBitSet.length());
                    copy.and(BitSetHelper.PRIME_TIME_MASK);

                    return copy.cardinality() != 0;
                })
                .penalize(HardMediumSoftScore.ONE_SOFT
                        , lesson -> {
                            BitSet lecBitSet = lesson.getTimeslot().getLectureBitSet();
                            BitSet copy = lecBitSet.get(0
                                    , lecBitSet.length());
                            copy.and(BitSetHelper.PRIME_TIME_MASK);

                            return copy.cardinality();
                        })
                .asConstraint("Penalizing for being in prime time");
    }


    Constraint studioSpace(ConstraintFactory constraintFactory){
        return  constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    if(!Constants.STUDIO_STYLE_COURSES.contains(lesson.getCourseName()) &&
                    !Constants.TESTING) return false;

                    if(!lesson.isHasLabAct()) return false;

                    if(lesson.getTimeslot().getNonLecDays().size() != 1) return true;

                    BitSet labActBitSet = lesson.getTimeslot().getLabActBitSet();
                    int indexFirstBit = labActBitSet.nextSetBit(0);
                    int cardinality = labActBitSet.cardinality();
                    BitSet mask = new BitSet();
                    mask.set(indexFirstBit, indexFirstBit + cardinality);
                    mask.and(labActBitSet);

                    return mask.cardinality() != cardinality;
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Studio space must be consecutive");
    }
}
