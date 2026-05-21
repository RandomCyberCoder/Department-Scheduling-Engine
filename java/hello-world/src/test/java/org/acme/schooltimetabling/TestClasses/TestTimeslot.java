package org.acme.schooltimetabling.TestClasses;

import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.Generators.TimeslotGenerator;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestTimeslot {
    final int LIST_LEN = 6;
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
    }

    @Test
    @DisplayName("Checking list length")
    void checkLength(){
        assertEquals(timeslotList.size(), LIST_LEN);
    }

    @Test
    @DisplayName("Checking for amount of labs")
    void checkLabs(){
        AtomicInteger numOfLabs = new AtomicInteger();
        timeslotList.forEach(timeslot -> {if(timeslot.hasLabAct) {
            numOfLabs.getAndIncrement();
        }});
        assertEquals(5, numOfLabs.get());
    }

    @Test
    @DisplayName("Checking # of only lectures")
    void checkLectures(){
        AtomicInteger numOfLabs = new AtomicInteger();
        timeslotList.forEach(timeslot -> {if(timeslot.hasLec && !timeslot.hasLabAct) {
            numOfLabs.getAndIncrement();
        }});
        assertEquals(1, numOfLabs.get());
    }

    @Test
    @DisplayName("Timeslot 1")
    void checkTimeslot1(){
        Timeslot timeslot = timeslotList.get(0);
        assertEquals(timeslot.lecHours, 1);
        assertEquals(timeslot.labActBitSet.cardinality(), 6);
        assertEquals(timeslot.lectureBitSet.cardinality(), 6);
        assertEquals(timeslot.allTimesBitSet.cardinality(), 12);
        assertTrue(timeslot.hasLec);
        assertTrue(timeslot.hasLabAct);


    }

    @Test
    @DisplayName("Timeslot 2")
    void checkTimeslot2(){
        Timeslot timeslot = timeslotList.get(1);
        assertEquals(timeslot.lecHours, 1);
        assertEquals(timeslot.lectureBitSet.cardinality(), 4);
        assertEquals(timeslot.labActBitSet.cardinality(), 4);
        assertEquals(timeslot.allTimesBitSet.cardinality(), 8);
        assertTrue(timeslot.hasLec);
        assertTrue(timeslot.hasLabAct);

    }

    @Test
    @DisplayName("Timeslot 3")
    void checkTimeslot3(){
        Timeslot timeslot = timeslotList.get(2);
        assertEquals(timeslot.lecHours, 1);
        assertEquals(timeslot.lectureBitSet.cardinality(), 4);
        assertEquals(timeslot.labActBitSet.cardinality(), 9);
        assertEquals(timeslot.allTimesBitSet.cardinality(), 13);
        assertTrue(timeslot.hasLec);
        assertTrue(timeslot.hasLabAct);
    }

    @Test
    @DisplayName("Timeslot 4")
    void checkTimeslot4(){
        Timeslot timeslot = timeslotList.get(3);
        assertEquals(timeslot.lecHours, 1);
        assertEquals(timeslot.lectureBitSet.cardinality(), 8);
        assertEquals(timeslot.labActBitSet.cardinality(), 0);
        assertEquals(timeslot.allTimesBitSet.cardinality(), 8);
        assertTrue(timeslot.hasLec);
        assertFalse(timeslot.hasLabAct);

    }

    @Test
    @DisplayName("Timeslot 5")
    void checkTimeslot5(){
        Timeslot timeslot = timeslotList.get(4);
        assertEquals(timeslot.lecHours, 1.5);
        assertEquals(timeslot.lectureBitSet.cardinality(), 6);
        assertEquals(timeslot.labActBitSet.cardinality(), 6);
        assertEquals(timeslot.allTimesBitSet.cardinality(), 16);
        assertTrue(timeslot.hasLec);
        assertTrue(timeslot.hasLabAct);

    }

    @Test
    @DisplayName("Timeslot 6")
    void checkTimeslot6(){
        Timeslot timeslot = timeslotList.get(5);
        assertEquals(timeslot.lecHours, 0);
        assertEquals(timeslot.lectureBitSet.cardinality(), 0);
        assertEquals(timeslot.labActBitSet.cardinality(), 6);
        assertEquals(timeslot.allTimesBitSet.cardinality(), 6);
        assertFalse(timeslot.hasLec);
        assertTrue(timeslot.hasLabAct);

    }
}
