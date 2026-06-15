package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.acme.schooltimetabling.builders.teachers.policies.FacultyPolicy;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.fileObjects.LabPatterns;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.LessonGenerator;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.acme.schooltimetabling.solver.justifications.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.*;
import java.util.*;


public class TimetableConstraintProvider implements ConstraintProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimetableConstraintProvider.class);
    private static final float FLOAT_TIME_DELTA = 0.01f;
    @Override
    /*TODO to consider now that we need to consider we can actually have lessons with only activities. Before this was
    *  technically possible already but the constraints or lesson creation never actually allowed this to happen.
    *  It might be worth making a new constraint for accommodating lab only lessons and those that need lab all in one
    *  block.
    *  Things to consider now in this new change that might not be in here already:
    *  -Lessons can now have lab/act only
    *  -We now have lessons whose lab may be required to be all in one block. Note such lessons will have lab only
    *  -For lessons whose lab days may be on multiple days, the lesson could have a lecture or be lab only
    *  -What the above means is that we now need to be careful how we assign timeslots to a lesson object and how
    *   we check for the correct amount of hours
    *  -timeslots with only one slot, can be used for lessons with either lab/act or lecture only now
    *  -for lecture blocks we have to be careful that it isn't given a timeslot that is continuous
    *  Summary:
    *  -lesson can be: lab + lab/act, lab/act only, or lecture only;
    *  -for lec + lab/act lessons, the lab/act will always be spread out
    *  -for lab/act only lessons, the time might either be all in one block or spread out
    *  -it might be worth making a constraint whose only responsibility is to handle that the right type of slot
    *   is given to a lesson; Actually lets start off doing this while updating all the other constraints
    *   and then figure out if we can merge it; This will be cognitively easier; make global flag for including
    *   only if we have a lesson with lab only that want all the time in one block????*/
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
                timeslotPatternMatch(constraintFactory),
                continuousTimeTaught(constraintFactory),

                // Medium Constraints
                prefTime(constraintFactory),
                compressIndividualTeachTime(constraintFactory),
                rewardPreferredHourGap(constraintFactory),
                penalizeDislikedHourGap(constraintFactory),
                rewardCoursesDiffTimes(constraintFactory)

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
            LOGGER.info("At least one room has been found to be prescheduled. Including the room prescheduling constraint.");
            solver_constraints.add(roomPreschedule(constraintFactory));
        }

        return solver_constraints.toArray(Constraint[]::new);
    }




    //-------------------------------------- Hard Constraints --------------------------------------

    /**
     * <p>This constraint will penalize solutions that have more than 50% of the lecture blocks (each block being
     * 30 minutes) in prime time</p>
     * <p>This constraint is an alternative to using the pair of constraints {@link #inPrimeTime} and
     * {@link #outPrimeTime}.</p>
     * @param constraintFactory constraint factory
     * @return constraint
     */
    Constraint primeTime50Plus(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.hasLecture)
                .groupBy(sum(lesson -> {
                   BitSet inPrime = lesson.maskInPT();
                   BitSet outPrime = lesson.maskOutPT();
                   return inPrime.cardinality() - outPrime.cardinality();
                }))
                .filter(
                        //if the number of blocks in prime time is greater than those out; penalize by one hard
                        blocks -> blocks > 0)
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("50%+ lecture time outside of primetime");
    }

    //TODO Later we must ask beard what about labs in the case that the lab time is all in one block
    /*TODO MORE OF A NOTE BUT THE REASON TEACHER MIGHT GET A LONG DAY IS IF THEY HAVE MULTIPLE INSTANCE OF ONE COURSE
    *  THEN THE WILL HAVE TO TEACH ALL ISNTANCES OF TAHT COURSE (LESSOSN) ON THE SAME DAY; ask about this*/
    //NOTE that I won't take labs into account because I'm not sure if labs matter, especially in the scenario of 3hour blocks
    /**
     * <p>This constraint makes sure if an instructor is teaching multiple instances of a course that
     * they all land on the same day for the lecture only. I do not take labs into account.
     * This is essential because teaching different instances of a course on different schedules is a
     * nightmare for the instructor to plan out.</p>
     *
     * @return Penalize by {@link HardMediumSoftScore#ONE_HARD}
     */
    Constraint sameClassSameDays(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                    Joiners.equal(lesson -> lesson.getTeacherObj().getId()),
                    Joiners.equal(Lesson::getCourseID))
                .filter((lesson, lesson2) -> {
                    /*Penalize courses not on the same day taught by the same professor */
                    EnumSet<Days> lsnSlot1Days = lesson.getTimeslot().getDaysSlot1();
                    EnumSet<Days> lsn2Slot1Days = lesson2.getTimeslot().getDaysSlot1();
                    return lsnSlot1Days.contains(Days.MONDAY) != lsn2Slot1Days.contains(Days.MONDAY) ||
                            lsnSlot1Days.contains(Days.TUESDAY) != lsn2Slot1Days.contains(Days.TUESDAY) ||
                            lsnSlot1Days.contains(Days.WEDNESDAY) != lsn2Slot1Days.contains(Days.WEDNESDAY) ||
                            lsnSlot1Days.contains(Days.THURSDAY) != lsn2Slot1Days.contains(Days.THURSDAY) ||
                            lsnSlot1Days.contains(Days.FRIDAY) != lsn2Slot1Days.contains(Days.FRIDAY);
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Teacher has same course on same days");
    }


    /**
     * <p>This constraint checks if the lesson timeslot overlaps with the instructors conflict
     * bitset. If it does it will be penalized with ONE_HARD.</p>
     *
     * <p>This only checks for the slot1 and slot2 bitsets. This takes into account gaps that are in a timeslot, represented
     * if you unset bits in {@link Timeslot#allTimesBitSet} that are set in <i>slot1</i> and <i>slot2</i>.</p>
     *
     * <p>NOTE: if all faculty should have conflicts during a certain time this is taken into
     * account in {@link FacultyPolicy}</p>
     *
     * @return Penalize by {@link HardMediumSoftScore#ONE_HARD}
     */
    Constraint teacherLessonConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    BitSet bitset = new BitSet();

                    //Use lecture and lab/act bitset rather than the "All" bitset to not take into account gaps
                    bitset.or(lesson.getTimeslot().getBitSetSlot1());
                    bitset.or(lesson.getTimeslot().getBitSetSlot2());

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
     */
    Constraint labActRoomConflict(ConstraintFactory constraintFactory){
        return constraintFactory
                //for each lesson
                .forEachUniquePair(Lesson.class,
                        //in the same room
                        Joiners.equal(Lesson::getRoom),
                        //that have a lab/activity
                        Joiners.filtering((lesson, lesson2) -> {
                            /*make sure that the lesson requires a lab/activity room.
                            * just in case the lesson can be split check if the is studio in case
                            * that the lesson's lecture and lab/act portion got split into separate portions*/
                            return (lesson.isHasLabAct() || lesson.isStudio()) &&
                                    (lesson2.isHasLabAct() || lesson2.isStudio());
                        }))
                .filter((lesson, lesson2) -> {
                    BitSet bitSet1;
                    BitSet bitSet2;

                    //studio courses occupy the room during lecture and lab
                    if(lesson.isStudio()){
                        bitSet1 = new BitSet();
                        bitSet1.or(lesson.getTimeslot().getBitSetSlot1());
                        bitSet1.or(lesson.getTimeslot().getBitSetSlot2());
                    }
                    /*If the split is for a non studio course or course simply has no lecture then the first slot
                     will contain the bitset we need*/
                    else if(!lesson.isHasLecture()) bitSet1 = lesson.getTimeslot().getBitSetSlot1();
                    else bitSet1 = lesson.getTimeslot().getBitSetSlot2();

                    /*same if else logic here for the studio room*/
                    if(lesson2.isStudio()){
                        bitSet2 = new BitSet();
                        bitSet2.or(lesson2.getTimeslot().getBitSetSlot1());
                        bitSet2.or(lesson2.getTimeslot().getBitSetSlot2());
                    }
                    /*If the split is for a non studio course or course simply has no lecture then the first slot
                     will contain the bitset we need*/
                    else if(!lesson.isHasLecture()) bitSet2 = lesson2.getTimeslot().getBitSetSlot1();
                    else bitSet2 = lesson2.getTimeslot().getBitSetSlot2();

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
     *<p>if a lesson has only lecture or lab/act. Then the timeslot should only have the first subslot
     * populated</p>
     */
    Constraint wrongHoursAmount(ConstraintFactory constraintFactory){
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    Timeslot ts = lesson.getTimeslot();
                    /*tests course with only lecture; tests course that has lecture and lab that can be scheduled
                    * normally, basically not a studio course. Tests the lecture lesson portion of a studio course split*/
                    EnumSet<Days> lecDays = EnumSet.noneOf(Days.class);
                    EnumSet<Days> labActDays = EnumSet.noneOf(Days.class);
                    float lecHours = 0f;
                    float labActHours = 0f;

                    //lesson can have both lec and lab/act
                    if(lesson.isHasLecture() && lesson.isHasLabAct()){
                        lecDays = ts.getDaysSlot1();
                        lecHours = ts.getHoursSlot1() * (float) lecDays.size();

                        labActDays = ts.getDaysSlot2();
                        labActHours = ts.getHoursSlot2() * (float) labActDays.size();
                    }
                    //it has lab/act only
                    else if(lesson.isHasLabAct()){
                        labActDays = ts.getDaysSlot1();
                        labActHours = ts.getHoursSlot1() * (float) labActDays.size();
                    }
                    //or it has lecture only
                    else{
                        lecDays = ts.getDaysSlot1();
                        lecHours = ts.getHoursSlot1() * (float) lecDays.size();
                    }

                    //return true of too many or not enough lec hours or lab/activity hours in the timeslot
                    return Math.abs(lesson.getLecHours() - lecHours) > FLOAT_TIME_DELTA
                            || Math.abs(lesson.getLabActHours() - labActHours) > FLOAT_TIME_DELTA;
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .justifyWith((lesson, score) -> new WrongHoursAmountJustification(lesson))
                .asConstraint("Lesson's timeslot must have exact time needed");
    }


    /**
     * <p>Checks that a lesson is in the correct room. If the lesson has a lab/act we make sure it is placed
     * in a lab room. If the lesson is lecture only, then make sure it is placed in the lecture room (aka the
     * universal/general) room, except if it got split, and it's a studio course. Lecture rooms are assumed to be
     * "infinite"</p>
     */
    Constraint wrongRoomType(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    Room room = lesson.getRoom();
                    //use same pattern as above. check if studio then check for lab/act
                    //if lesson has lab/act make sure it goes into lab type room
                    if(lesson.isHasLabAct() || lesson.isStudio()){
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
                    //if lesson is lecture only and the lecture doesn't have to go into a lab room
                    else{
                        //lecture only course should only be in the LEC_ONLY room
                        return Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY) != room.getID();
                    }
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Lesson with wrong room type");
    }


    /**
     * This constraint will look at studio courses and penalize any studio lessons whose lecture blocks are not
     * immediately followed by a lab/act block and any lab time not immediately after lecture blocks
     */
    Constraint studioLabAfterLec(ConstraintFactory constraintFactory){
            return constraintFactory.forEach(Lesson.class)
                    .filter(lesson -> {
                        final int NO_NEXT_BIT_SET = -1;
                        if(!lesson.isStudio() || 
                                //special case if we split up the lesson then this doesn't have to be true;
                                (lesson.isStudio() && (!lesson.isHasLecture() || !lesson.isHasLabAct()))
                        ) return false;
                        final BitSet lecBs = lesson.getTimeslot().getBitSetSlot1();
                        final BitSet labActBs = lesson.getTimeslot().getBitSetSlot2();
                        int endLec;
                        int labActScan = 0;
                        //for all lecture blocks, check that a lab starts right after it
                        for(int lecScan = lecBs.nextSetBit(0);
                            lecScan != NO_NEXT_BIT_SET;
                            lecScan = lecBs.nextSetBit(endLec + 1)
                        ){
                            endLec = lecBs.nextClearBit(lecScan) - 1;
                            boolean labStart = labActBs.get(endLec + 1);
                            int idxNextLab = labActBs.nextSetBit(labActScan);
                            /*penalize this lesson for having a timeslot that doesn't have a lab right after lecture UPDATE*/
                            if(!labStart || idxNextLab != endLec + 1) return true;

                            //set next bit we can search from to the first bit that is not set after the current lab/act block
                            labActScan = labActBs.nextClearBit(idxNextLab);
                        }
                        //check that we don't have surplus lab time blocks
                        return labActBs.nextSetBit(labActScan) != NO_NEXT_BIT_SET;
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
                    //1) skip lessons that are in the lecture only room
                    //2) take into account only lessons that have a room that has prescheduling times
                    //3) if lesson split of lec && lab/act, ignore the lesson portion if lesson isn't studio
                    if(Constants.LEC_ONLY.equals(lesson.getRoom().getName()) ||
                            lesson.getRoom().getPrescheduled().isEmpty() ||
                            (lesson.isHasLecture() && !lesson.isHasLabAct() && !lesson.isStudio())) return false;

                    //penalize lessons that are scheduled in a rooms prescheduled time
                    BitSet prescheduled = lesson.getRoom().getPrescheduled();

                    Timeslot timeslot = lesson.getTimeslot();
                    BitSet usageTime = new BitSet();
                    //scenario where the lesson has both lec and lab
                    if(lesson.isHasLecture() && lesson.isHasLabAct()){
                        usageTime.or(timeslot.getBitSetSlot2());
                        if(lesson.isStudio()) usageTime.or(timeslot.getBitSetSlot1());
                    }
                    //scenario where we may have split a studio courses or the course only has a lab/act
                    else if(lesson.isHasLecture() && lesson.isStudio() || lesson.isHasLabAct()){
                        usageTime.or(timeslot.getBitSetSlot1());
                    }

                    return usageTime.intersects(prescheduled);
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Penalize a lesson ");
    }


    /**
     * Makes sure that if a lesson requires a lab/act time to be all in one block that it gets assigned a timeslot
     * that meets this requirement. It also ensures that timeslots that have their time all in one block don't get
     * assigned to lessons that don't want it.
     */
    Constraint timeslotPatternMatch(ConstraintFactory constraintFactory){
        return  constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    Timeslot timeslot = lesson.getTimeslot();
                    //lab + lab/act
                    if(lesson.hasLecture && lesson.hasLabAct){
                        //in such a configuration the lab/act portion will be spread out; right now no lab with continuous
                        //time are in second slots
                        return !timeslot.hasSlot2 || timeslot.isContinuousSlot1() || timeslot.isContinuousSlot2();
                    }
                    //lab/act only
                    else if(lesson.hasLabAct){
                        return timeslot.isHasSlot2() ||
                                //the next portion depends on if the lab has to be all in one block
                                (lesson.getLabPattern() == LabPatterns.MULTIPLE ?
                                        timeslot.isContinuousSlot1() :
                                        !timeslot.isContinuousSlot1());
                    }
                    //lecture only
                    else{
                        //if this lesson is lecture only timeslot can have only one slot and the time can't be in one block
                        return timeslot.isHasSlot2() || timeslot.isContinuousSlot1();
                    }
                })
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Lesson has the wrong type of timeslot");
    }


    //used for the "continuousTimeTaught" constraint
    private int countTeacherLongBlocks(Teacher teacher, List<Lesson> lessons){
        //aggregate all time being taught into this BitSet
        BitSet scheduledTime = new BitSet();
        //aggregate all unique days taught
        EnumSet<Days> daysTeaching = EnumSet.noneOf(Days.class);
        //keeps track of what lesson pairs have already been counted towards the number of gaps
        Set<Set<Integer>> used = new HashSet<>();
        System.out.println(lessons.size());
        for (Lesson lesson : lessons) {
            Timeslot ts = lesson.getTimeslot();
            scheduledTime.or(ts.getAllTimesBitSet());
            daysTeaching.addAll(lesson.getTimeslot().getDaysSlot1());
            daysTeaching.addAll(lesson.getTimeslot().getDaysSlot2());
        }

        int totalLongBlocks = 0;
        for (Days day : daysTeaching) {
            totalLongBlocks += countLongBlocks(day, scheduledTime, lessons, used);
        }

        //divide to account for potentially multiple classes counted
        return totalLongBlocks;
    }


    //used for the "continuousTimeTaught" constraint
    private int countLongBlocks(Days day, BitSet time, List<Lesson> lessons, Set<Set<Integer>> used){
        //6 hours
        final int MAX_TEACHING_CONTINUOUS_BLOCKS = 12;
        final int OFFSET;
        if(day == Days.MONDAY) OFFSET = BitSetHelper.MONDAY_OFFSET;
        else if(day == Days.TUESDAY) OFFSET = BitSetHelper.TUESDAY_OFFSET;
        else if(day == Days.WEDNESDAY) OFFSET = BitSetHelper.WEDNESDAY_OFFSET;
        else if(day == Days.THURSDAY) OFFSET = BitSetHelper.THURSDAY_OFFSET;
        else OFFSET = BitSetHelper.FRIDAY_OFFSET;
        BitSet bs = time.get(OFFSET, OFFSET + BitSetHelper.MAX_BITS_PER_DAY);

        int count = 0;
        int end;
        for(int start = bs.nextSetBit(0);
            start != -1;
            start = bs.nextSetBit(end + 1)
        ){
            end = bs.nextClearBit(start) - 1;
            if(end - start + 1 > MAX_TEACHING_CONTINUOUS_BLOCKS){
                int first = -1;
                int second = -1;
                for(int i = 0; i < lessons.size(); i++){
                    BitSet lsBs = lessons.get(i).getTimeslot().getAllTimesBitSet();
                    if(lsBs.get(OFFSET + end)) first = i;
                    if(lsBs.get(OFFSET + start)) second = i;
                }
                //need to check that they aren't the same or else the creation of inline set will error
                if(first != second && used.add(Set.of(first, second))) count++;
            }
        }

        return count;
    }


    /**
     * Penalize teachers who teache for more than 6 consecutive hours in one day. Note this only penalizes
     * when courses combined results in more than 6 hours of time back to back. It will not penalize individual
     * timeslots that result in more than 6 hours of continuous time taught.
     */
    Constraint continuousTimeTaught(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacherObj, toList())
                .penalize(HardMediumSoftScore.ONE_HARD,
                        this::countTeacherLongBlocks)
                .asConstraint("Penalize long teaching blocks");
    }

    //-------------------------------------- Medium Constraints --------------------------------------

    /**
     * reward 30-minute blocks that are in an instructor's preferred time
     */
    Constraint prefTime(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(Lesson.class)
                .reward(HardMediumSoftScore.ONE_MEDIUM, lesson -> {
                    final Timeslot ts = lesson.getTimeslot();
                    final Teacher teacher = lesson.getTeacherObj();
                    final BitSet tmp = new BitSet();
                    tmp.or(ts.getBitSetSlot1());
                    tmp.or(ts.getBitSetSlot2());
                    tmp.and(teacher.getPreferences());

                    //reward per hour rather than per 30-minute block
                    return tmp.cardinality() / 2;
                })
                .asConstraint("Reward hour blocks in prof's pref times");
    }

    //used by "compressIndividualTeachTime" constraint
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
            nxtStrt = bs.nextSetBit(end + 1)
        ){

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
    Constraint compressIndividualTeachTime(ConstraintFactory constraintFactory){
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
                        if(Collections.disjoint(daysTeaching, ts.getDaysSlot1())) lsSharedDay = true;
                        if(Collections.disjoint(daysTeaching, ts.getDaysSlot2())) lsSharedDay = true;
                        //collect the days being taught
                        daysTeaching.addAll(ts.getDaysSlot1());
                        daysTeaching.addAll(ts.getDaysSlot2());
                        //accumulate time taught
                        scheduledTime.or(ts.getBitSetSlot1());
                        scheduledTime.or(ts.getBitSetSlot2());
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

    //used by constraints penalizing/rewarding hour gap preference
    /**
     * count number of gaps between lessons for the given <i>day</i>. If a pair of lessons is found in <i>used</i>,
     * it will not be counted towards the total number of gaps in the <i>day</i>.
     *
     * @param day used to mask what bits will be looked at
     * @param time bitset that will be masked according to the <i>day</i> parameter
     * @param lessons list all lessons a teacher has been scheduled
     * @param used set of lesson pairs that have already been used; elements are sets rather than pairs
     *             because the order of the lesson ids in a pair shouldn't matter
     * @return number of gaps found in the <i>day</i>
     */
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

    //used by constraints penalizing/rewarding hour gap preference
    private int countTeacherHourGaps(List<Lesson> lessons) {
        //aggregate all time being taught into this BitSet
        BitSet scheduledTime = new BitSet();
        //aggregate all unique days taught
        EnumSet<Days> daysTeaching = EnumSet.noneOf(Days.class);
        //keeps track of what lesson pairs have already been counted towards the number of gaps
        Set<Set<Integer>> used = new HashSet<>();

        for (Lesson lesson : lessons) {
            Timeslot ts = lesson.getTimeslot();
            scheduledTime.or(ts.getAllTimesBitSet());
            daysTeaching.addAll(lesson.getTimeslot().getDaysSlot1());
            daysTeaching.addAll(lesson.getTimeslot().getDaysSlot2());
        }

        int totalGaps = 0;
        for (Days day : daysTeaching) {
            totalGaps += countHourGaps(day, scheduledTime, lessons, used);
        }

        //divide to account for potentially multiple classes counted
        return totalGaps;
    }

    /**
     * reward hour gaps in an instructor's teaching schedule if they prefer hour gaps between courses
     */
    Constraint rewardPreferredHourGap(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacherObj, toList())
                .filter((teacher, lessons) ->
                        teacher.getGapPref() == Preference.AGREE)
                .reward(HardMediumSoftScore.ONE_MEDIUM,
                        (teacher, lessons) -> countTeacherHourGaps(lessons))
                .asConstraint("Reward teachers who prefer 1-hour gaps");
    }


    /**
     * penalize hour gaps in an instructor's teaching schedule if they dislike hour gaps between courses
     */
    Constraint penalizeDislikedHourGap(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .groupBy(Lesson::getTeacherObj, toList())
                .filter((teacher, lessons) ->
                        teacher.getGapPref() == Preference.DISAGREE)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (teacher, lessons) -> countTeacherHourGaps(lessons))
                .asConstraint("Penalize teachers who dislike 1-hour gaps");
    }


    /**
     * reward different instances of a courses being taught that don't overlap
     */
    Constraint rewardCoursesDiffTimes(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.filtering((lesson1, lesson2) -> {
                            Integer lesson1LinkerId = lesson1.getLinkerId();
                            Integer lesson2LinkerId = lesson2.getLinkerId();

                            return lesson1LinkerId != null
                                    && !lesson1LinkerId.equals(lesson2LinkerId)
                                    && lesson1.getCourseID() == lesson2.getCourseID();
                        }))
                .filter(((lesson1, lesson2) -> {
                    BitSet bitSet1 = lesson1.getTimeslot().getAllTimesBitSet();
                    BitSet bitSet2 = lesson2.getTimeslot().getAllTimesBitSet();

                    return !bitSet1.intersects(bitSet2);
                }))
                .reward(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Reward linked courses at different times");
    }

    //-------------------------------------- Soft Constraints --------------------------------------

    /*at least 50 percent of the time for scheduled Department courses should be outside Prime Time hours
     * https://content-calpoly-edu.s3.amazonaws.com/registrar/1/images/Semester%20Scheduling%20Time%20Patterns%20w%20Footer_12.16.25.pdf
     * lets make this a positive score and */

    /**
     * Rewards 30-minute blocks of lecture time outside prime time.
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
