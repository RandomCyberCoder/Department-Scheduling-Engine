package org.acme.schooltimetabling.builders.teachers.policies;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

public class FacultyPolicy implements TeachingPolicy{
    private static final BitSet DEFAULT_FACULTY_CONFLICT = buildDefaultFacultyConflict();

    private final BitSet facultyConflict;

    /**
     * use this when creating schedules. Look at {@link this#buildDefaultFacultyConflict()} to inspect
     * what the faculty conflict is.
     */
    public FacultyPolicy(){
        this.facultyConflict = (BitSet) DEFAULT_FACULTY_CONFLICT.clone();
    }

    /**
     * This is meant to be used for testing purposes
     * @param facultyConflict bitset which faculty can't be scheduled during
     */
    public FacultyPolicy(BitSet facultyConflict){
        this.facultyConflict = (BitSet) facultyConflict.clone();
    }

    @Override
    public boolean isFaculty() {
        return true;
    }

    @Override
    public void apply(BitSet preference, BitSet acceptable, BitSet conflict) {
        preference.andNot(facultyConflict);
        acceptable.andNot(facultyConflict);
        conflict.or(facultyConflict);
    }

    private static BitSet buildDefaultFacultyConflict() {
        final int NUM_BLOCKS_FULL_HOUR = 2;
        String department = ScheduleConfig.getDepartment().toLowerCase();
        final DateTimeFormatter FORMATTER = Constants.TIME_FORMATTER;
        List<LocalTime> facultyTimes;
        EnumSet<Days> facultyDays;
        BitSet facultyConflict = new BitSet();

        if("csc".equals(department)){
            facultyTimes = List.of(
                    LocalTime.parse("11:00AM", FORMATTER)
            );
            facultyDays = EnumSet.of(Days.TUESDAY);
        }
        else if("cpe".equals(department)){
            /*CPE faculty times*/
            facultyTimes = List.of(
                    LocalTime.parse("11:00AM", FORMATTER)
            );
            facultyDays = EnumSet.of(Days.WEDNESDAY);
        }
        else{
            throw new RuntimeException(String.format("the department must be either 'cpe' or 'csc'. '%s' was found " +
                    "instead", department));
        }


        for(LocalTime localTime: facultyTimes) {
            BitSet temp = BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR, facultyDays);
            facultyConflict.or(temp);
        }

        return facultyConflict;
    }
}
