package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.LogSetUp;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.LessonGenerator;
import org.acme.schooltimetabling.solver.justifications.*;

import java.time.Duration;
import java.util.*;

public class TimetableConstraintProvider implements ConstraintProvider {
    private static final float FLOAT_TIME_DELTA = 0.01f;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {

        //studio class specific constraints
        /*NOTE left out due to studio space being different from what I originally thought
        * the true studio constraint can just be taken into account in other constraints*/
        Constraint[] studioConstraint = new Constraint[]{
                studioSpace(constraintFactory),
                studioLabAfterLesson(constraintFactory),
        };

        //mutability
        List<Constraint> solver_constraints = new ArrayList<>(Arrays.asList(
                // Hard constraints
                sameClassSameDays(constraintFactory),
                teacherLessonConflict(constraintFactory),
                lessonConflict(constraintFactory),
                labActRoomConflict(constraintFactory),
                wrongHoursAmount(constraintFactory),
                wrongRoomType(constraintFactory),

                // Medium Constraints

                // Soft constraints
                outPrimeTime(constraintFactory),
                inPrimeTime(constraintFactory)
        ));

        //add studio specific constraints for studio courses
        if(LessonGenerator.studio_detected){
            LogSetUp.LOGGER.info("Studio classes detected. Adding studio specific constraints.");
            solver_constraints.addAll(Arrays.asList(studioConstraint));
        }
        else LogSetUp.LOGGER.info("No studio classes detected, leaving out studio specific constraints");

        return solver_constraints.toArray(new Constraint[0]);
    }


    /**
     * <p>This constraint makes sure if an instructor is teaching multiple instances of a course that
     * they all land on the same day. This is essential because teaching different instances of a course
     * on different schedules is a nightmare for the instructor to plan out.</p>
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
                    //TODO (STUDIO) CHECK the following assumption w/ Beard
                    //for studio style classes we only care about the lecture portion, skip lab/act portion
                    if(lesson.isStudio() && lesson.isHasLabAct() ||
                            lesson2.isStudio() && lesson2.isHasLabAct()){
                        return false;
                    }

                    //compare only lecture days
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
     * bitset. If it does it will be penalized with ONE_HARD.</p>
     *
     * <p>This only checks for the lecture and lab bitsets. This takes into account gaps.</p>
     *
     * @param constraintFactory constraint factory
     * @return constraint
     */
    Constraint teacherLessonConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    BitSet bitset = new BitSet();

                    //Use lecture and lab/act bitset rather than the "All" bitset to not take into account gaps
                    bitset.or(lesson.getTimeslot().getLectureBitSet());
                    bitset.or(lesson.getTimeslot().getLabActBitSet());

