package org.acme.schooltimetabling.helperClasses;
import org.acme.schooltimetabling.constants.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

public class BitSetHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitSetHelper.class);
    public static final int MONDAY_OFFSET = 0;
    public static final int TUESDAY_OFFSET = 30;
    public static final int WEDNESDAY_OFFSET = 60;
    public static final int THURSDAY_OFFSET = 90;
    public static final int FRIDAY_OFFSET = 120;
    private static final int NUM_OF_BITS = 150;
    private static final int PRIME_TIME_DAY_START_OFFSET = 4;
    private static final int PRIME_TIME_DAY_END_OFFSET = 16;
    public static final int MAX_BITS_PER_DAY = 30;
    public static final BitSet NON_PRIME_TIME_MASK;
    public static final BitSet PRIME_TIME_MASK;

    static {
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
        NON_PRIME_TIME_MASK.flip(0, NUM_OF_BITS);
        /* Setting the rest of the bits that could not be set by simply flipping*/
        NON_PRIME_TIME_MASK.set(FRIDAY_OFFSET + PRIME_TIME_DAY_END_OFFSET, FRIDAY_OFFSET + MAX_BITS_PER_DAY);
    }

    /**
     * Returns a BitSet with the appropriate bits set specified by the parameters
     * @param startTime Start time bits will be set
     * @param numberOfBlocks Amount of 30 minute blocks wanted from start time
     * @param days Days bits should be set for
     * @return BitSet with <i>n</i> bits set starting from <i>startTime</i> for given days
     * @throws Exception
     */
    public static BitSet timeSlotBitSet(LocalTime startTime, int numberOfBlocks, EnumSet<Days> days){
        final int LST_POSSIBLE_HR = 22;
        final int FIRST_POSSIBLE_HR = 7;
        /* Each hour has two 30-minute blocks*/
        final int HOUR_OFFSET = 2;
        BitSet bitSet = new BitSet();
        //TODO so it's not so wierd of mapping values. Do math such as hour-7 and multiply by 2 to get offset
        int startHour = startTime.getHour();
        if(startHour < FIRST_POSSIBLE_HR || startHour > LST_POSSIBLE_HR) throw new RuntimeException(String.format(
                "There was an error reading the time '%s'", startTime));
        int dayOffset = (startHour - FIRST_POSSIBLE_HR) * HOUR_OFFSET;

        /* This offset is used mostly for testing*/
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


    /**
     * Give a bitset representation for the survey header column string
     * @param dayTime header string; i.e. W_3_PM
     * @return a BitSet representation of the parameter
     */
    public static BitSet srvHdrToBs(String dayTime){
        final int DAYS_IDX = 0;
        final int HOUR_IDX = 1;
        final int MERIDIEM_IDX = 2;
        final int ONE_HOUR_BLOCK = 2;
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("h:mma");
        String[] fieldNameParsed = dayTime.split("_");
        String formattedTime = String.format("%d:00%s", Integer.valueOf(fieldNameParsed[HOUR_IDX]),
                fieldNameParsed[MERIDIEM_IDX].toUpperCase());
        LocalTime localTime = LocalTime.parse(formattedTime, FORMATTER);
        EnumSet<Days> days = EnumSet.noneOf(Days.class);

        for(Character day: fieldNameParsed[DAYS_IDX].toUpperCase().toCharArray()){
            if(day == 'M') days.add(Days.MONDAY);
            else if(day == 'T') days.add(Days.TUESDAY);
            else if(day == 'W') days.add(Days.WEDNESDAY);
            else if(day == 'R') days.add(Days.THURSDAY);
            else if(day == 'F') days.add(Days.FRIDAY);
            else{
                LOGGER.error("During generation, and invalid day was given. TERMINATING SO IT CAN BE FIXED");
                System.exit(1);
            }
        }

        return BitSetHelper.timeSlotBitSet(localTime, ONE_HOUR_BLOCK, days);
    }

    /**
     * Returns a bitset with one hour worth of bits set starting from the time
     * in the <i>header</i> parameter. Format for header is '&lt;time&gt; &lt;PM/AM&gt;'.
     * If the header is in the format specified it's assumed the time is for Monday, Wednesday,
     * and Friday. If the header is meant for Tuesday and Thursday append a 2 to the end. This format
     * is due to the current survey setup.
     * @param header Header name for the timeslot in the survey
     * @return A BitSet with one hour worth of bits set starting from the time of <i>header</i>
     * @throws Exception
     */
    public static BitSet old_surveyBitset(String header) throws Exception{
        /*bitset for a day is broken into 30min blocks starting from
        * 7:00 AM - 10:00 PM for a total of 30 30-minute blocks per day*/
        BitSet bitset = new BitSet();
        boolean timeTR = "2".equals(header.substring(header.length() -1));
        /*truncate the 2 if this is a TR time*/
        String time = timeTR ? header.substring(0, header.length()-1) : header;

        int dayOffset = switch (time) {
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
        /*Set one hour worth of bits for each day needed*/
        if(timeTR){
            bitset.set(TUESDAY_OFFSET + dayOffset);
            bitset.set(TUESDAY_OFFSET + dayOffset + 1);

            bitset.set(THURSDAY_OFFSET + dayOffset);
            bitset.set(THURSDAY_OFFSET + dayOffset + 1);
        }
        else{
            bitset.set(MONDAY_OFFSET + dayOffset);
            bitset.set(MONDAY_OFFSET + dayOffset + 1);

            bitset.set(WEDNESDAY_OFFSET + dayOffset);
            bitset.set(WEDNESDAY_OFFSET + dayOffset + 1);

            bitset.set(FRIDAY_OFFSET + dayOffset);
            bitset.set(FRIDAY_OFFSET + dayOffset + 1);
        }

        return bitset;
    }
}
