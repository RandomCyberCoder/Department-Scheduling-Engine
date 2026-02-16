package org.acme.schooltimetabling.TestClasses;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

public class TestBitset {
    private static final int MONDAY_OFFSET = 0;
    private static final int TUESDAY_OFFSET = 30;
    private static final int WEDNESDAY_OFFSET = 60;
    private static final int THURSDAY_OFFSET = 90;
    private static final int FRIDAY_OFFSET = 120;
    private static final int HOUR_OFFSET = 2;
    BitSet modifyBitset;
    @BeforeEach
    void setUp(){
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        modifyBitset = new BitSet();
    }

    @Test
    @DisplayName("Making sure that Friday 10PM bit is set")
    void testLastBitSet(){
        BitSet checkBitset = null;
        LocalTime time = LocalTime.parse("9:00PM", DateTimeFormatter.ofPattern("h:mma"));
        modifyBitset.set(149);
        try{
            checkBitset = BitSetHelper.timeSlotBitSet(time, 2, EnumSet.of(Days.FRIDAY));
        }
        catch (Exception e){
            /*Shouldn't be throwing and exception*/
        }
        assertEquals(checkBitset.length(), modifyBitset.length());
        assertEquals(checkBitset.length(), 150);
    }

    @Test
    @DisplayName("MWF morning")
    void MWF_surveyBitset(){
        BitSet checkbitset = null;
        try{
            checkbitset = BitSetHelper.old_surveyBitset("9 AM");
        }
        catch (Exception e){
            //shouldn't throw an error
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 6);
        assertEquals(checkbitset.length(), FRIDAY_OFFSET + 3 *  HOUR_OFFSET);
    }

    @Test
    @DisplayName("MWF night")
    void MWF_night_surveyBitset(){
        BitSet checkbitset = null;
        try{
            checkbitset = BitSetHelper.old_surveyBitset("9 PM");
        }
        catch (Exception e){
            //shouldn't throw an error
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 6);
        assertEquals(checkbitset.length(), FRIDAY_OFFSET + 15 *  HOUR_OFFSET);
    }

    @Test
    @DisplayName("TH morning")
    void TH_surveyBitset(){
        BitSet checkbitset = null;
        try{
            checkbitset = BitSetHelper.old_surveyBitset("10 AM2");
        }
        catch (Exception e){
            //shouldn't throw an error
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 4);
        assertEquals(checkbitset.length(), THURSDAY_OFFSET + 4 *  HOUR_OFFSET);
    }

    @Test
    @DisplayName("TH night")
    void TH_night_surveyBitset(){
        BitSet checkbitset = null;
        try{
            checkbitset = BitSetHelper.old_surveyBitset("9 PM2");
        }
        catch (Exception e){
            //shouldn't throw an error
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 4);
        assertEquals(checkbitset.length(), THURSDAY_OFFSET + 15 *  HOUR_OFFSET);
    }

    @Test
    @DisplayName("Timeslot BitSet Creation")
    void MWT_timeslot_Bitset(){
        LocalTime time = LocalTime.parse("9:00AM", DateTimeFormatter.ofPattern("h:mma"));
        BitSet checkbitset = null;
        try{
            checkbitset = BitSetHelper.timeSlotBitSet(time, 3, EnumSet.of(Days.MONDAY, Days.WEDNESDAY
                            ,Days.THURSDAY));
        }
        catch (Exception e){
            /*shouldn't error*/
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 9);
        assertEquals(checkbitset.length(), THURSDAY_OFFSET + HOUR_OFFSET * 3 + 1);
    }

    @Test
    @DisplayName("Timeslot BitSet Creation evening")
    void MWT_timeslot_evening_Bitset(){
        LocalTime time = LocalTime.parse("12:00PM", DateTimeFormatter.ofPattern("h:mma"));
        BitSet checkbitset = null;
        try{
            checkbitset = BitSetHelper.timeSlotBitSet(time, 4, EnumSet.of(Days.MONDAY, Days.THURSDAY
                    ,Days.FRIDAY));
        }
        catch (Exception e){
            /*shouldn't error*/
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 12);
        assertEquals(checkbitset.length(), FRIDAY_OFFSET + HOUR_OFFSET * 7);
    }

    @Test
    @DisplayName("Check that the PrimeTime masks are set correctly")
    void primeTime_BitSet_check(){
        final int BITS_PER_DAY = 30;
        final int NUM_DAYS = 5;
        final int TOTAL_BITS = BITS_PER_DAY * NUM_DAYS;
        final int PRIME_TIME_BITS_PER_DAY = 12;
        final int NON_PRIME_TIME_BITS_PER_DAY = 18;
        final BitSet INTERSECTION_TEST = (BitSet) BitSetHelper.PRIME_TIME_MASK.clone();
        INTERSECTION_TEST.and(BitSetHelper.NON_PRIME_TIME_MASK);

        assertAll(
                "Checking all prime time related bitsets",
                () -> assertEquals(TOTAL_BITS
                        , BitSetHelper.PRIME_TIME_MASK.cardinality()
                                + BitSetHelper.NON_PRIME_TIME_MASK.cardinality()),
                () -> assertEquals(INTERSECTION_TEST.cardinality(), 0),
                () -> assertEquals(PRIME_TIME_BITS_PER_DAY * NUM_DAYS
                        , BitSetHelper.PRIME_TIME_MASK.cardinality()),
                () -> assertEquals(NON_PRIME_TIME_BITS_PER_DAY * NUM_DAYS
                        , BitSetHelper.NON_PRIME_TIME_MASK.cardinality())
        );
    }

}
