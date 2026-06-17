package org.acme.schooltimetabling.domain.teacher;

import org.acme.schooltimetabling.builders.teachers.TeacherBuilder;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

/**
 * The Faculty object represents a teacher that is a faculty member. It extends the Teacher object.
 * It no longer has any special behavior.
 */
public class Faculty extends Teacher{
    private static final Logger LOGGER = LoggerFactory.getLogger(Faculty.class);
    /**
     * BitSet representation of the time faculty members can't be scheduled during
     */
    private static final BitSet FACULTY_CONFLICT;

    /*THIS IS NO LONGER USED BUT I'M LEAVING IT FOR NOW BECAUSE IT HAS TEST BEHAVIOR; THE FACULTY MASKING IS NOW
    * DONE WHEN I CREATE THE FACULTY OBJECT; L
    *
    * static block for setting up times faculty members can't be scheduled during*/
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

    /**
     * use {@link TeacherBuilder} instead if possible
     */
    public Faculty(int id, String name, BitSet preferences, BitSet acceptable, BitSet conflict) {
        super(id, name, preferences, acceptable, conflict);
    }

    /**
     * use {@link TeacherBuilder} instead if possible
     */
    public Faculty(int id, String name, BitSet preferences, BitSet acceptable, BitSet conflict, Preference gap) {
        super(id, name, preferences, acceptable, conflict, gap);
    }

    public Faculty(Teacher teacher){
        super(teacher);
    }

    public static BitSet getFacultyConflict() {
        return (BitSet) FACULTY_CONFLICT.clone();
    }
}
