package org.acme.schooltimetabling.solver;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
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
        //9-11 MWF
        BitSet bs_MWF_9AM_4blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 4, MWF);
        //9-10 MWF
        BitSet bs_MWF_9AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 2, MWF);
        //10-11 MWF
        BitSet bs_MWF_10_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, MWF);
        //9-11:30 TR
        BitSet bs_MWF_11_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("11:00AM", formatter),
                2, MWF);

        Timeslot ts_lab_mwf_9_4blcks = Timeslot.test_lecLabBitAndDays(1, ConstraintTestHelper.EMPTY_BS, bs_MWF_9AM_4blcks,
                ConstraintTestHelper.NO_DAYS, MWF);
        Timeslot ts_lab_mwf_10_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_MWF_9AM_2blcks, bs_MWF_10_2blcks,
                ConstraintTestHelper.NO_DAYS, MWF);
        //no conflict with either
        Timeslot ts_lec_mwf_10_lab_mwf_11 = Timeslot.test_lecLabBitAndDays(3, bs_MWF_10_2blcks, bs_MWF_11_2blcks,
                MWF, MWF);


        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName"
                , "", "0-0-2", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lab_mwf_9_4blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lab_mwf_10_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lec_mwf_10_lab_mwf_11,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Correct time slot hours for course type")
    void timeslotAndLessonTimeMatch(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> MTWR = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher = new Faculty(1, "instructor1", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);

        /*9-10 MWF*/
        BitSet bs_mwf_9_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 2, MWF);
        /*10-11 MWF*/
        BitSet bs_mwf_10_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, MWF);
        /*10-11:30 MWF*/
        BitSet bs_mwf_10_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                3, MWF);
        /*8:30-10:00 MW*/
        BitSet bs_mw_830_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter),
                3, MW);
        /*9-10 MTWR*/
        BitSet bs_mtwr_9_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                2, MTWR);
        /*10-11:30 MW*/
        BitSet bs_mw_10_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                3, MW);
        /*7-8 MW*/
        BitSet bs_mw_7_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, MW);
        /*8-9 MW*/
        BitSet bs_mw_8_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter),
                2, MW);

        /*right amount of hours*/
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, bs_mwf_9_2blcks, bs_mwf_10_2blcks, MWF, MWF);
        /*wrong lab/act hours*/
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, bs_mwf_9_2blcks, bs_mwf_10_3blcks, MWF, MWF);
        /*wrong lec hours*/
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(3, bs_mwf_10_3blcks, bs_mwf_10_2blcks, MWF, MWF);
        /*right amount of hours*/
        /*1hr lec 4 days*/
        Timeslot timeslot4 = Timeslot.test_lecLabBitAndDays(4, bs_mtwr_9_2blcks, ConstraintTestHelper.EMPTY_BS,
                MTWR, ConstraintTestHelper.NO_DAYS);
        /*right amount of hours*/
        /*1.5 hours lec & lab 2 days*/
        Timeslot timeslot5 = Timeslot.test_lecLabBitAndDays(5, bs_mw_830_3blcks, bs_mw_10_3blcks, MW, MW);
        /*right amount of hours of 1 activity unit and 2 lec units*/
        Timeslot timeslot6 = Timeslot.test_lecLabBitAndDays(5, bs_mw_7_2blcks, bs_mw_8_2blcks, MW, MW);
        Timeslot ts_lab_mw_830_3blcks = Timeslot.test_lecLabBitAndDays(7, ConstraintTestHelper.EMPTY_BS, bs_mw_830_3blcks,
                ConstraintTestHelper.NO_DAYS, MW);


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
                , "", "0-1-0", 1, teacher, timeslot6, room);
        Lesson lsLabGood = Lesson.test_buildLesson("7", 1, "", ""
                , "", "0-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lab_mw_830_3blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(lesson1, lesson2, lesson3, lesson4, lesson5, lesson6, lsLabGood)
                .penalizesBy(3);
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

        Lesson lsLabOnly = Lesson.test_buildLesson("2", 1, Constants.LEC_ONLY,
                "noName", "", "0-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2, lsLabOnly)
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
        Lesson lsLabOnly = Lesson.test_buildLesson("2", 1, ConstraintTestHelper.TEST_NAME_RANDOM_LAB_ROOM,
                "noName", "", "0-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2, lsLabOnly)
                .penalizesBy(NO_PENALTY);
    }



    @Test
    @DisplayName("PrimeTime reward")
    void primeTimeReward(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);

        /*8-9 MWF; Lecture time completely in prime time*/
        BitSet bs_MWF_8_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 2, MWF);
        /*10-11 MWF*/
        BitSet bs_MWF_10_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, MWF);
        /*8-9:30 MWF; Lecture time partially outside of prime time*/
        BitSet bs_MWF_8_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 3, MWF);
        BitSet bs_MW_8_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 2, MW);
        Timeslot timeslot = Timeslot.test_lecLabBitAndDays(1, bs_MWF_8_2blcks, bs_MWF_10_2blcks, MWF, MWF);
        Lesson lesson = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot, ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bs_MWF_8_3blcks, bs_MWF_10_2blcks, MWF, MWF);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2, ConstraintTestHelper.DUMMY_ROOM);
        Timeslot ts3 = Timeslot.test_lecLabBitAndDays(3, ConstraintTestHelper.EMPTY_BS, bs_MW_8_2blcks,
                ConstraintTestHelper.NO_DAYS, MW);
        Lesson lsLabOnly = Lesson.test_buildLesson("3", 1, "someCourse", "noName", "",
                "0-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER, ts3, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::outPrimeTime)
                .given(lesson, lesson2, lsLabOnly)
                /*Note this takes into account weight of rewards*/
                .rewardsWith(12 + 0);
    }


    @Test
    @DisplayName("PrimeTime penalty")
    void primeTimePenalty(){
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        /*7-9:30 MW; Lecture time completely in prime time*/
        BitSet bs_MW_7_5blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter)
                , 5, MW);
        /*10-11 MW*/
        BitSet bs_MW_10_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, MW);
        /*8:30-10:00 MWF; Lecture time partially outside of prime time*/
        BitSet bs_MWF_830_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter)
                , 3, MWF);
        Timeslot timeslot = Timeslot.test_lecLabBitAndDays(1, bs_MW_7_5blcks, bs_MW_10_2blcks, MW, MW);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot, ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bs_MWF_830_3blcks, bs_MW_10_2blcks, MWF, MW);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "someCourse", "noName", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2, ConstraintTestHelper.DUMMY_ROOM);
        Timeslot ts3 = Timeslot.test_lecLabBitAndDays(3, ConstraintTestHelper.EMPTY_BS, bs_MW_10_2blcks,
                ConstraintTestHelper.NO_DAYS, MW);
        Lesson lsLabOnly = Lesson.test_buildLesson("3", 1, "someCourse", "noName", "",
                "0-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts3, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::inPrimeTime)
                .given(lesson1, lesson2, lsLabOnly)
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
        Timeslot ts_lab_2pm_2blcks = Timeslot.test_lecLabBitAndDays(3, ConstraintTestHelper.EMPTY_BS, bs_2pm_2blcks,
                ConstraintTestHelper.NO_DAYS, MWF);

        Lesson lessonOutPrimeTime = Lesson.test_buildLesson("1", 1, "", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_8am_2blcks_9am_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lessonInPrimeTime = Lesson.test_buildLesson("2", 1, "", "",
                "", "3-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_2pm_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lsLabOnly = Lesson.test_buildLesson("3", 1, "someCourse", "noName", "",
                "0-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lab_2pm_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::primeTime50Plus)
                .given(lessonOutPrimeTime, lessonInPrimeTime, lsLabOnly)
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
        Timeslot ts_lab_MWF_8am_2blcks = Timeslot.test_lecLabBitAndDays(1, ConstraintTestHelper.EMPTY_BS,
                bs_9am_2blcks, ConstraintTestHelper.NO_DAYS, MWF);

        Lesson lessonOutPrimeTime = Lesson.test_buildLesson("1", 1, "", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_8am_2blcks_9am_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lessonInPrimeTime = Lesson.test_buildLesson("2", 1, "", "",
                "", "3-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ts_130pm_3blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lsLabOnly = Lesson.test_buildLesson("3", 1, "someCourse", "noName", "",
                "0-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lab_MWF_8am_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::primeTime50Plus)
                .given(lessonOutPrimeTime, lessonInPrimeTime, lsLabOnly)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("no Pen: teacher with small gaps")
    void teacherNoLargeGaps(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        EnumSet<Days> TR = EnumSet.of(Days.TUESDAY, Days.THURSDAY);

        BitSet bs_mwf_8am_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet bs_mwf_9am_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter), 2, MWF);
        BitSet bs_mwf_1PM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter), 2, MWF);
        BitSet bs_tr_830pm_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30PM", formatter), 3, TR);
        BitSet bs_mwf_3pm_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:00PM", formatter), 2, MWF);

        Timeslot ts_mwf_8am_2blcks_9am_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_mwf_8am_2blcks, bs_mwf_9am_2blcks, MWF, MWF);
        Timeslot ts_mwf_1pm_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_mwf_1PM_2blcks, ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.NO_DAYS, ConstraintTestHelper.NO_DAYS);
        Timeslot ts_tr_830pm_3blcks = Timeslot.test_lecLabBitAndDays(3, ConstraintTestHelper.EMPTY_BS, bs_tr_830pm_3blcks,
                ConstraintTestHelper.NO_DAYS, ConstraintTestHelper.NO_DAYS);
        Timeslot ts_mwf_3pm_2blcks =
                Timeslot.test_lecLabBitAndDays(4, bs_mwf_3pm_2blcks, ConstraintTestHelper.EMPTY_BS,
                        MWF, ConstraintTestHelper.NO_DAYS);

        Lesson ls1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "0-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER , ts_mwf_8am_2blcks_9am_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson ls2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "0-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER , ts_mwf_1pm_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson ls3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER , ts_tr_830pm_3blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson ls4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "0-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER , ts_mwf_3pm_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::compressTeachTime)
                .given(ls1, ls2, ls3, ls4)
                .penalizesBy(0);
    }


    @Test
    @DisplayName("Penalties: large gaps and long days")
    void penalizeLargeGapsAndLongDays(){

        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        Teacher TEACHER1 = new Teacher( 1, "dummyInstructor", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);
        Teacher TEACHER2 = new Teacher( 1, "dummyInstructor", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);


    /* ---------------------------
       Teacher 1: gap > 3 hours
       --------------------------- */

        BitSet t1_8am = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet t1_1230pm = BitSetHelper.timeSlotBitSet(LocalTime.parse("12:30PM", formatter), 2, MWF); // 3.5 hr gap

        Timeslot ts_t1_a = Timeslot.test_lecLabBitAndDays(1, t1_8am, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Timeslot ts_t1_b = Timeslot.test_lecLabBitAndDays(1, t1_1230pm, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Lesson t1_l1 = Lesson.test_buildLesson("t1_l1", 1,"","","","0-0-0",1,
                TEACHER1, ts_t1_a, ConstraintTestHelper.DUMMY_ROOM);

        Lesson t1_l2 = Lesson.test_buildLesson("t1_l2", 1,"","","","0-0-0",1,
                TEACHER1, ts_t1_b, ConstraintTestHelper.DUMMY_ROOM);


/* ---------------------------
   Teacher 2: day > 8 hours (8.5h) with NO >3hr gaps
   --------------------------- */

        BitSet t2_8am = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet t2_12pm = BitSetHelper.timeSlotBitSet(LocalTime.parse("12:00PM", formatter), 2, MWF);
        BitSet t2_430pm = BitSetHelper.timeSlotBitSet(LocalTime.parse("4:30PM", formatter), 1, MWF);

        Timeslot ts_t2_a = Timeslot.test_lecLabBitAndDays(1, t2_8am, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Timeslot ts_t2_b = Timeslot.test_lecLabBitAndDays(1, t2_12pm, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Timeslot ts_t2_c = Timeslot.test_lecLabBitAndDays(1, t2_430pm, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Lesson t2_l1 = Lesson.test_buildLesson("t2_l1",1,"","","","0-0-0",1,
                TEACHER2, ts_t2_a, ConstraintTestHelper.DUMMY_ROOM);

        Lesson t2_l2 = Lesson.test_buildLesson("t2_l2",1,"","","","0-0-0",1,
                TEACHER2, ts_t2_b, ConstraintTestHelper.DUMMY_ROOM);

        Lesson t2_l3 = Lesson.test_buildLesson("t2_l3",1,"","","","0-0-0",1,
                TEACHER2, ts_t2_c, ConstraintTestHelper.DUMMY_ROOM);



    /* ---------------------------
       Teacher 5: multiple large gaps
       still only one penalty
       --------------------------- */

        BitSet t5_8am = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet t5_1230pm = BitSetHelper.timeSlotBitSet(LocalTime.parse("12:30PM", formatter), 2, MWF);
        BitSet t5_6pm = BitSetHelper.timeSlotBitSet(LocalTime.parse("6:00PM", formatter), 2, MWF);

        Timeslot ts_t5_a = Timeslot.test_lecLabBitAndDays(1, t5_8am, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Timeslot ts_t5_b = Timeslot.test_lecLabBitAndDays(1, t5_1230pm, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Timeslot ts_t5_c = Timeslot.test_lecLabBitAndDays(1, t5_6pm, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);

        Lesson t5_l1 = Lesson.test_buildLesson("t5_l1",1,"","","","0-0-0",1,
                ConstraintTestHelper.DUMMY_TEACHER, ts_t5_a, ConstraintTestHelper.DUMMY_ROOM);

        Lesson t5_l2 = Lesson.test_buildLesson("t5_l2",1,"","","","0-0-0",1,
                ConstraintTestHelper.DUMMY_TEACHER, ts_t5_b, ConstraintTestHelper.DUMMY_ROOM);

        Lesson t5_l3 = Lesson.test_buildLesson("t5_l3",1,"","","","0-0-0",1,
                ConstraintTestHelper.DUMMY_TEACHER, ts_t5_c, ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::compressTeachTime)
                .given(
                        t1_l1, t1_l2,
                        t2_l1, t2_l2, t2_l3,
                        t5_l1, t5_l2, t5_l3
                )
                .penalizesBy(3);
    }


    @Test
    @DisplayName("Reward: teacher prefers one-hour gaps")
    void rewardPreferredHourGap() {
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        Teacher teacher = new Teacher(1, "prefers gaps", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, Preference.AGREE);

        BitSet early = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        BitSet late = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter), 2, MWF);

        Timeslot tsEarly = Timeslot.test_lecLabBitAndDays(1, early, ConstraintTestHelper.EMPTY_BS, MWF, ConstraintTestHelper.NO_DAYS);
        Timeslot tsLate = Timeslot.test_lecLabBitAndDays(2, ConstraintTestHelper.EMPTY_BS, late, ConstraintTestHelper.NO_DAYS, MWF);

        Lesson lesson1 = Lesson.test_buildLesson("gap1", 1, "", "", "",
                "0-0-0", 1, teacher, tsEarly, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson2 = Lesson.test_buildLesson("gap2", 1, "", "", "",
                "0-0-0", 1, teacher, tsLate, ConstraintTestHelper.DUMMY_ROOM);

        //------ no reward (hour gap between lec and lab)
        EnumSet<Days> RF = EnumSet.of(Days.THURSDAY, Days.FRIDAY);
        BitSet rfLecture = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:00PM", formatter), 2, RF);
        BitSet rfLab = BitSetHelper.timeSlotBitSet(LocalTime.parse("5:00PM", formatter), 2, RF);
        Timeslot rfTimeslot = Timeslot.test_lecLabBitAndDays(6, rfLecture, rfLab, RF, RF);
        Teacher singleCourseTeacher = new Teacher(4, "single slot", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, Preference.AGREE);
        Lesson singleCourse = Lesson.test_buildLesson("gap3", 1, "", "", "",
                "0-0-0", 1, singleCourseTeacher, rfTimeslot, ConstraintTestHelper.DUMMY_ROOM);

        //------ no reward
        Teacher disagreeTeacher = new Teacher(2, "hates gaps", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, Preference.DISAGREE);

        Lesson disagreeLesson1 = Lesson.test_buildLesson("gapDisagree1", 1, "", "", "",
                "0-0-0", 1, disagreeTeacher, tsEarly, ConstraintTestHelper.DUMMY_ROOM);
        Lesson disagreeLesson2 = Lesson.test_buildLesson("gapDisagree2", 1, "", "", "",
                "0-0-0", 1, disagreeTeacher, tsLate, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::rewardPreferredHourGap)
                .given(lesson1, lesson2,
                        singleCourse,
                        disagreeLesson1, disagreeLesson2)
                .rewardsWith(1);
    }

    @Test
    @DisplayName("Penalty: teacher dislikes one-hour gaps")
    void penalizeDislikedHourGap() {
        EnumSet<Days> TR = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        EnumSet<Days> T = EnumSet.of(Days.TUESDAY);
        Teacher teacher = new Teacher(2, "hates gaps", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, Preference.DISAGREE);

        BitSet blockA = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter), 2, TR);
        BitSet blockB = BitSetHelper.timeSlotBitSet(LocalTime.parse("11:00AM", formatter), 2, TR);
        BitSet blockC = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter), 2, T);

        Timeslot tsA = Timeslot.test_lecLabBitAndDays(3, blockA, ConstraintTestHelper.EMPTY_BS, TR, ConstraintTestHelper.NO_DAYS);
        Timeslot tsB = Timeslot.test_lecLabBitAndDays(4, blockB, ConstraintTestHelper.EMPTY_BS, TR, ConstraintTestHelper.NO_DAYS);
        Timeslot tsC = Timeslot.test_lecLabBitAndDays(5, ConstraintTestHelper.EMPTY_BS, blockC, ConstraintTestHelper.NO_DAYS, T);

        Lesson lessonA = Lesson.test_buildLesson("penalty1", 1, "", "", "",
                "0-0-0", 1, teacher, tsA, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lessonB = Lesson.test_buildLesson("penalty2", 1, "", "", "",
                "0-0-0", 1, teacher, tsB, ConstraintTestHelper.DUMMY_ROOM);
        Lesson lessonC = Lesson.test_buildLesson("penalty2", 1, "", "", "",
                "0-0-0", 1, teacher, tsC, ConstraintTestHelper.DUMMY_ROOM);

        //------ No penalty
        EnumSet<Days> RF = EnumSet.of(Days.THURSDAY, Days.FRIDAY);
        BitSet lectureRf = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:00PM", formatter), 2, RF);
        BitSet labRf = BitSetHelper.timeSlotBitSet(LocalTime.parse("5:00PM", formatter), 2, RF);
        Timeslot rfTimeslot = Timeslot.test_lecLabBitAndDays(6, lectureRf, labRf, RF, RF);
        Teacher singleCourseTeacher = new Teacher(4, "single course", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, Preference.DISAGREE);
        Lesson singleCourse = Lesson.test_buildLesson("singleCourse", 1, "", "", "",
                "0-0-0", 1, singleCourseTeacher, rfTimeslot, ConstraintTestHelper.DUMMY_ROOM);
        //-------- No penalty
        Teacher gapPrefTeacher = new Teacher(3, "likes gaps", ConstraintTestHelper.EMPTY_BS,
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, Preference.AGREE);

        Lesson likedGap1 = Lesson.test_buildLesson("agree1", 1, "", "", "",
                "0-0-0", 1, gapPrefTeacher, tsA, ConstraintTestHelper.DUMMY_ROOM);
        Lesson likedGap2 = Lesson.test_buildLesson("agree2", 1, "", "", "",
                "0-0-0", 1, gapPrefTeacher, tsB, ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::penalizeDislikedHourGap)
                .given(lessonA, lessonB, lessonC,
                        singleCourse,
                        likedGap1, likedGap2)
                .penalizesBy(2);
    }

    @Test
    @DisplayName("Penalty: room prescheduling blocks lab and studio lecture")
    void roomPrescheduleConflict() {
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        BitSet blockedLessonBits = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter), 2, MWF);
        BitSet blockedLessonBits2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MWF);
        Timeslot blockedTimeslot = Timeslot.test_lecLabBitAndDays(1, blockedLessonBits, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);
        Timeslot blockedTimeslot_LecLab = Timeslot.test_lecLabBitAndDays(2, blockedLessonBits2, blockedLessonBits,
                MWF, MWF);

        Lesson blockedLessonLab = Lesson.test_buildLesson("room-blocked-lab", 2, "", "", "",
                "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER,blockedTimeslot_LecLab,
                ConstraintTestHelper.TEST_ROOM_PRESCHEDULED);
        Lesson blockedStudio = Lesson.test_buildLesson("room-blocked-studio", 3, ConstraintTestHelper.DUMMY_STUDIO,
                "", "", "1-0-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, blockedTimeslot, ConstraintTestHelper.TEST_ROOM_PRESCHEDULED);

        constraintVerifier.verifyThat(TimetableConstraintProvider::roomPreschedule)
                .given(blockedLessonLab, blockedStudio)
                .penalizesBy(2);
    }

    @Test
    @DisplayName("No Penalty: room prescheduling skip branches")
    void noRoomPrescheduleConflict() {
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        BitSet roomBlocked = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter), 2, MWF);
        BitSet bs_10AM = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter), 2, MWF);

        Timeslot blockedTimeslot = Timeslot.test_lecLabBitAndDays(2, roomBlocked, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);
        Timeslot ts_9AM_10AM_MWF = Timeslot.test_lecLabBitAndDays(2, roomBlocked, bs_10AM, MWF, MWF);
        Timeslot ts_10AM_MWF = Timeslot.test_lecLabBitAndDays(2, bs_10AM, ConstraintTestHelper.EMPTY_BS, MWF,
                ConstraintTestHelper.NO_DAYS);

        Lesson openLessonLec = Lesson.test_buildLesson("room-open-lec", 1, "", "", "",
                "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_9AM_10AM_MWF,
                ConstraintTestHelper.TEST_ROOM_PRESCHEDULED);
        Lesson lecOnlyRoomLesson = Lesson.test_buildLesson("room-lec-only", 3, "", "", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, blockedTimeslot,
                ConstraintTestHelper.TEST_ROOM_PRESCHEDULED);
        Lesson emptyRoomLesson = Lesson.test_buildLesson("room-empty", 4, "", "", "",
                "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER, blockedTimeslot, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::roomPreschedule)
                .given(openLessonLec, lecOnlyRoomLesson, emptyRoomLesson)
                .penalizesBy(0);
    }
}
