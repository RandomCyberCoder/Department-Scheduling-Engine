package org.acme.schooltimetabling.domain.teacher;

import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import org.acme.schooltimetabling.TimetableApp;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

/**
 * The Faculty object represents a teacher that is a faculty member. It extends the Teacher object and overrides the
 * {@link #getConflict()}  getConflict}
 */
public class Faculty extends Teacher{
    private static final Logger LOGGER = LoggerFactory.getLogger(Faculty.class);
    /**
     * BitSet representation of the time faculty members can't be scheduled during
     */
    private static final BitSet FACULTY_CONFLICT;

    /*static block for setting up times faculty members can't be scheduled during*/
    static {
        /*enter here the bitsets needed for faculty time*/
        FACULTY_CONFLICT = new BitSet();
        final int NUM_BLOCKS_FULL_HOUR = 2;
        List<LocalTime> facultyTimes;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
        EnumSet<Days> facultyDays;

        try{
            //set special bits for testing
            if(Constants.TESTING){
                LocalTime localTime = LocalTime.parse("9:00PM", formatter);
                BitSet temp = BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR,
                        EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY));
                FACULTY_CONFLICT.or(temp);
            }
            /*CSC faculty times*/
            if(ScheduleConfig.getDepartment().equalsIgnoreCase("csc")){
                facultyTimes = List.of(
                        LocalTime.parse("11:00AM", formatter)
                );
                facultyDays = EnumSet.of(Days.TUESDAY);
            }
            else{
                /*CPE faculty times*/
                facultyTimes = List.of(
                        LocalTime.parse("11:00AM", formatter)
                );
                facultyDays = EnumSet.of(Days.WEDNESDAY);
            }

            /*create the BitSet for faculty conflict*/
            for(LocalTime localTime: facultyTimes) {
                BitSet temp = BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR, facultyDays);
                FACULTY_CONFLICT.or(temp);
            }
        }
        catch (Exception e){
            Faculty.LOGGER.error("Problem creating BitSet mask for the faculty tenure conflict.");
            Faculty.LOGGER.error("Terminating program until error is resolved");
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

    }

    public Faculty(int id, String name, BitSet preferences, BitSet acceptable, BitSet conflict) {
        super(id, name, preferences, acceptable, conflict);
    }

    public Faculty(int id, String name, BitSet preferences, BitSet acceptable, BitSet conflict, Preference gap) {
        super(id, name, preferences, acceptable, conflict, gap);
    }

    public Faculty(Teacher teacher){
        super(teacher);
    }

    /**
     * Needs to be overridden, faculty can't be scheduled during a certain time. Faculty's {@link Teacher#conflict conflict BitSet}
     * is combined with {@link Faculty#FACULTY_CONFLICT BitSet facutly restriction}. Needed for the
     * {@link org.acme.schooltimetabling.solver.TimetableConstraintProvider#teacherLessonConflict teacherLessonConflict}
     * constraint
     * @return BitSet representing faculty's conflict preferences and faculty conflict
     */
    @Override
    public BitSet getConflict() {
        BitSet conflict = super.getConflict();
        BitSet facultyConflictCopy = conflict.get(0, conflict.size());
        facultyConflictCopy.or(FACULTY_CONFLICT);
        return facultyConflictCopy;
    }

    public static BitSet getFacultyConflict() {
        return (BitSet) FACULTY_CONFLICT.clone();
    }
}
