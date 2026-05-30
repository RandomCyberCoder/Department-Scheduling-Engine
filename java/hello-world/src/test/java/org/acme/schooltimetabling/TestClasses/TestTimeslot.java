package org.acme.schooltimetabling.TestClasses;

import net.bytebuddy.asm.Advice;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.TimeslotGenerator;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestTimeslot {
    final int LIST_LEN = 7;
    private static final float EPSILON = 0.01f;
    private static DateTimeFormatter FORMATTER;
    static ArrayList<Timeslot> timeslotList;
    private static final int MONDAY_OFFSET = 0;
    private static final int TUESDAY_OFFSET = 30;
    private static final int WEDNESDAY_OFFSET = 60;
    private static final int THURSDAY_OFFSET = 90;
    private static final int FRIDAY_OFFSET = 120;
    private static final int HOUR_OFFSET = 2;

    @BeforeAll
    static void setUp(){
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        timeslotList = TimeslotGenerator.generateTimeslots("possibletimes.csv");
        FORMATTER = Constants.TIME_FORMATTER;
    }

    @Test
    @DisplayName("Checking list length")
    void checkLength(){
        assertEquals(timeslotList.size(), LIST_LEN);
    }

    @Test
    @DisplayName("Checking # of slot2")
    void checkLectures(){
        AtomicInteger numOfSlot2 = new AtomicInteger();
        timeslotList.forEach(timeslot -> {if(
                timeslot.hasSlot2) {
            numOfSlot2.getAndIncrement();
        }});
        assertEquals(5, numOfSlot2.get());
    }

    //DONE
    @Test
    @DisplayName("Timeslot 1")
    void checkTimeslot1(){
        Timeslot timeslot = timeslotList.get(0);

        EnumSet<Days> expectedDays = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet expected1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("7:00AM", FORMATTER),
                2,
                expectedDays
        );

        BitSet expected2 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("8:00AM", FORMATTER),
                2,
                expectedDays
        );

        BitSet expectedTotal = new BitSet();
        expectedTotal.xor(expected1);
        expectedTotal.xor(expected2);

        assertEquals(1, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("7:00AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("8:00AM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(expectedDays, timeslot.getDaysSlot1());
        assertEquals(expected1, timeslot.getBitSetSlot1());

        assertEquals(1, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot2.cardinality());
        assertEquals(LocalTime.parse("8:00AM", FORMATTER), timeslot.getStartTimeSlot2());
        assertEquals(LocalTime.parse("9:00AM", FORMATTER), timeslot.getEndTimeSlot2());
        assertEquals(expectedDays, timeslot.getDaysSlot2());
        assertEquals(expected2, timeslot.getBitSetSlot2());

        assertEquals(12, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertTrue(timeslot.isHasSlot2());
        assertFalse(timeslot.isContinuous());
    }

    @Test
    @DisplayName("Timeslot 2")
    void checkTimeslot2(){
        Timeslot timeslot = timeslotList.get(1);
        EnumSet<Days> days1 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("8:00AM", FORMATTER),
                2,
                days1
        );

        EnumSet<Days> days2 = EnumSet.of(Days.FRIDAY);
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("8:00AM", FORMATTER),
                4,
                days2
        );

        BitSet expectedTotal = new BitSet();
        expectedTotal.xor(bitSet1);
        expectedTotal.xor(bitSet2);

        assertEquals(1, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(4, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("8:00AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("9:00AM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(days1, timeslot.getDaysSlot1());
        assertEquals(bitSet1, timeslot.getBitSetSlot1());

        assertEquals(2, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(4, timeslot.bitSetSlot2.cardinality());
        assertEquals(LocalTime.parse("8:00AM", FORMATTER), timeslot.getStartTimeSlot2());
        assertEquals(LocalTime.parse("10:00AM", FORMATTER), timeslot.getEndTimeSlot2());
        assertEquals(days2, timeslot.getDaysSlot2());
        assertEquals(bitSet2, timeslot.getBitSetSlot2());

        assertEquals(8, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertTrue(timeslot.isHasSlot2());
        assertFalse(timeslot.isContinuous());
    }

    @Test
    @DisplayName("Timeslot 3")
    void checkTimeslot3(){
        Timeslot timeslot = timeslotList.get(2);

        EnumSet<Days> days1 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("8:00AM", FORMATTER),
                2,
                days1
        );

        EnumSet<Days> days2 = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.FRIDAY);
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("9:00AM", FORMATTER),
                3,
                days2
        );

        BitSet expectedTotal = new BitSet();
        expectedTotal.xor(bitSet1);
        expectedTotal.xor(bitSet2);

        assertEquals(1, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(4, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("8:00AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("9:00AM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(days1, timeslot.getDaysSlot1());
        assertEquals(bitSet1, timeslot.getBitSetSlot1());

        assertEquals(1.5, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(9, timeslot.bitSetSlot2.cardinality());
        assertEquals(LocalTime.parse("9:00AM", FORMATTER), timeslot.getStartTimeSlot2());
        assertEquals(LocalTime.parse("10:30AM", FORMATTER), timeslot.getEndTimeSlot2());
        assertEquals(days2, timeslot.getDaysSlot2());
        assertEquals(bitSet2, timeslot.getBitSetSlot2());

        assertEquals(13, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertTrue(timeslot.isHasSlot2());
        assertFalse(timeslot.isContinuous());
    }

    @Test
    @DisplayName("Timeslot 4")
    void checkTimeslot4(){
        Timeslot timeslot = timeslotList.get(3);

        EnumSet<Days> days1 = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("7:00AM", FORMATTER),
                2,
                days1
        );

        EnumSet<Days> days2 = EnumSet.noneOf(Days.class);
        BitSet bitSet2 = new BitSet();

        BitSet expectedTotal = new BitSet();
        expectedTotal.xor(bitSet1);
        expectedTotal.xor(bitSet2);

        assertEquals(1, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(8, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("7:00AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("8:00AM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(days1, timeslot.getDaysSlot1());
        assertEquals(bitSet1, timeslot.getBitSetSlot1());

        assertEquals(0, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(0, timeslot.bitSetSlot2.cardinality());
        assertEquals(days2, timeslot.getDaysSlot2());
        assertEquals(bitSet2, timeslot.getBitSetSlot2());

        assertEquals(8, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertFalse(timeslot.isHasSlot2());
        assertFalse(timeslot.isContinuous());
    }

    @Test
    @DisplayName("Timeslot 5")
    void checkTimeslot5(){
        Timeslot timeslot = timeslotList.get(4);

        EnumSet<Days> days = EnumSet.of(Days.TUESDAY, Days.THURSDAY);

        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("9:30AM", FORMATTER),
                3,
                days
        );

        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("12:00PM", FORMATTER),
                3,
                days
        );



        BitSet expectedTotal = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("9:30AM", FORMATTER),
                8,
                days
        );

        assertEquals(1.5, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("9:30AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("11:00AM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(days, timeslot.getDaysSlot1());
        assertEquals(bitSet1, timeslot.getBitSetSlot1());

        assertEquals(1.5, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot2.cardinality());
        assertEquals(LocalTime.parse("12:00PM", FORMATTER), timeslot.getStartTimeSlot2());
        assertEquals(LocalTime.parse("1:30PM", FORMATTER), timeslot.getEndTimeSlot2());
        assertEquals(days, timeslot.getDaysSlot2());
        assertEquals(bitSet2, timeslot.getBitSetSlot2());

        assertEquals(16, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertTrue(timeslot.isHasSlot2());
        assertFalse(timeslot.isContinuous());
    }

    @Test
    @DisplayName("Timeslot 6")
    void checkTimeslot6(){
        Timeslot timeslot = timeslotList.get(5);

        EnumSet<Days> days1 = EnumSet.of(Days.FRIDAY);
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("9:00AM", FORMATTER),
                6,
                days1
        );

        EnumSet<Days> days2 = EnumSet.noneOf(Days.class);
        BitSet bitSet2 = new BitSet();

        BitSet expectedTotal = new BitSet();
        expectedTotal.xor(bitSet1);
        expectedTotal.xor(bitSet2);

        assertEquals(3, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("9:00AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("12:00PM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(days1, timeslot.getDaysSlot1());
        assertEquals(bitSet1, timeslot.getBitSetSlot1());

        assertEquals(0, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(0, timeslot.bitSetSlot2.cardinality());
        assertEquals(days2, timeslot.getDaysSlot2());
        assertEquals(bitSet2, timeslot.getBitSetSlot2());

        assertEquals(6, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertFalse(timeslot.isHasSlot2());
        assertTrue(timeslot.isContinuous());
    }

    @Test
    @DisplayName("Timeslot 7")
    void checkTimeslot7(){
        Timeslot timeslot = timeslotList.get(6);

        EnumSet<Days> days1 = EnumSet.of(Days.FRIDAY);
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("9:00AM", FORMATTER),
                6,
                days1
        );

        EnumSet<Days> days2 = EnumSet.of(Days.FRIDAY);
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(
                LocalTime.parse("12:00PM", FORMATTER),
                6,
                days2
        );

        BitSet expectedTotal = new BitSet();
        expectedTotal.xor(bitSet1);
        expectedTotal.xor(bitSet2);

        assertEquals(3, timeslot.getHoursSlot1(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot1.cardinality());
        assertEquals(LocalTime.parse("9:00AM", FORMATTER), timeslot.getStartTimeSlot1());
        assertEquals(LocalTime.parse("12:00PM", FORMATTER), timeslot.getEndTimeSlot1());
        assertEquals(days1, timeslot.getDaysSlot1());
        assertEquals(bitSet1, timeslot.getBitSetSlot1());

        assertEquals(3, timeslot.getHoursSlot2(), EPSILON);
        assertEquals(6, timeslot.bitSetSlot2.cardinality());
        assertEquals(days2, timeslot.getDaysSlot2());
        assertEquals(bitSet2, timeslot.getBitSetSlot2());

        assertEquals(12, timeslot.allTimesBitSet.cardinality());
        assertEquals(expectedTotal, timeslot.getAllTimesBitSet());
        assertTrue(timeslot.isHasSlot2());
        assertFalse(timeslot.isContinuous());
    }
}
