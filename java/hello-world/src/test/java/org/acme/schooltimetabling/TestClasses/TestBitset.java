package org.acme.schooltimetabling.TestClasses;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        modifyBitset = new BitSet();
    }

    @Test
    @DisplayName("Making sure that Friday 10PM bit is set")
    void testLastBitSet(){
        BitSet checkBitset = null;
        LocalTime time = LocalTime.parse("9:00PM", DateTimeFormatter.ofPattern("h:mma"));
        modifyBitset.set(149);
        try{
            checkBitset = BitSetHelper.timeSlotBitSet(time, 2, false, false,
                    false,false,true);
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
            checkbitset = BitSetHelper.surveyBitset("9 AM");
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
            checkbitset = BitSetHelper.surveyBitset("9 PM");
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
            checkbitset = BitSetHelper.surveyBitset("10 AM2");
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
            checkbitset = BitSetHelper.surveyBitset("9 PM2");
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
            checkbitset = BitSetHelper.timeSlotBitSet(time, 3, true,false,
                    true,true,false);
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
            checkbitset = BitSetHelper.timeSlotBitSet(time, 4, true,false,
                    false,true,true);
        }
        catch (Exception e){
            /*shouldn't error*/
            e.printStackTrace();
        }
        assertEquals(checkbitset.cardinality(), 12);
        assertEquals(checkbitset.length(), FRIDAY_OFFSET + HOUR_OFFSET * 7);
    }

}
