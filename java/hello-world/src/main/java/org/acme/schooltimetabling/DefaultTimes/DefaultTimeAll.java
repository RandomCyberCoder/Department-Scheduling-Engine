package org.acme.schooltimetabling.defaultTimes;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.util.BitSet;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

public class DefaultTimeAll implements DefaultTime{
    private static final DefaultTimeAll INSTANCE;
    private BitSet conflict;
    private BitSet acceptable;
    private BitSet preference;

    static {
        INSTANCE = new DefaultTimeAll();
        INSTANCE.conflict = new BitSet();
        INSTANCE.acceptable = new BitSet();
        INSTANCE.preference = new BitSet();

        final int NUM_BLOCKS_FULL_HOUR = 2;
        final DateTimeFormatter FORMATTER = Constants.TIME_FORMATTER;
        EnumSet<Days> defaultDays = EnumSet.allOf(Days.class);

        //I'm trying to set preferred times outside of prime time to help solver push more lectures into this time.
        List<LocalTime> preferredTimes = List.of(
                LocalTime.parse("7:00AM", FORMATTER),
                LocalTime.parse("8:00AM", FORMATTER),
                LocalTime.parse("3:00PM", FORMATTER),
                LocalTime.parse("4:00PM", FORMATTER),
                LocalTime.parse("5:00PM", FORMATTER)

        );

        for(LocalTime localTime : preferredTimes){
            INSTANCE.preference.or(BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR, defaultDays));
        }


        List<LocalTime> acceptableTimes = List.of(
                LocalTime.parse("9:00AM", FORMATTER),
                LocalTime.parse("10:00AM", FORMATTER),
                LocalTime.parse("11:00AM", FORMATTER),
                LocalTime.parse("12:00PM", FORMATTER),
                LocalTime.parse("1:00PM", FORMATTER),
                LocalTime.parse("2:00PM", FORMATTER)
        );
        for(LocalTime localTime : acceptableTimes) {
            INSTANCE.acceptable.or(BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR, defaultDays));
        }

        List<LocalTime> conflictTimes = List.of(
                LocalTime.parse("6:00PM", FORMATTER),
                LocalTime.parse("7:00PM", FORMATTER),
                LocalTime.parse("8:00PM", FORMATTER),
                LocalTime.parse("9:00PM", FORMATTER)
        );
        for(LocalTime localTime : conflictTimes) {
            INSTANCE.conflict.or(BitSetHelper.timeSlotBitSet(localTime, NUM_BLOCKS_FULL_HOUR, defaultDays));
        }
    }

    private DefaultTimeAll(){}

    public static DefaultTimeAll getInstance(){
        return INSTANCE;
    }

    @Override
    public BitSet getPreference(){
        return (BitSet) preference.clone();
    }

    @Override
    public BitSet getAcceptable() {
        return (BitSet) acceptable.clone();
    }

    @Override
    public BitSet getConflict() {
        return (BitSet) conflict.clone();
    }
}
