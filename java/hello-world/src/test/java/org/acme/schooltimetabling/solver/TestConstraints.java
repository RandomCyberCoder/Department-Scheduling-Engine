package org.acme.schooltimetabling.solver;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Timetable;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.LessonGenerator;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.junit.jupiter.api.BeforeAll;
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
    final static int NO_PENALTY = 0;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");

    @BeforeAll
    static void setUp(){
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        LessonGenerator.OLD_studio_detected = true;
        ConstraintTestHelper.load();
    }


    @Test
    @DisplayName("Testing same teacher same course constraint")
    void sameLessonNTeacher(){
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> MT = EnumSet.of(Days.MONDAY, Days.TUESDAY);
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        EnumSet<Days> soloT = EnumSet.of(Days.TUESDAY);

        //penalize ls1-ls2 and ls1-ls3 combos. Penalty +2
        Timeslot timeslot1 = Timeslot.test_CreateWithDaysOnly(1, MW, MW);
        Lesson ls1 = Lesson.test_buildLesson("1", 1, "csc201", "dummyInstructor", "",
                "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot1, ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_CreateWithDaysOnly(2, MW, MT);
        Lesson ls2 = Lesson.test_buildLesson("2", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2, ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot3 = Timeslot.test_CreateWithDaysOnly(2, MWF, MWF);
        Lesson ls3 = Lesson.test_buildLesson("3", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot3, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::sameClassSameDays)
                .given(ls1, ls2, ls3)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(2);

    }


    @Test
    @DisplayName("teacher conflict times & timeslot conflict")
    void teacherAndTimeslot(){
        //No penalty for teacher1
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet teacher1Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 4, MW);
        Teacher teacher1 = new Teacher(1, "instructor1", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, teacher1Bits);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter)
                , 4, MW);
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, ts1, ConstraintTestHelper.EMPTY_BS,
                MW, ConstraintTestHelper.NO_DAYS);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, timeslot1, ConstraintTestHelper.DUMMY_ROOM);

        //No Penalty 1+ for teacher2
        BitSet teacher2Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 4, MW);
        Teacher teacher2 = new Teacher(1, "instructor1", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, teacher2Bits);
        BitSet bs_MW_7AM_blck2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter), 2, MW);
        BitSet bs_MW_8AM_blcks2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MW);
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bs_MW_7AM_blck2, bs_MW_8AM_blcks2, MW, MW);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher2, timeslot2, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherLessonConflict)
                .given(lesson1, lesson2)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Faculty override conflict & timeslot conflict")
    void facultyAndTimeslot(){
        //NOTE: the faculty class sets bits from 9pm-10pm MWF a faculty time only during testing
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);

        Teacher teacher = new Faculty(1, "instructor1", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);
        BitSet bs_9pm_2blcks_MW = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00PM", formatter)
                , 2, MW);
        BitSet bs_8pm_2blcks_MW = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00PM", formatter)
                , 2, MW);
        Timeslot ts_9pm_2bl = Timeslot.test_lecLabBitAndDays(1, bs_9pm_2blcks_MW, ConstraintTestHelper.EMPTY_BS, MW,
                ConstraintTestHelper.NO_DAYS);
        Timeslot ts_8pm_2bl_9pm_2bl = Timeslot.test_lecLabBitAndDays(2, bs_8pm_2blcks_MW, bs_9pm_2blcks_MW, MW, MW);
        Timeslot ts_8pm_2bl = Timeslot.test_lecLabBitAndDays(1, bs_8pm_2blcks_MW, ConstraintTestHelper.EMPTY_BS, MW,
                ConstraintTestHelper.NO_DAYS);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "3-1-0", 1, teacher, ts_9pm_2bl, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher, ts_8pm_2bl_9pm_2bl, ConstraintTestHelper.DUMMY_ROOM);

        //no penalty
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "dummyName", "noName",
                "", "3-0-0", 2,  teacher, ts_8pm_2bl, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherLessonConflict)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(2);
    }


    @Test
    @DisplayName("Test: teacher can't teach two lessons at the same time")
    void teacherLessonSameTime(){
        Teacher teacher1 = new Faculty(1, "instructor1", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);

        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter)
                , 6, enumSet);
        //1PM-4 MW
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, ts1, ConstraintTestHelper.EMPTY_BS, enumSet, enumSet);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, timeslot1, ConstraintTestHelper.DUMMY_ROOM);


        EnumSet<Days> enumSet2 = EnumSet.of(Days.WEDNESDAY);
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:30PM", formatter)
                , 2, enumSet2);
        //3:30PM-4:30PM W
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, ConstraintTestHelper.EMPTY_BS, ts2,
                ConstraintTestHelper.NO_DAYS, enumSet2);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName",
                "", "3-1-0", 3, teacher1, timeslot2, ConstraintTestHelper.DUMMY_ROOM);


        EnumSet<Days> soloDay = EnumSet.of(Days.MONDAY);
        BitSet bs3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("2:00PM", formatter), 2, soloDay);
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_1PM_MWF = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter), 2, soloDay);
        BitSet bs_2PM_MWF = BitSetHelper.timeSlotBitSet(LocalTime.parse("2:00PM", formatter), 2, soloDay);
        Timeslot ts_1PM_MWF_blks4 = Timeslot.test_lecLabBitAndDays(1, bs_1PM_MWF, bs_2PM_MWF, MWF
                , MWF);

        Timeslot ts3 = Timeslot.test_lecLabBitAndDays(1, bs3, ConstraintTestHelper.EMPTY_BS, soloDay, ConstraintTestHelper.NO_DAYS);
        Teacher teacher2 = new Faculty(2, "instructor2", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName",
                "", "3-1-0", 3,  teacher2, ts_1PM_MWF_blks4, ConstraintTestHelper.DUMMY_ROOM);

        //penalizes lesson1 and lesson2 grouping
        constraintVerifier.verifyThat(TimetableConstraintProvider::lessonConflict)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("A room accommodates only one lesson at a time")
    void roomMultiLessons(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> M = EnumSet.of(Days.MONDAY);
        EnumSet<Days> TR = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        Room room = new Room("1", "dummyRoom", 1);

        //9-11 MWF
        BitSet bs_MWF_9AM_4blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 4, MWF);
        //10-11 MWF
        BitSet bs_MWF_10AM_to_11 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, MWF);
        //9-11:30 TR
        BitSet bs_TR_9AM_1130 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                3, TR);
        //9AM-12PM M
        BitSet ts_bs1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM",formatter), 6, M);
        //4pm-7pm M
        BitSet ts_bs2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("4:00PM", formatter), 6, M);

        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, ConstraintTestHelper.EMPTY_BS, bs_MWF_9AM_4blcks,
                ConstraintTestHelper.NO_DAYS, MWF);
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, ConstraintTestHelper.EMPTY_BS, bs_MWF_10AM_to_11,
                ConstraintTestHelper.NO_DAYS, MWF);
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(3, ConstraintTestHelper.EMPTY_BS, bs_TR_9AM_1130,
                ConstraintTestHelper.NO_DAYS, TR);
        Timeslot st_ts_conflict = Timeslot.test_lecLabBitAndDays(4, ts_bs1, ConstraintTestHelper.EMPTY_BS,
                M, ConstraintTestHelper.NO_DAYS);
        Timeslot st_ts_no_conflict = Timeslot.test_lecLabBitAndDays(6, ts_bs2, ConstraintTestHelper.EMPTY_BS,
                M, ConstraintTestHelper.NO_DAYS);

        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot1, room);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2, room);
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot3, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Correct time slot hours for course type")
    void timeslotAndLessonTimeMatch(){
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> days2 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> days3 = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher = new Faculty(1, "instructor1", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);

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
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet2, days, days);
        /*wrong lab/act hours*/
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, bitSet1, bitSet3, days, days);
        /*wrong lec hours*/
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(3, bitSet3, bitSet2, days, days);
        /*right amount of hours*/
        /*1hr lec 4 days*/
        Timeslot timeslot4 = Timeslot.test_lecLabBitAndDays(4, bitSet5, ConstraintTestHelper.EMPTY_BS,
                days3, ConstraintTestHelper.NO_DAYS);
        /*right amount of hours*/
        /*1.5 hours lec & lab 2 days*/
        Timeslot timeslot5 = Timeslot.test_lecLabBitAndDays(5, bitSet4, bitSet6, days2, days2);
        /*right amount of hours of 1 activity unit and 2 lec units*/
        Timeslot timeslot6 = Timeslot.test_lecLabBitAndDays(5, bitSet7, bitSet8, days2, days2);


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
    @DisplayName("Penalty: Lesson should be in the right Room")
    void penLessonRoomCheck(){
        //Lesson requires a specific room
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.NON_STUDIO_SPECIFIC, "",
                "", "2-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.NON_STUDIO_SPECIFIC),
                ConstraintTestHelper.DUMMY_TEACHER, ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_RANDO_LAB);

        //lecture only room needs to be in lecture room
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "some only lec course", "",
                "", "1-0-0", 1111, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_RANDO_LAB);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2
                )
                .penalizesBy(2);
    }



    @Test
    @DisplayName("No Penalty: Lesson should be in the right Room")
    void noPenLessonRoomCheck(){
        //recreating the lecture only room
        final int LEC_ONLY_ID = Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY);
        final Room LEC_ONLY_ROOM = new Room(Integer.toString(LEC_ONLY_ID), "LEC_ONLY", LEC_ONLY_ID);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.NON_STUDIO_SPECIFIC, "",
                "", "2-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.NON_STUDIO_SPECIFIC),
                ConstraintTestHelper.DUMMY_TEACHER, ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_SPECIFIC);

        //lecture only room needs to be in lecture room
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "some only lec course", "",
                "", "1-0-0", 1111, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, LEC_ONLY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2
                )
                .penalizesBy(NO_PENALTY);
    }



