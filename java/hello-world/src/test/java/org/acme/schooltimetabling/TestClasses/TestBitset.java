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
            checkBitset = BitSetHelper.timeSlotBitSet(time, 1, false, false,
                    false,false,true);
        }
        catch (Exception e){
            /*Shouldn't be throwing and exception*/
        }
        assertEquals(checkBitset.length(), modifyBitset.length());
        assertEquals(checkBitset.length(), 150);

    }
}
