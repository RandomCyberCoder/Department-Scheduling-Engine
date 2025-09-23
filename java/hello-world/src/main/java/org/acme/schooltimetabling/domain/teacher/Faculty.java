package org.acme.schooltimetabling.domain.teacher;

import org.acme.schooltimetabling.TimetableApp;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

public class Faculty extends Teacher{
    private static final Logger LOGGER = LoggerFactory.getLogger(Faculty.class);
    static final BitSet FACULTY_CONFLICT;
    static {
        /*enter here the bitsets needed for faculty time*/
        FACULTY_CONFLICT = new BitSet();
        final int NUM_BLOCKS_FULL_HOUR = 2;
        List<LocalTime> facultyTimes;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
        final EnumSet<Days> FACULTY_DAYS = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        try{

            if(ParseInput.scheduleConfig.department.equalsIgnoreCase("csc")){
                facultyTimes = List.of(
                        LocalTime.parse("1:00PM", formatter),
                        LocalTime.parse("2:00PM", formatter)
                );
            }
            else{
                /*CPE*/
                facultyTimes = List.of(
                        LocalTime.parse("12:00PM", formatter),
                        LocalTime.parse("1:00PM", formatter),
                        LocalTime.parse("2:00PM", formatter)
                );
            }

            for(LocalTime localTime: facultyTimes) {
                /*The tw*/
                BitSet temp = BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR, FACULTY_DAYS);
                FACULTY_CONFLICT.and(temp);
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

    @Override
    public BitSet getConflict() {
        BitSet conflict = super.getConflict();
        BitSet facultyConflictCopy = conflict.get(0, conflict.size());
        facultyConflictCopy.and(FACULTY_CONFLICT);
        return facultyConflictCopy;
    }
}