//NOTE LEFT out on purpose. While true Studio classes fix are made; These are quasi studio types???
//    @Test
//    @DisplayName("Studio Space test")
//    void studioSpace() throws Exception{
//        //simulate studio split using lesson generator
//        //make a studio split. lecture only, then the lessons with a combo of lec and lab/act
//
//        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
//        EnumSet<Days> F = EnumSet.of(Days.FRIDAY);
//        EnumSet<Days> MTWR = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
//
//        /*7-9:30 MW*/
//        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter)
//                , 5, MW);
//        /*10-11 MW*/
//        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
//                2, MW);
//        /*8:30-10:00 F*/
//        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter)
//                , 3, F);
//        /*9-10 MTWR*/
//        BitSet bitSet4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
//                , 1, MTWR);
//
//        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet2, MW, MW);
//        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet3, MW, F);
//        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(1, bitSet4, EMPTY_BS, MTWR, NO_DAYS);
//        Timeslot oneDay = Timeslot.test_lecLabBitAndDays(1, bitSet3, EMPTY_BS, F, NO_DAYS);
//
//        //no penalty for lessons 1-3
//        Lesson lesson = Lesson.test_buildLesson("1", 1, "nonStudio", "noName", "",
//                "3-0-1", 1, DUMMY_TEACHER, timeslot1, DUMMY_ROOM);
//        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "nonStudio", "noName", "",
//                "2-1-0", 1, DUMMY_TEACHER, timeslot2, DUMMY_ROOM);
//        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "nonStudio", "noName", "",
//                "4-0-0", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM);
//        //mimic studio style split
//        //no penalty
//        Lesson studioLL1 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "doesn't matter", "",
//                "1-0-0", 1, DUMMY_TEACHER, timeslot1, DUMMY_ROOM, DUMMY_LINKER);
//        //non consec time; penalty +1
//        Lesson studioLLA1 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "doesn't matter", "",
//                "0-0-1", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM, DUMMY_LINKER);
//        //consec time; no penalty
//        Lesson studioLLA1_2 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "doesn't matter", "",
//                "0-0-1", 1, DUMMY_TEACHER, oneDay, DUMMY_ROOM, DUMMY_LINKER);
//
//        constraintVerifier.verifyThat(TimetableConstraintProvider::studioSpace)
//                .given(lesson, lesson2, lesson3, studioLL1, studioLLA1, studioLLA1_2)
//                /*Note this takes into account weight of rewards*/
//                .penalizesBy(1);
//    }


