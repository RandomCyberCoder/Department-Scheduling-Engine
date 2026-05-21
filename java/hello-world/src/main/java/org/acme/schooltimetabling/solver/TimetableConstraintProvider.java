package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.LessonGenerator;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.acme.schooltimetabling.solver.justifications.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.*;
import java.util.*;

public class TimetableConstraintProvider implements ConstraintProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimetableConstraintProvider.class);
    private static final float FLOAT_TIME_DELTA = 0.01f;
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {

        Constraint[] studioConstraint = new Constraint[]{
                studioLabAfterLec(constraintFactory)
        };

        //universal constraints
        List<Constraint> solver_constraints = new ArrayList<>(Arrays.asList(
                // Hard constraints
                sameClassSameDays(constraintFactory),
                teacherLessonConflict(constraintFactory),
                lessonConflict(constraintFactory),
                labActRoomConflict(constraintFactory),
                wrongHoursAmount(constraintFactory),
                wrongRoomType(constraintFactory),

                // Medium Constraints
                prefTime(constraintFactory),
                compressTeachTime(constraintFactory),
                rewardPreferredHourGap(constraintFactory),
                penalizeDislikedHourGap(constraintFactory)

                // Soft constraints
        ));

        //-------------------------- solution specific constraints --------------------------

        //add studio specific constraints for studio courses
        if(LessonGenerator.proper_studio_detected || Constants.TESTING){
            LOGGER.info("Studio classes detected. Adding studio specific constraints.");
            solver_constraints.addAll(Arrays.asList(studioConstraint));
        }
        else LOGGER.info("No studio classes detected, leaving out studio specific constraints");

        final List<String> AGRSV_CHOICES_AVAILABLE = List.of("AGGRESSIVE", "AGGRESSIVE_BOTH", "AGGRESSIVE_PENALTY",
                "AGGRESSIVE_REWARD");
        //Add prime-time constraints based on is we want to use the aggressive solver
        String AGGRESSIVE_CHOICE = ScheduleConfig.getAggressiveChoice();
        if(ScheduleConfig.isTesting()){
            //add them all in for testing purposes
            solver_constraints.addAll(List.of(
                    outPrimeTime(constraintFactory),
                    inPrimeTime(constraintFactory),
                    primeTime50Plus(constraintFactory),
                    outBestTime(constraintFactory),
                    inBestTime(constraintFactory)
            ));
        }
        else if(AGRSV_CHOICES_AVAILABLE.contains(AGGRESSIVE_CHOICE)){
            List<Constraint> chosenThings = new ArrayList<>(List.of(primeTime50Plus(constraintFactory)));
            if ("AGGRESSIVE_BOTH".equals(AGGRESSIVE_CHOICE) || "AGGRESSIVE_PENALTY".equals(AGGRESSIVE_CHOICE)) {
                chosenThings.add(outBestTime(constraintFactory));
            }
            if ("AGGRESSIVE_BOTH".equals(AGGRESSIVE_CHOICE) || "AGGRESSIVE_REWARD".equals(AGGRESSIVE_CHOICE)) {
                chosenThings.add(inBestTime(constraintFactory));
            }
            solver_constraints.addAll(chosenThings);
        }
        else if(AGGRESSIVE_CHOICE.equals("NONE")){
            solver_constraints.addAll(Arrays.asList(outPrimeTime(constraintFactory), inPrimeTime(constraintFactory)));
        }
        else{
            LOGGER.error("Valid aggressive solver choices are: 'AGGRESSIVE', 'AGGRESSIVE_BOTH', 'AGGRESSIVE_PENALTY', " +
                    "'AGGRESSIVE_REWARD', and 'NONE'. TERMINATING PROGRAM");
            System.exit(1);
        }

        //add in the room prescheduling conflict constraint only if we prescheduled rooms detected
        if(Room.hasPrescheduled || ScheduleConfig.isTesting()){
            LOGGER.info("At least on room has been found to be prescheduled. Including the room prescheduling constraint.");
            solver_constraints.add(roomPreschedule(constraintFactory));
        }

        return solver_constraints.toArray(Constraint[]::new);
    }




    //-------------------------------------- Hard Constraints --------------------------------------

    /**
     * <p>This constraint will penalize solutions that have more then 50% of the lecture blocks (each block being
     * 30 minutes) in primetime</p>
     * <p>This constraint is an alternative to using the pair of constraints {@link #inPrimeTime} and
     * {@link #outPrimeTime}.</p>
     * @param constraintFactory constraint factory
     * @return constraint
     */
    Constraint primeTime50Plus(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .groupBy(sum(lesson -> {
                   BitSet inPrime = lesson.maskInPT();
                   BitSet outPrime = lesson.maskOutPT();
                   return inPrime.cardinality() - outPrime.cardinality();
                }))
                .filter(
                        //if the number of blocks in primetime is greater than those out; penalize by one hard
                        blocks -> blocks > 0)
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("50%+ lecture time outside of primetime");
    }

    /**
     * <p>This constraint makes sure if an instructor is teaching multiple instances of a course that
     * they all land on the same day. Technically time should be taken into account; however, the timeslots that are available
     * naturally enforce this.This is essential because teaching different instances of a course
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
                    /*Penalize courses not on the same day taught by the same professor */
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
     * <p>NOTE: if all faculty should have conflicts during a certain time this is taken into
     * account in {@link Faculty#getConflict()}</p>
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
     * <p>Checks that a professor isn't teaching two lessons at the same time.</p>
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
                            /*make sure that the lesson requires a lab/activity room; just in case; don't think it's
                            possible not to have both*/
                            return lesson.isHasLabAct() && lesson2.isHasLabAct();
                        }))
                .filter((lesson, lesson2) -> {
                    BitSet bitSet1;
                    BitSet bitSet2;

                    //studio courses occupy the room during lecture and lab
                    if(lesson.isStudio()){
                        bitSet1 = new BitSet();
                        bitSet1.or(lesson.getTimeslot().getLectureBitSet());
                        bitSet1.or(lesson.getTimeslot().getLabActBitSet());
                    }
                    else bitSet1 = lesson.getTimeslot().getLabActBitSet();

                    /*same if else logic here for the studio room*/
                    if(lesson2.isStudio()){
                        bitSet2 = new BitSet();
                        bitSet2.or(lesson2.getTimeslot().getLectureBitSet());
                        bitSet2.or(lesson2.getTimeslot().getLabActBitSet());
                    }
                    else bitSet2 = lesson2.getTimeslot().getLabActBitSet();

                    //penalize any overlap; aka room being occupied at the same time
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
     * @param constraintFactory constraint factory
     * @return Constraint
     */
    Constraint wrongHoursAmount(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    final Timeslot ts = lesson.getTimeslot();
                    /*tests course with only lecture; tests course that has lecture and lab that can be scheduled
                    * normally, basically not a studio course. Tests the lecture lesson portion of a studio course split*/
                    EnumSet<Days> lDays = ts.getLecDays();
                    EnumSet<Days> nonLDays = ts.getNonLecDays();

                    float tsLecHrs = ts.getLecHours();
                    tsLecHrs *= lDays.size();
                    float tsLabActHrs = ts.getLabActHours();
                    tsLabActHrs *= nonLDays.size();

                    //return true of too many or not enough lec hours or lab/activity hours in the timeslot
                    return Math.abs(lesson.getLecHours() - tsLecHrs) > FLOAT_TIME_DELTA
                            || Math.abs(lesson.getLabActHours() - tsLabActHrs) > FLOAT_TIME_DELTA;
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
                        * lab/act room is valid for it; Penalize if it's in a lecture room*/
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) == room.getID();
                    }
                    //if lesson is lecture only
                    else{
                        //lecture only course should only be in the LEC_ONLY room
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) != room.getID();
                    }
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Lesson with wrong room type");
    }




    /**
     * This constraint will look at studio courses and penalize and studio lessons whose lecture blocks are not
     * immediately followed by a lab/act block
     * @param constraintFactory constraint factory
     * @return penalizes lessons that don't have timeslots that have lab time right after the lecture ends
     */
    Constraint studioLabAfterLec(ConstraintFactory constraintFactory){
            return constraintFactory.forEach(Lesson.class)
                    .filter(lesson -> {
                        final int NO_NEXT_BIT_SET = -1;
                        if(!lesson.isStudio()) return false;
                        final BitSet lecBs = lesson.getTimeslot().getLectureBitSet();
                        final BitSet labActBs = lesson.getTimeslot().getLabActBitSet();
                        int end;
                        //for all lecture blocks, check that a lab starts right after it
                        for(int idx = lecBs.nextSetBit(0); idx != NO_NEXT_BIT_SET; idx = lecBs.nextSetBit(end + 1)){
                            end = lecBs.nextClearBit(idx) - 1;
                            boolean labStart = labActBs.get(end + 1);
                            //penalize this lesson for having a timeslot that doesn't have a lab right after lecture
                            if(!labStart) return true;
                        }

                        return false;
                    })
                    .penalize(HardMediumSoftScore.ONE_HARD)
                    .asConstraint("Studio lab right after lecture");
    }

    /**
     * constraint that will be added in when a prescheduled room is detected. The constraint will ensure that
     * no lesson is scheduled in a room during its preschedule time
     */
    Constraint roomPreschedule(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    //skip lessons that are in the lecture only room
                    //take into account only lessons that have a room that has prescheduling times
                    if(Constants.LEC_ONLY.equals(lesson.getRoom().getName()) ||
                            lesson.getRoom().getPrescheduled().isEmpty()) return false;

                    //penalize lessons that are scheduled in a rooms prescheduled time
                    BitSet prescheduled = lesson.getRoom().getPrescheduled();

                    //studio course both// non studio only lab
                    return prescheduled.intersects(lesson.getTimeslot().getLabActBitSet()) ||
                            (lesson.isStudio() && prescheduled.intersects(lesson.getTimeslot().getLectureBitSet()));
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Penalize a lesson ");
    }

    //-------------------------------------- Medium Constraints --------------------------------------

    Constraint prefTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .reward(HardMediumSoftScore.ONE_MEDIUM, lesson -> {
                    final Timeslot ts = lesson.getTimeslot();
                    final Teacher teacher = lesson.getTeacherObj();
                    final BitSet tmp = new BitSet();
                    tmp.or(ts.getLectureBitSet());
                    tmp.or(ts.getLabActBitSet());
                    tmp.and(teacher.getPreferences());

                    //reward per hour rather than per 30-minute block
                    return tmp.cardinality() / 2;
                })
                .asConstraint("Reward 30min blocks in prof's pref times");
    }

    //cap at 8 hour days
    private static final int MAX_DAY_LEN = 16;
    private static final int MAX_GAP = 6;
    private boolean dayHasLrgGap(Days day, BitSet time){
        final int NO_NEXT_BIT_SET = -1;
        final int OFFSET;
        if(day == Days.MONDAY) OFFSET = BitSetHelper.MONDAY_OFFSET;
        else if(day == Days.TUESDAY) OFFSET = BitSetHelper.TUESDAY_OFFSET;
        else if(day == Days.WEDNESDAY) OFFSET = BitSetHelper.WEDNESDAY_OFFSET;
        else if(day == Days.THURSDAY) OFFSET = BitSetHelper.THURSDAY_OFFSET;
        else OFFSET = BitSetHelper.FRIDAY_OFFSET;
        BitSet bs = time.get(OFFSET, OFFSET + BitSetHelper.MAX_BITS_PER_DAY);

        //skip if empty
        int start = bs.nextSetBit(0);
        if(start == -1) return false;
        int end = bs.nextClearBit(start) - 1;
        for(int nxtStrt = bs.nextSetBit(end + 1);
            nxtStrt != NO_NEXT_BIT_SET;
            nxtStrt = bs.nextSetBit(end + 1)){

            int localDiff = nxtStrt - end - 1;
            //to large a gap between lessons on the given day
            if(localDiff > MAX_GAP) return true;
            //the end of this lec or lab.
            end = bs.nextClearBit(nxtStrt) - 1;
        }

        //check if too much time in a day
        return end - start + 1 > MAX_DAY_LEN;
    }

    /**
     * This constraint penalizes a teacher if there is a gap between lessons more than {@link #MAX_GAP} amount of 30-minute
     * blocks between the end of a lesson and the start of another lesson. It will also penalize a teacher that has
     * a day longer than 8 hours; i.e. the time from when their first lesson starts to when their last lesson ends.
     */
    Constraint compressTeachTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacherObj, toList())
                .filter((teacher, lessons) -> {
                    //constraint is not relevant to anyone teaching only one class
                    if(lessons.size() == 1) return  false;

                    //constraint is not relevant if all lessons don't share a day;
                    EnumSet<Days> daysTeaching = EnumSet.noneOf(Days.class);
                    boolean lsSharedDay = false;
                    BitSet scheduledTime = new BitSet();
                    for(Lesson lesson: lessons){
                        Timeslot ts = lesson.getTimeslot();
                        //flag if we find any two lessons that share a day
                        if(Collections.disjoint(daysTeaching, ts.getLecDays())) lsSharedDay = true;
                        if(Collections.disjoint(daysTeaching, ts.getNonLecDays())) lsSharedDay = true;
                        //collect the days being taught
                        daysTeaching.addAll(ts.getLecDays());
                        daysTeaching.addAll(ts.getNonLecDays());
                        //accumulate time taught
                        scheduledTime.or(ts.getLectureBitSet());
                        scheduledTime.or(ts.getLabActBitSet());
                    }

                    //if no lessons share days, then there is nothing to penalize
                    if(!lsSharedDay) return false;

                    for(Days day: daysTeaching){
                        //if any day has a large gap, penalize
                        if(dayHasLrgGap(day, scheduledTime)) return true;
                    }

                    return false;
                })
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Penalize teacher schedules with large gaps or long days");
    }

    //------------------------

    private int countHourGaps(Days day, BitSet time, List<Lesson> lessons, Set<Set<Integer>> used) {
        // Determine the day's offset
        final int OFFSET;
        if(day == Days.MONDAY) OFFSET = BitSetHelper.MONDAY_OFFSET;
        else if(day == Days.TUESDAY) OFFSET = BitSetHelper.TUESDAY_OFFSET;
        else if(day == Days.WEDNESDAY) OFFSET = BitSetHelper.WEDNESDAY_OFFSET;
        else if(day == Days.THURSDAY) OFFSET = BitSetHelper.THURSDAY_OFFSET;
        else OFFSET = BitSetHelper.FRIDAY_OFFSET;
        BitSet bs = time.get(OFFSET, OFFSET + BitSetHelper.MAX_BITS_PER_DAY);

        int gaps = 0;

        // Start scanning the day's scheduled lessons
        int start = bs.nextSetBit(0);

        while (start != -1) {

            // Find the end of this lesson block
            int end = bs.nextClearBit(start) - 1;

            // Look for the next lesson block
            int nextStart = bs.nextSetBit(end + 1);

            if (nextStart != -1) {
                int gap = nextStart - end - 1;  // number of empty 30-min blocks

                if (gap == 2) {  // exactly 1 hour
                    int first = -1;
                    int second = -1;
                    for(int i = 0; i < lessons.size(); i++){
                        BitSet lsBs = lessons.get(i).getTimeslot().getAllTimesBitSet();
                        if(lsBs.get(OFFSET + end)) first = i;
                        if(lsBs.get(OFFSET + nextStart)) second = i;
                    }
                    //need to check that they aren't the same or else the creation of inline set will error
                    if(first != second && used.add(Set.of(first, second))) gaps++;
                }
            }

            // Move to the next lesson block
            start = nextStart;
        }

        return gaps;
    }

    private int countTeacherHourGaps(List<Lesson> lessons) {
        BitSet scheduledTime = new BitSet();
        EnumSet<Days> daysTeaching = EnumSet.noneOf(Days.class);
        Set<Set<Integer>> used = new HashSet<>();

        for (Lesson lesson : lessons) {
            Timeslot ts = lesson.getTimeslot();
            scheduledTime.or(ts.getAllTimesBitSet());
            daysTeaching.addAll(lesson.getTimeslot().getLecDays());
            daysTeaching.addAll(lesson.getTimeslot().getNonLecDays());
        }

        int totalGaps = 0;
        for (Days day : daysTeaching) {
            totalGaps += countHourGaps(day, scheduledTime, lessons, used);
        }

        //divide to account for potentially multiple classes counted
        return totalGaps;
    }

    Constraint rewardPreferredHourGap(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacherObj, toList())
                .filter((teacher, lessons) ->
                        teacher.getGapPref() == Preference.AGREE)
                .reward(HardMediumSoftScore.ONE_MEDIUM,
                        (teacher, lessons) -> countTeacherHourGaps(lessons))
                .asConstraint("Reward teachers who prefer 1-hour gaps");
    }

    Constraint penalizeDislikedHourGap(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacherObj, toList())
                .filter((teacher, lessons) ->
                        teacher.getGapPref() == Preference.DISAGREE)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (teacher, lessons) -> countTeacherHourGaps(lessons))
                .asConstraint("Penalize teachers who dislike 1-hour gaps");
    }

    //-------------------------------------- Soft Constraints --------------------------------------

    /*at least 50 percent of the time for scheduled Department courses should be outside Prime Time hours
     * https://content-calpoly-edu.s3.amazonaws.com/registrar/1/images/Semester%20Scheduling%20Time%20Patterns%20w%20Footer_12.16.25.pdf
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

                    return lesson.maskOutPT().cardinality() != 0;
                })
                .reward(HardMediumSoftScore.ONE_SOFT
                        , lesson -> lesson.maskOutPT().cardinality())
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
                    return lesson.maskInPT().cardinality() != 0;
                })
                .penalize(HardMediumSoftScore.ONE_SOFT
                        , lesson -> lesson.maskInPT().cardinality())
                .asConstraint("Penalizing for being in prime time");
    }


    /**
     * This constraint is used when we choose the "Aggressive" version of the solver that aims to compress and REWARD
     * time that is in the time interval specified in the configuration file
     */
    Constraint inBestTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.maskInCmprs().cardinality() > 0)
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Reward time in preferred time interval");
    }


    /**
     * This constraint is used when we choose the "Aggressive" version of the solver that aims to compress and PENALIZE
     * time that is in the time interval specified in the configuration file
     */
    Constraint outBestTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.maskOutCmprs().cardinality() > 0)
                .penalize(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Penalize time out of preferred time interval");
    }
}