                    bitset.and(lesson.getTeacherObj().getConflict());
                    return bitset.cardinality() > 0;
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("TimeSlot conflicts with teacher's availability (hard no)");
    }




    /**
     * <p>Checks that a teacher isn't teaching two lessons at the same time.</p>
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




    /**
     * This constraint will penalize any unique pair of lessons (w/ lab/act) that use the same room
     * at the same time
     *
     * @param constraintFactory - constraint factory
     * @return constraint for one lesson in a room at a time
     */
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
                    BitSet bitSet1;
                    BitSet bitSet2;

                    //pseudocode
                    /*(if) the first one is a studio course we look at the first bitset (currently called the lecture
                    bitset. We worry about the right timeslot being assigned in the "wrongHoursAmount constraint*/
                    /*(else) if not a studio course then we check like normal for the lab/act bitset*/
                    if(lesson.isStudio()) bitSet1 = lesson.getTimeslot().getLectureBitSet();
                    else bitSet1 = lesson.getTimeslot().getLabActBitSet();

                    /*same if else logic here for the studio room*/
                    if(lesson.isStudio()) bitSet2 = lesson2.getTimeslot().getLectureBitSet();
                    else bitSet2 = lesson2.getTimeslot().getLabActBitSet();

                    return bitSet1.intersects(bitSet2);
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
                    /*
                    TODO will have to assign the timeslot differently for studio courses only. for lecture only we also
                     have to add logic to prevent it being given a time slot that has all the time on just one day*/
                    final Timeslot ts = lesson.getTimeslot();
                    //Special case for lab split for studio courses
                    if(lesson.isStudio() && lesson.isHasLabAct()){
                        //lab/act portion of studio course should be continuous; PENALTY if not
                        if(!ts.isContinuous()) return true;
                        float ts_hours = ts.getLecHours();

                        float diff = Math.abs(lesson.getLab_activity_hours() - ts_hours);

                        return diff > FLOAT_TIME_DELTA;
                    }

                    /*Only studio courses should have continuous time. Prevent lecture only courses
                    from taking studio timeslots; PENALTY*/
                    if(ts.isContinuous()) return true;

                    /*tests course with only lecture; tests course that has lecture and lab that can be scheduled
                    * normally, basically not a studio course. Tests the lecture lesson portion of a studio course split*/
                    EnumSet<Days> lDays = ts.getLecDays();
                    EnumSet<Days> nonLDays = ts.getNonLecDays();

                    float tsLecHrs = ts.getLecHours();
                    tsLecHrs *= lDays.size();
                    float tsLabActHrs = ts.onlyLec ? 0 : ts.getLabActHours();
                    tsLabActHrs *= nonLDays.size();

                    //return true of too many or not enough lec hours or lab/activity hours in the timeslot
                    return !(Math.abs(lesson.lec_hours - tsLecHrs) <= FLOAT_TIME_DELTA)
                            || !(Math.abs(lesson.lab_activity_hours - tsLabActHrs)  <=  FLOAT_TIME_DELTA);
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .justifyWith((lesson, score) -> new WrongHoursAmountJustification(lesson))
                .asConstraint("Lesson's timeslot must have exact time needed");
    }

    /**
     * <p>Make sure the lessons are in the correct room. If the lesson has a lab/act we make sure it is placed
     * in a lab room. If the lesson is lecture only the we make sure it is placed in the lecture room (aka the
     * universal/general) room. Lecture rooms assumed to be "infinite"</p>
     *
     * @param constraintFactory constraint factory
     * @return constraint penalizing lessons in the wrong room
     */
    Constraint wrongRoomType(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    Room room = lesson.getRoom();
                    //if lesson has a lab/act
                    if(lesson.isHasLabAct()){
                        /*certain labs/acts can only be in certain rooms*/
                        if(Constants.COURSE_TO_ROOMS.containsKey(lesson.getCourseName())){
                            //check that the room the lesson is in the set of valid rooms
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




    /**
     * Checks studio courses' lab/act portion occurs on one day with consecutive time
     * @param constraintFactory constraint factory
     * @return constraint penalizing studio courses not using studio time
     */
    Constraint studioSpace(ConstraintFactory constraintFactory){
        return  constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    //only check lab/act portion of the studio style split
                    if(!Constants.STUDIO_STYLE_COURSES.contains(lesson.getCourseName()) ||
                            !lesson.isHasLabAct() ||
                            Constants.TESTING) return false;


                    //"lecture" portion will be used for lab/act space
                    //The timeslot should only have the lecture portion set. For a single day
                    return !lesson.getTimeslot().isContinuous();

                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Studio space must be consecutive on a single day");
    }


    /**
     * For studio courses, it makes sure that at least one lecture occurs before the lab occurs
     * //TODO check with beard. Not sure if this is actually a thing but leaving it here just in case
     * @param constraintFactory
     * @return constraint penalizing studio courses that have their lab time before any lecture has taken place
     */
    Constraint studioLabAfterLesson(ConstraintFactory constraintFactory){
        //filter for studio only courses
        //just check the first bit of the lab vs lec bitset. if lab comes before penalty
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getLinker),
                        //skip non-studio classes
                        Joiners.filtering((lesson, lesson2) -> lesson.isStudio() && lesson.isStudio())
                )
                .filter((lesson, lesson2) -> {
                    //NOTE: one lesson will be the lec and the other one will be the lab/act

                    //if first lesson is the lec, get the lecture bitset else use lesson2's bitset
                    final BitSet lecBS = lesson.isHasLecture() ? lesson.getTimeslot().getLectureBitSet() :
                            lesson2.getTimeslot().getLectureBitSet();
                    //if first lesson is the lab, get the lecture (yes the lecture) bitset else use lesson2's bitset
                    final BitSet labBS = lesson.isHasLabAct() ? lesson.getTimeslot().getLectureBitSet() :
                            lesson2.getTimeslot().getLectureBitSet();

                    return labBS.nextSetBit(0) <= lecBS.nextSetBit(0);
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Studio Penalty: lab before all lecture");
    }


    /*at least 50 percent of the time for scheduled Department courses should be outside Prime Time hours
     * https://content-calpoly-edu.s3.amazonaws.com/registrar/1/universityscheduling/documents/academic/SchedulingTimePattern112017.pdf
     * lets make this a positive score and */

    /**
     * Rewards 30-minute blocks of lecture time outside prime time.
     *
     * @param constraintFactory constraint factory
     * @return constraint rewarding lecture time out of prime time
     */
    Constraint outPrimeTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    if(!lesson.hasLecture) return false;

                    /*if the lesson has a lecture then we are guaranteed lecture bitset is being
                     * used for the lecture portion*/
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


    /**
     * Penalizes 30-minute blocks of lecture time in prime time
     *
     * @param constraintFactory constraint factory
     * @return constraint penalizing lecture time in prime time
     */
    Constraint inPrimeTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    if(!lesson.hasLecture) return false;

                    /*if the lesson has a lecture then we are guaranteed the lecture bitset is being
                    * used for the lecture portion*/
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

}
