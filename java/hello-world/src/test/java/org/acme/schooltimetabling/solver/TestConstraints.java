package org.acme.schooltimetabling.solver;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Timetable;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

public class TestConstraints {
    ConstraintVerifier<TimetableConstraintProvider, Timetable> constraintVerifier = ConstraintVerifier.build(
            new TimetableConstraintProvider(), Timetable.class, Lesson.class);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");

    @Test
    @DisplayName("Testing same teacher same course constraint")
    void sameLessonNTeacher() throws Exception{
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher = new Teacher(1, "dummyInstructor", new BitSet(), new BitSet(), new BitSet());
        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);

        Timeslot timeslot1 = Timeslot.test_CreateWithDaysOnly(1, enumSet, enumSet);
        Lesson lesson1 = new Lesson("1", 1, "csc201", "dummyInstructor", "",
                "3-1-0", 1, teacher, timeslot1, room);

        Timeslot timeslot2 = Timeslot.test_CreateWithDaysOnly(2, enumSet, enumSet);
        Lesson lesson2 = new Lesson("2", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, teacher, timeslot2, room);

        enumSet.add(Days.TUESDAY);
        Timeslot timeslot3 = Timeslot.test_CreateWithDaysOnly(2, enumSet, enumSet);
        Lesson lesson3 = new Lesson("3", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, teacher, timeslot3, room);
        constraintVerifier.verifyThat(TimetableConstraintProvider::sameClassSameDays)
                .given(lesson1, lesson2, lesson3)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(2);

    }

    @Test
    @DisplayName("teacher conflict times & timeslot conflict")
    void teacherAndTimeslot() throws Exception{
        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        Room room = new Room("1", "dummyRoom", 1);
        BitSet teacher1Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 4, enumSet);
        Teacher teacher1 = new Teacher(1, "instructor1", new BitSet(), new BitSet(), teacher1Bits);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter)
                , 4, enumSet);
        Timeslot timeslot1 = Timeslot.test_lacLabBitAndDays(1, ts1, new BitSet(), enumSet, enumSet);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, timeslot1, room);

        BitSet teacher2Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 4, enumSet);
        Teacher teacher2 = new Teacher(1, "instructor1", new BitSet(), new BitSet(), teacher2Bits);
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:30AM", formatter)
                , 1, enumSet);
        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(1, ts2, new BitSet(), enumSet, enumSet);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher2, timeslot2, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherLessonConflict)
                .given(lesson1, lesson2)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Faculty override conflict & timeslot conflict")
    void facultyAndTimeslot() throws Exception{
        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        Room room = new Room("1", "dummyRoom", 1);

        Teacher teacher2 = new Faculty(1, "instructor1", new BitSet(), new BitSet(), new BitSet());
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter)
                , 1, enumSet);
        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(1, ts2, new BitSet(), enumSet, enumSet);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher2, timeslot2, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherLessonConflict)
                .given(lesson2)
                .penalizesBy(1);
    }

    @Test
    @DisplayName("Test: teacher can't teach two lessons at the same time")
    void teacherLessonSameTime() throws Exception{
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher1 = new Faculty(1, "instructor1", new BitSet(), new BitSet(), new BitSet());

        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter)
                , 6, enumSet);
        Timeslot timeslot1 = Timeslot.test_lacLabBitAndDays(1, ts1, new BitSet(), enumSet, enumSet);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, timeslot1, room);


        EnumSet<Days> enumSet2 = EnumSet.of(Days.WEDNESDAY);
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:30PM", formatter)
                , 2, enumSet2);
        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(2, new BitSet(), ts2, EnumSet.noneOf(Days.class)
                , enumSet2);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName",
                "", "3-1-0", 3, teacher1, timeslot2, room);


        Teacher teacher2 = new Faculty(2, "instructor2", new BitSet(), new BitSet(), new BitSet());
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName",
                "", "3-1-0", 3,  teacher2, timeslot1, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::lessonConflict)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("A room accommodates one lesson at a time")
    void roomMultiLessons() throws Exception{
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher1 = new Faculty(1, "instructor1", new BitSet(), new BitSet(), new BitSet());
        Teacher teacher2 = new Faculty(1, "instructor1", new BitSet(), new BitSet(), new BitSet());
        Teacher teacher3 = new Faculty(1, "instructor1", new BitSet(), new BitSet(), new BitSet());

        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 4, days);
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        EnumSet<Days> days2 = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                3, days2);

        Timeslot timeslot1 = Timeslot.test_lacLabBitAndDays(1, new BitSet(), bitSet1, EnumSet.noneOf(Days.class)
                , days);
        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(2, new BitSet(), bitSet2, EnumSet.noneOf(Days.class)
                , days);
        Timeslot timeslot3 = Timeslot.test_lacLabBitAndDays(3, new BitSet(), bitSet3, EnumSet.noneOf(Days.class)
                , days2);

        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName"
                , "", "3-1-0", 1, teacher1, timeslot1, room);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName"
                , "", "3-1-0", 1, teacher2, timeslot2, room);
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName"
                , "", "3-1-0", 1, teacher3, timeslot3, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Correct time slot hours for course type")
    void timeslotAndLessonTimeMatch() throws Exception{
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> days2 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> days3 = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher = new Faculty(1, "instructor1", new BitSet(), new BitSet(), new BitSet());

        /*9-10 MWF*/
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 2, days);
        /*10-11 MWF*/
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        /*10-11:30 MWF*/
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                3, days);
        /*8:30-10:00 MW*/
        BitSet bitSet4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter),
                3, days2);
        /*9-10 MTWR*/
        BitSet bitSet5 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                2, days3);
        /*10-11:30 MW*/
        BitSet bitSet6 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                3, days2);
        /*7-8 MW*/
        BitSet bitSet7 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, days2);
        /*8-9 MW*/
        BitSet bitSet8 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter),
                2, days2);

        /*right amount of hours*/
        Timeslot timeslot1 = Timeslot.test_lacLabBitAndDays(1, bitSet1, bitSet2, days, days);
        /*wrong lab/act hours*/
        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(2, bitSet1, bitSet3, days, days);
        /*wrong lec hours*/
        Timeslot timeslot3 = Timeslot.test_lacLabBitAndDays(3, bitSet3, bitSet2, days, days);
        /*right amount of hours*/
        /*1hr lec 4 days*/
        Timeslot timeslot4 = Timeslot.test_lacLabBitAndDays(4, bitSet5, new BitSet(), days3
                , EnumSet.noneOf(Days.class));
        /*right amount of hours*/
        /*1.5 hours lec & lab 2 days*/
        Timeslot timeslot5 = Timeslot.test_lacLabBitAndDays(5, bitSet4, bitSet6, days2, days2);
        /*right amount of hours of 1 activity unit and 2 lec units*/
        Timeslot timeslot6 = Timeslot.test_lacLabBitAndDays(5, bitSet7, bitSet8, days2, days2);





        /*For this test only the timeslot and course configuration matter*/
        /*3hrs lec & 3hrs lab per week*/
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName1", "noName"
                , "", "3-1-0", 1, teacher, timeslot1, room);
        /*3hrs lec & 3hrs lab per week*/
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "dummyName2", "noName"
                , "", "3-1-0", 1, teacher, timeslot2, room);
        /*3hrs lec & 3hrs lab per week*/
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "dummyName3", "noName"
                , "", "3-1-0", 1, teacher, timeslot3, room);
        /*4hrs lec per week*/
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "dummyName4", "noName"
                , "", "4-0-0", 1, teacher, timeslot4, room);
        /*3hrs lec & 3hrs lab per week*/
        Lesson lesson5 = Lesson.test_buildLesson("5", 1, "dummyName5", "noName"
                , "", "3-1-0", 1, teacher, timeslot5, room);
        /*lecture with activity 2hrs lec and 2 act hrs per week*/
        Lesson lesson6 = Lesson.test_buildLesson("6", 1, "dummyName6", "noName"
                , "", "2-0-1", 1, teacher, timeslot6, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(lesson1, lesson2, lesson3, lesson4, lesson5, lesson6)
                .penalizesBy(2);
    }


    @Test
    @DisplayName("CSC Lesson should be in the right Room")
    void lessonRoomCheck(){
        int room301ID = Constants.ROOM_TO_ID_BIMAP.get("301");
        int lecOnlyID = Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY);
        /*Tests assumes that we are scheduling the CSC courses*/
        Room room1 = new Room(Integer.toString(room301ID), "301", 3);
        Room room2 = new Room("9999", "badRoom", 9999);
        Room room3 = new Room(Integer.toString(lecOnlyID), Constants.LEC_ONLY, lecOnlyID);
        Teacher teacher = new Teacher(1, "noName", new BitSet(), new BitSet(), new BitSet());
        Timeslot timeslot1 = Timeslot.test_minSetUp("1");
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "csc101", "noName"
                , "", "0-1-1", 1, teacher, timeslot1, room1);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "csc101", "noName"
                , "", "0-1-1", 1, teacher, timeslot1, room2);
        Lesson lesson3 = Lesson.test_buildLesson("1", 1, "csc101", "noName"
                , "", "0-1-1", 1, teacher, timeslot1, room3);
        Lesson lesson4 = Lesson.test_buildLesson("1", 1, "csc445", "noName"
                , "", "0-0-0", 1, teacher, timeslot1, room3);
        Lesson lesson5 = Lesson.test_buildLesson("1", 1, "csc445", "noName"
                , "", "0-0-0", 1, teacher, timeslot1, room1);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2, lesson3, lesson4, lesson5)
                .penalizesBy(3);
    }

    @Test
    @DisplayName("PrimeTime reward")
    void primeTimeReward() throws Exception{
        Room room = new Room("1", "UNKNOWN", 1);
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        Teacher teacher = new Teacher(1, "noName", new BitSet(), new BitSet(), new BitSet());
        /*8-9 MWF; Lecture time completely in prime time*/
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 2, days);
        /*10-11 MWF*/
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        /*8-9:30 MWF; Lecture time partially outside of prime time*/
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 3, days);
        Timeslot timeslot = Timeslot.test_lacLabBitAndDays(1, bitSet1, bitSet2, days, days);
        Lesson lesson = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "0-0-0", 1, teacher, timeslot, room);

        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(1, bitSet3, bitSet2, days, days);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "0-0-0", 1, teacher, timeslot2, room);
        constraintVerifier.verifyThat(TimetableConstraintProvider::outPrimeTime)
                .given(lesson, lesson2)
                /*Note this takes into account weight of rewards*/
                .rewardsWith(12);
    }

    @Test
    @DisplayName("PrimeTime penalty")
    void primeTimePenalty() throws Exception{
        Room room = new Room("1", "UNKNOWN", 1);
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> days2 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        Teacher teacher = new Teacher(1, "noName", new BitSet(), new BitSet(), new BitSet());
        /*7-9:30 MW; Lecture time completely in prime time*/
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter)
                , 5, days);
        /*10-11 MW*/
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        /*8:30-10:00 MWF; Lecture time partially outside of prime time*/
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter)
                , 3, days2);
        Timeslot timeslot = Timeslot.test_lacLabBitAndDays(1, bitSet1, bitSet2, days, days);
        Lesson lesson = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "0-0-0", 1, teacher, timeslot, room);

        Timeslot timeslot2 = Timeslot.test_lacLabBitAndDays(1, bitSet3, bitSet2, days2, days);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "0-0-0", 1, teacher, timeslot2, room);
        constraintVerifier.verifyThat(TimetableConstraintProvider::inPrimeTime)
                .given(lesson, lesson2)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(  2 + 6);
    }
}