//NOTE LEFT out on purpose. While true Studio classes fix are made; These are quasi studio types???
//    @Test
//    @DisplayName("Studio: lesson and lab order")
//    void check_studioLabAfterLesson() throws Exception{
//        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
//        EnumSet<Days> M = EnumSet.of(Days.MONDAY);
//        EnumSet<Days> T = EnumSet.of(Days.TUESDAY);
//
//        BitSet bs_MWF_1PM_blcks2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter), 2, MWF);
//        BitSet bs_M_9AM_blcks4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
//                4, M);
//        BitSet bs_T_9AM_blcks4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
//                4, T);
//
//        Timeslot ts_MWF_1PM = Timeslot.test_lecLabBitAndDays(1, bs_MWF_1PM_blcks2, EMPTY_BS, MWF, NO_DAYS);
//        Timeslot ts_M_9AM = Timeslot.test_lecLabBitAndDays(2, bs_M_9AM_blcks4, EMPTY_BS, M, NO_DAYS);
//        Timeslot ts_T_9AM = Timeslot.test_lecLabBitAndDays(3, bs_T_9AM_blcks4, EMPTY_BS, T, NO_DAYS);
//
//        //penalize pair, +1
//        Lesson st_lec1_MWF_1PM = Lesson.test_buildLesson("1", 1, DUMMY_STUDIO, "", "",
//                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_MWF_1PM, DUMMY_ROOM,
//                1);
//        Lesson st_lab_M_9AM = Lesson.test_buildLesson("2", 1, DUMMY_STUDIO, "", "",
//                "0-0-1", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_M_9AM, DUMMY_ROOM,
//                1);
//
//        //non penalty pair
//        Lesson st_lec2_MWF_1PM = Lesson.test_buildLesson("3", 1, DUMMY_STUDIO, "", "",
//                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_MWF_1PM, DUMMY_ROOM,
//                2);
//        Lesson st_lab_T_9AM = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "", "",
//                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_T_9AM, DUMMY_ROOM,
//                2);
//
//        constraintVerifier.verifyThat(TimetableConstraintProvider::studioLabAfterLesson)
//                .given(st_lec1_MWF_1PM, st_lab_M_9AM,
//                        st_lec2_MWF_1PM, st_lab_T_9AM)
//                .penalizesBy(1);
//    }


    @Test
    @DisplayName("PrimeTime reward")
    void primeTimeReward(){
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);

        /*8-9 MWF; Lecture time completely in prime time*/
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 3, days);
        /*10-11 MWF*/
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        /*8-9:30 MWF; Lecture time partially outside of prime time*/
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 3, days);
        Timeslot timeslot = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet2, days, days);
        Lesson lesson = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot, ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bitSet3, bitSet2, days, days);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::outPrimeTime)
                .given(lesson, lesson2)
                /*Note this takes into account weight of rewards*/
                .rewardsWith(12 + 0);
    }


    @Test
    @DisplayName("PrimeTime penalty")
    void primeTimePenalty(){EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> days2 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        /*7-9:30 MW; Lecture time completely in prime time*/
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter)
                , 5, days);
        /*10-11 MW*/
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        /*8:30-10:00 MWF; Lecture time partially outside of prime time*/
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter)
                , 3, days2);
        Timeslot timeslot = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet2, days, days);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot, ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bitSet3, bitSet2, days2, days);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::inPrimeTime)
                .given(lesson1, lesson2)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(2 + 6);
    }

    @Test
    @DisplayName("no penalty Primetime hard constraint")
    void hardPrimeTimeNoPenalty(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_8am_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet bs_9am_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter), 2, MWF);
        BitSet bs_2pm_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("2:00PM", formatter), 2, MWF);

        Timeslot ts_8am_2blcks_9am_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_8am_2blcks, bs_9am_2blcks, MWF, MWF);
        Timeslot ts_2pm_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_2pm_2blcks, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Lesson lessonOutPrimeTime = Lesson.test_buildLesson("1", 1, "", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_8am_2blcks_9am_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lessonInPrimeTime = Lesson.test_buildLesson("2", 1, "", "",
                "", "3-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_2pm_2blcks, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::primeTime50Plus)
                .given(lessonOutPrimeTime, lessonInPrimeTime)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("penalty Primetime hard constraint")
    void hardPrimeTimePenalty(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_8am_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet bs_9am_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter), 2, MWF);
        BitSet bs_130pm_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:30PM", formatter), 3, MWF);

        Timeslot ts_8am_2blcks_9am_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_8am_2blcks, bs_9am_2blcks, MWF, MWF);
        Timeslot ts_130pm_3blcks = Timeslot.test_lecLabBitAndDays(2, bs_130pm_3blcks, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Lesson lessonOutPrimeTime = Lesson.test_buildLesson("1", 1, "", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_8am_2blcks_9am_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lessonInPrimeTime = Lesson.test_buildLesson("2", 1, "", "",
                "", "3-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_130pm_3blcks, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::primeTime50Plus)
                .given(lessonOutPrimeTime, lessonInPrimeTime)
                .penalizesBy(1);
    }
}


