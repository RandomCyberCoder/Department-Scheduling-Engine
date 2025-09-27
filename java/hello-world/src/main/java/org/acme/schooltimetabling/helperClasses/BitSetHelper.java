package org.acme.schooltimetabling.helperClasses;
import org.acme.schooltimetabling.constants.Days;
import org.apache.poi.hssf.record.CFHeaderRecord;
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STUnsignedDecimalNumber;

import java.time.LocalTime;
import java.util.BitSet;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.EnumSet;

public class BitSetHelper {

    private static final int MONDAY_OFFSET = 0;
    private static final int TUESDAY_OFFSET = 30;
    private static final int WEDNESDAY_OFFSET = 60;
    private static final int THURSDAY_OFFSET = 90;
    private static final int FRIDAY_OFFSET = 120;
    private static final int PRIME_TIME_DAY_START_OFFSET = 4;
    private static final int PRIME_TIME_DAY_END_OFFSET = 16;
    private static final int MAX_BITS_PER_DAY = 30;
    public static final BitSet MONDAY_MASK;
    public static final BitSet TUESDAY_MASK;
    public static final BitSet WEDNESDAY_MASK;
    public static final BitSet THURSDAY_MASK;
    public static final BitSet FRIDAY_MASK;
    public static final BitSet NON_PRIME_TIME_MASK;
    public static final BitSet PRIME_TIME_MASK;

    static {
        MONDAY_MASK = new BitSet();
        MONDAY_MASK.set(MONDAY_OFFSET, TUESDAY_OFFSET);

        TUESDAY_MASK = new BitSet();
        TUESDAY_MASK.set(TUESDAY_OFFSET, WEDNESDAY_OFFSET);

        WEDNESDAY_MASK = new BitSet();
        WEDNESDAY_MASK.set(WEDNESDAY_OFFSET, THURSDAY_OFFSET);

        THURSDAY_MASK = new BitSet();
        THURSDAY_MASK.set(THURSDAY_OFFSET, FRIDAY_OFFSET);

        FRIDAY_MASK = new BitSet();
        FRIDAY_MASK.set(FRIDAY_OFFSET, FRIDAY_OFFSET + 30);

        PRIME_TIME_MASK = new BitSet();
        PRIME_TIME_MASK.set(MONDAY_OFFSET + PRIME_TIME_DAY_START_OFFSET
                , MONDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET);
        PRIME_TIME_MASK.set(TUESDAY_OFFSET + PRIME_TIME_DAY_START_OFFSET
                , TUESDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET);
        PRIME_TIME_MASK.set(WEDNESDAY_OFFSET + PRIME_TIME_DAY_START_OFFSET
                , WEDNESDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET);
        PRIME_TIME_MASK.set(THURSDAY_OFFSET + PRIME_TIME_DAY_START_OFFSET
                , THURSDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET);
        PRIME_TIME_MASK.set(FRIDAY_OFFSET + PRIME_TIME_DAY_START_OFFSET
                , FRIDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET);

        NON_PRIME_TIME_MASK = (BitSet) PRIME_TIME_MASK.clone();
        /* The bits not set in the prime time bitset mask should be set*/
        NON_PRIME_TIME_MASK.flip(0, NON_PRIME_TIME_MASK.length());
        /* Setting the rest of the bits that could not be set by simply flipping*/
        NON_PRIME_TIME_MASK.set(FRIDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET, FRIDAY_OFFSET + MAX_BITS_PER_DAY);
    }

    public static BitSet timeSlotBitSet(LocalTime startTime, int numberOfBlocks, EnumSet<Days> days) throws Exception{
        BitSet bitSet = new BitSet();
        int dayOffset = 0;

        dayOffset = switch (startTime.getHour()) {
            case 7 -> 0; // 7 AM
            case 8 -> 2;
            case 9 -> 4;
            case 10 -> 6;
            case 11 -> 8;
            case 12 -> 10;
            case 13 -> 12;
            case 14 -> 14;
            case 15 -> 16;
            case 16 -> 18;
            case 17 -> 20;
            case 18 -> 22;
            case 19 -> 24;
            case 20 -> 26;
            case 21 -> 28; //9 PM
            default ->
                    throw new Exception(String.format("There was an error reading the time '%s'", startTime.toString()));
        };

        /*I'm not adding the possiblity for 30 minute localtime here. I don't think this was an issue during the
        * nomral planning but we should add it here for the testing*/
        /* Move offset forward one bit for offset if the time starts 30 minutes after the hour*/
        dayOffset += startTime.getMinute() == 30 ? 1 : 0;

        /* the 'to index' (the second index passed) to BitSet.set() is exclusive*/
        if(days.contains(Days.MONDAY)){
            bitSet.set(MONDAY_OFFSET +  dayOffset, MONDAY_OFFSET + dayOffset + numberOfBlocks);
        }
        if(days.contains(Days.TUESDAY)){
            bitSet.set(TUESDAY_OFFSET +  dayOffset, TUESDAY_OFFSET + dayOffset + numberOfBlocks);
        }
        if(days.contains(Days.WEDNESDAY)){
            bitSet.set(WEDNESDAY_OFFSET +  dayOffset, WEDNESDAY_OFFSET + dayOffset + numberOfBlocks);
        }
        if(days.contains(Days.THURSDAY)){
            bitSet.set(THURSDAY_OFFSET +  dayOffset, THURSDAY_OFFSET + dayOffset + numberOfBlocks);
        }
        if(days.contains(Days.FRIDAY)){
            bitSet.set(FRIDAY_OFFSET +  dayOffset, FRIDAY_OFFSET + dayOffset + numberOfBlocks);
        }

        return bitSet;
    }

    public static BitSet surveyBitset(String header) throws Exception{
        /*bitset for a day is broken into 30min blocks starting from
        * 7:00 AM - 10:00 PM for a total of 30 30-minute blocks per day*/
        BitSet bitset = new BitSet();
        int mondayOffset = 0;
        int tuesdayOffset = 30;
        int wednesdayOffset = 60;
        int thursdayOffset = 90;
        int fridayOffset = 120;
        int dayOffset = 0;
        boolean timeTR = "2".equals(header.substring(header.length() -1));
        /*truncate the 2 if this is a TR time*/
        String time = timeTR ? header.substring(0, header.length()-1) : header;

        dayOffset = switch (time) {
            case "7 AM" -> 0;
            case "8 AM" -> 2;
            case "9 AM" -> 4;
            case "10 AM" -> 6;
            case "11 AM" -> 8;
            case "12 PM" -> 10;
            case "1 PM" -> 12;
            case "2 PM" -> 14;
            case "3 PM" -> 16;
            case "4 PM" -> 18;
            case "5 PM" -> 20;
            case "6 PM" -> 22;
            case "7 PM" -> 24;
            case "8 PM" -> 26;
            case "9 PM" -> 28;
            default ->
                    throw new Exception(String.format("There was an error reading the time from survey with header '%s'", header));
        };
        
        /*check if the time is for TR or MWF*/
        /*Note that we set <dayOffWeek>Offset + dayOffset for first
        * 30 minutes of the hour then add one for the second 30 minutes
        * of the hour*/
        if(timeTR){
            /*set Tuesday bits*/
            bitset.set(tuesdayOffset + dayOffset);
            bitset.set(tuesdayOffset + dayOffset + 1);
            /*set Thursday bits*/
            bitset.set(thursdayOffset + dayOffset);
            bitset.set(thursdayOffset + dayOffset + 1);
        }
        else{
            /*set Monday bits*/
            bitset.set(mondayOffset + dayOffset);
            bitset.set(mondayOffset + dayOffset + 1);
            /*set Wednesday bits*/
            bitset.set(wednesdayOffset + dayOffset);
            bitset.set(wednesdayOffset + dayOffset + 1);
            /*set Friday bits */
            bitset.set(fridayOffset + dayOffset);
            bitset.set(fridayOffset + dayOffset + 1);
        }

        return bitset;
    }
}
