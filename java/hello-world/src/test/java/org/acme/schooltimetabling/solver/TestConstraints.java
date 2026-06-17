package org.acme.schooltimetabling.solver;

import ai.timefold.solver.core.api.score.stream.Constraint;
import org.acme.schooltimetabling.builders.lessons.LessonBuilder;
import org.acme.schooltimetabling.builders.teachers.TeacherBuilder;
import org.acme.schooltimetabling.builders.teachers.policies.DefaultTeachingPolicy;
import org.acme.schooltimetabling.builders.teachers.policies.FacultyPolicy;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Timetable;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.fileObjects.LabPatterns;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.acme.schooltimetabling.helperClasses.Generators.LessonGenerator;
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
    final DateTimeFormatter FORMATTER = Constants.TIME_FORMATTER;

    @BeforeAll
    static void setUp(){
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        ConstraintTestHelper.load();
    }


    @Test
    @DisplayName("Testing same teacher same course constraint")
    void sameLessonNTeacher(){
        LessonBuilder builder = new LessonBuilder();
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

        Timeslot timeslot3 = Timeslot.test_CreateWithDaysOnly(3, MWF, MWF);
        Lesson ls3 = Lesson.test_buildLesson("3", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot3, ConstraintTestHelper.DUMMY_ROOM);



        Teacher teacher2 = new Teacher(2, "rand",
                ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS, ConstraintTestHelper.EMPTY_BS);
        Lesson ls5 =  Lesson.test_buildLesson("1", 1, "csc201", "dummyInstructor", "",
                "3-1-0", 1, teacher2, timeslot1, ConstraintTestHelper.DUMMY_ROOM);
        Lesson ls4 = builder
                .id(4)
                .section(5)
                .courseName("csc201")
                .courseConfig("3-1-0")
                .teacherObj(teacher2)
                .courseId(1)
                .timeslot(Timeslot.test_CreateWithDaysOnly(4, MWF, ConstraintTestHelper.NO_DAYS))
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();


        constraintVerifier.verifyThat(TimetableConstraintProvider::sameClassSameDays)
                .given(ls1, ls2, ls3, ls4, ls5)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(3);

    }


    @Test
    @DisplayName("teacher conflict times & timeslot conflict")
    void teacherAndTimeslot(){
        //No penalty for teacher1
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet teacher1Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", FORMATTER)
                , 4, MW);
        Teacher teacher1 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(teacher1Bits)
                .canon("instructor1")
                .gapPref(Preference.NEUTRAL)
                .build();
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", FORMATTER)
                , 4, MW);
        Timeslot ts_10AM_MW = Timeslot.test_lecLabBitAndDays(1, ts1, ConstraintTestHelper.EMPTY_BS,
                MW, ConstraintTestHelper.NO_DAYS);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, ts_10AM_MW, ConstraintTestHelper.DUMMY_ROOM);

        //No Penalty 1+ for teacher2
        BitSet teacher2Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", FORMATTER)
                , 4, MW);
        Teacher teacher2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(teacher2Bits)
                .canon("instructor2")
                .gapPref(Preference.NEUTRAL)
                .build();
        BitSet bs_MW_7AM_blck2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", FORMATTER), 2, MW);
        BitSet bs_MW_8AM_blcks2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", FORMATTER), 2, MW);
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

        BitSet bs_9pm_2blcks_MW = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00PM", FORMATTER)
                , 2, MW);
        BitSet bs_8pm_2blcks_MW = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00PM", FORMATTER)
                , 2, MW);

        Teacher teacher = new TeacherBuilder(new FacultyPolicy(bs_9pm_2blcks_MW))
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("instructor1")
                .gapPref(Preference.NEUTRAL)
                .preschedule(bs_9pm_2blcks_MW)
                .build();

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
        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", FORMATTER)
                , 6, enumSet);
        //1PM-4 MW
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, ts1, ConstraintTestHelper.EMPTY_BS, enumSet, enumSet);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);


        EnumSet<Days> enumSet2 = EnumSet.of(Days.WEDNESDAY);
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:30PM", FORMATTER)
                , 2, enumSet2);
        //3:30PM-4:30PM W
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, ts2, ConstraintTestHelper.EMPTY_BS,
                enumSet2, ConstraintTestHelper.NO_DAYS);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName",
                "", "1-0-0", 3, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);


        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_1PM_MWF = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", FORMATTER), 2, MWF);
        BitSet bs_2PM_MWF = BitSetHelper.timeSlotBitSet(LocalTime.parse("2:00PM", FORMATTER), 2, MWF);
        Timeslot ts_1PM_MWF_blks4 = Timeslot.test_lecLabBitAndDays(1, bs_1PM_MWF, bs_2PM_MWF, MWF
                , MWF);


        Teacher teacher2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("instructor2")
                .gapPref(Preference.NEUTRAL)
                .build();
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName",
                "", "3-1-0", 3,  teacher2, ts_1PM_MWF_blks4,
                ConstraintTestHelper.DUMMY_ROOM);

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
        BitSet bs_MWF_9AM_4blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", FORMATTER)
                , 4, MWF);
        //9-10 MWF
        BitSet bs_MWF_9AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", FORMATTER)
                , 2, MWF);
        //10-11 MWF
        BitSet bs_MWF_10_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", FORMATTER),
                2, MWF);
        //9-11:30 TR
        BitSet bs_MWF_11_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("11:00AM", FORMATTER),
                2, MWF);

        Timeslot ts_mwf_9_4blcks = Timeslot.test_lecLabBitAndDays(1, bs_MWF_9AM_4blcks, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);
        Timeslot ts_mwf_9_10_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_MWF_9AM_2blcks, bs_MWF_10_2blcks,
                MWF, MWF);
        //no conflict with either
        Timeslot ts_lec_mwf_10_lab_mwf_11 = Timeslot.test_lecLabBitAndDays(3, bs_MWF_10_2blcks, bs_MWF_11_2blcks,
                MWF, MWF);
        Timeslot ts_mwf_11 = Timeslot.test_lecLabBitAndDays(4, bs_MWF_11_2blcks, ConstraintTestHelper.EMPTY_BS,
                MWF, ConstraintTestHelper.NO_DAYS);


        //potential time conflict: 8-10am
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName"
                , "", "0-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_mwf_9_4blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        //potential time conflict: mwf 10-11pm
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_mwf_9_10_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);
        //potential time conflict: mwf 11-12pm
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_lec_mwf_10_lab_mwf_11,
                ConstraintTestHelper.DUMMY_ROOM);
        //potential time conflict mwf: 11-12pm
        Lesson lesson4studio = Lesson.test_buildLesson("4", 1, ConstraintTestHelper.DUMMY_STUDIO, "", "",
                "3-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_mwf_11,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(lesson1, lesson2, lesson3, lesson4studio)
                .penalizesBy(2);
    }

    @Test
    @DisplayName("A room accommodates only one lesson at a time; test2")
    void roomMultiLessons2(){
        Timeslot timeslot1 = new Timeslot(1,
                "M", "9:00AM", "12:00PM", 3f, 3f,
                "", "", "", 0);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName"
                , "", "0-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = new Timeslot(2,
                "MWF", "8:00AM", "10:00AM", 1f, 2f,
                "", "", "", 0);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName"
                , "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(lesson1, lesson2)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Correct time slot hours for course type")
    void timeslotAndLessonTimeMatch(){
        LessonBuilder builder = new LessonBuilder();
        Timeslot ts_mwf_9am_12pm = new Timeslot(1,
                "MWF", "9:00AM", "12:00PM", 1f, 3f,
                "", "", "", 0);

        Timeslot ts_tr_10_1pm = new Timeslot(2,
                "tr", "10:00AM", "1:00PM", 1f, 3f,
                "", "", "" , 0);

        Timeslot ts_m_7_10am = new Timeslot(3,
                "m", "7:00AM", "10:00AM", 3, 3,
                "", "", "", 0);


        Lesson lec_lab = builder
                .clear()
                .id(1)
                .section(1)
                .courseName("")
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .timeslot(ts_mwf_9am_12pm)
                .build();

        Lesson lec_lab2 = builder
                .clear()
                .id(2)
                .section(3)
                .courseName("")
                .courseConfig("2-0-1")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .timeslot(ts_tr_10_1pm)
                .build();

        Lesson lab = builder
                .clear()
                .id(3)
                .section(1)
                .courseName("continuous")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .timeslot(ts_m_7_10am)
                .build();

        Lesson lec = builder
                .clear()
                .id(3)
                .section(1)
                .courseName("continuous")
                .courseConfig("3-0-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .timeslot(ts_m_7_10am)
                .build();




        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(lec_lab, lec_lab2, lab ,lec)
                .penalizesBy(0);
    }


    @Test
    @DisplayName("Wrong time slot hours for course type")
    void timeslotAndLessonTimeMismatch(){
        LessonBuilder builder = new LessonBuilder();
        Timeslot ts_tr_9_11am = new Timeslot(1,
                "TR", "9:00AM", "11:00AM", 1f, 2f,
                "", "", "", 0);
        Timeslot ts_m_7_10am = new Timeslot(3,
                "m", "7:00AM", "10:00AM", 3, 3,
                "", "", "", 0);

        //lesson with lab and lec
        Lesson lec_lab = builder
                .clear()
                .id(1)
                .section(1)
                .courseName("")
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(ts_tr_9_11am)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();
        //lesson with only lab
        Lesson act = builder
                .clear()
                .id(2)
                .section(3)
                .courseName("")
                .courseConfig("0-0-1")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(ts_m_7_10am)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();
        //lesson with only lec
        Lesson lec = builder
                .clear()
                .id(3)
                .section(4)
                .courseName("")
                .courseConfig("1-0-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(ts_m_7_10am)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(lec_lab, act, lec)
                .penalizesBy(3);
    }


    @Test
    @DisplayName("Penalty: Lesson should be in the right Room")
    void penLessonRoomCheck(){
        //recreating the lecture only room
        final int LEC_ONLY_ID = Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY);
        final Room LEC_ONLY_ROOM = new Room(Integer.toString(LEC_ONLY_ID), "LEC_ONLY", LEC_ONLY_ID);

        //Lesson requires a specific room
        Lesson lesson1 = Lesson.test_buildLesson("0", 1, ConstraintTestHelper.NON_STUDIO_SPECIFIC, "",
                "", "2-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.NON_STUDIO_SPECIFIC),
                ConstraintTestHelper.DUMMY_TEACHER, ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_RANDO_LAB);

        //lecture only room needs to be in lecture room
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "some only lec course", "",
                "", "1-0-0", 1111, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_RANDO_LAB);

        Lesson lsLabOnly = Lesson.test_buildLesson("2", 1, ConstraintTestHelper.TEST_STUDIO_SPECIFIC,
                "noName", "", "2-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_RANDO_LAB);

        Lesson lecOnlyStudioSplit = Lesson.test_buildLesson("3", 1, ConstraintTestHelper.DUMMY_STUDIO,
                "", "", "1-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, LEC_ONLY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2, lsLabOnly, lecOnlyStudioSplit)
                .penalizesBy(4);
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
        Lesson lsLabOnly = Lesson.test_buildLesson("2", 1, "activity course",
                "noName", "", "0-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.DUMMY_ROOM);

        Lesson studioLecSplit = Lesson.test_buildLesson("3", 1, ConstraintTestHelper.TEST_STUDIO_SPECIFIC,
                "noName", "", "0-0-1", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_SPECIFIC);
        Lesson studioLabSplit = Lesson.test_buildLesson("4", 1, ConstraintTestHelper.TEST_STUDIO_SPECIFIC,
                "noName", "", "2-0-0", 1, ConstraintTestHelper.DUMMY_TEACHER,
                ConstraintTestHelper.DUMMY_TS, ConstraintTestHelper.TEST_ROOM_SPECIFIC);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2, lsLabOnly, studioLecSplit, studioLabSplit)
                .penalizesBy(NO_PENALTY);
    }



    @Test
    @DisplayName("PrimeTime reward")
    void primeTimeReward(){

        //mwf 4- 2 in 2 out
        Timeslot ts_1in1out = new Timeslot(1,
                "MF", "8:00AM", "12:00PM", 2f, 4f,
                "", "", "", 0);
        //add 4 to the reward
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-2", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_1in1out,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_0out = new Timeslot(2,
                "M", "9:00AM", "10:00AM", 1f, 1f,
                "", "", "", 0);
        //add 0 to the reward
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "1-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_0out,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_5out = new Timeslot(3,
                "MWF", "4:00PM", "6:00PM", 2f,2f,
                "", "" , "", 0);
        //add 0 to reward
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-2-0", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_5out,
                ConstraintTestHelper.DUMMY_ROOM);
        //add 12 to reward
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, ConstraintTestHelper.TEST_STUDIO_SPECIFIC,
                "", "", "6-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_5out,
                ConstraintTestHelper.DUMMY_ROOM);

        //f 8 hours
        //lec only 6
        constraintVerifier.verifyThat(TimetableConstraintProvider::outPrimeTime)
                .given(lesson1, lesson2, lesson3, lesson4)
                /*Note this takes into account weight of rewards*/
                .rewardsWith(16);
    }


    @Test
    @DisplayName("PrimeTime penalty")
    void primeTimePenalty(){
        //mwf 4- 2 in 2 out
        Timeslot ts_1in1out = new Timeslot(1,
                "MF", "8:00AM", "12:00PM", 2f, 4f,
                "", "", "", 0);
        //add 4 to the penalty
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-2", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_1in1out,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_2in = new Timeslot(2,
                "MF", "11:00AM", "12:00PM", 1f, 1f,
                "", "", "", 0);
        //lab in 0 towards penalty
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "0-0-1", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_2in,
                ConstraintTestHelper.DUMMY_ROOM);
        //all time in; 4 towards penalty
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "2-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_2in,
                ConstraintTestHelper.DUMMY_ROOM);

        //all time out
        Timeslot ts_8out = new Timeslot(3,
                "MTWR", "12:00PM", "3:00PM", 3f, 3f,
                "", "", "", 0);
        //24 towards penalty
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "12-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_8out,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::inPrimeTime)
                .given(lesson1, lesson2, lesson3, lesson4)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(32);
    }


    @Test
    @DisplayName("no penalty Primetime hard constraint")
    void hardPrimeTimeNoPenalty(){
        Timeslot ts_1in1out = new Timeslot(1,
                "MF", "8:00AM", "12:00PM", 2f, 4f,
                "", "", "", 0);
        //add 4 to the penalty
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-2", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_1in1out,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_4NHalfIn = new Timeslot(2,
                "M", "9:00AM", "1:30PM", 4.5f, 4.5f,
                "", "", "",0);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "4-4-4", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_4NHalfIn,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_5Out = new Timeslot(3,
                "M", "3:00PM", "8:00PM", 5f, 5f,
                "", "", "",0);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "4-4-4", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_5Out,
                ConstraintTestHelper.DUMMY_ROOM);

        Lesson lesson4_LabOnly = Lesson.test_buildLesson("4", 1, "", "", "",
                "0-0-4", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_4NHalfIn,
                ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::primeTime50Plus)
                .given(lesson1, lesson2, lesson3, lesson4_LabOnly)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("penalty Primetime hard constraint")
    void hardPrimeTimePenalty(){
        Timeslot ts_1in1out = new Timeslot(1,
                "MF", "8:00AM", "12:00PM", 2f, 4f,
                "", "", "", 0);
        //add 4 to the penalty
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-2", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_1in1out,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_5In = new Timeslot(2,
                "M", "9:00AM", "2:00PM", 5f, 5f,
                "", "", "",0);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-4-4", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_5In,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot ts_4NHalfOut = new Timeslot(3,
                "M", "3:00PM", "7:30PM", 4.5f, 4.5f,
                "", "", "",0);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "4-4-4", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_4NHalfOut,
                ConstraintTestHelper.DUMMY_ROOM);

        Lesson lesson4_LabOnly = Lesson.test_buildLesson("4", 1, "", "", "",
                "0-0-4", 2, ConstraintTestHelper.DUMMY_TEACHER, ts_4NHalfOut,
                ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::primeTime50Plus)
                .given(lesson1, lesson2, lesson3, lesson4_LabOnly)
                .penalizesBy(1);
    }



    @Test
    @DisplayName("no Pen: discourage large schedule gaps or long days")
    void teacherNoLargeGaps(){
        Teacher TEACHER2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.NEUTRAL)
                .build();

        Timeslot timeslot1 = new Timeslot(1,
                "MW", "8:00AM", "10:00AM", 2f, 2f,
                "F", "9:00AM", "11:00AM", 2f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-1", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = new Timeslot(2,
                "MTWR", "1:00PM", "4:00PM", 3f, 3f,
                "", "", "", 0);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "12-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot3 = new Timeslot(3,
                "M", "7:00PM", "9:00PM", 2f, 2f,
                "", "", "", 0);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-0-1", 2, TEACHER2, timeslot3,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::compressIndividualTeachTime)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(0);
    }


    @Test
    @DisplayName("Penalty: discourage large schedule gaps or long days")
    void penalizeLargeGapsAndLongDays(){
        Teacher TEACHER2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.NEUTRAL)
                .build();

        //3 hour gap check
        Timeslot timeslot1 = new Timeslot(1,
                "MW", "8:00AM", "10:00AM", 2f, 2f,
                "", "", "", 0);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = new Timeslot(2,
                "MW", "1:30PM", "3:00PM", 1.5f, 1.5f,
                "", "", "", 0);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "0-1-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        //8+ hour time in one day check
        Timeslot timeslot3 = new Timeslot(3,
                "R", "1:30PM", "7:30PM", 6f, 6f,
                "", "", "", 0);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-2-0", 2, TEACHER2, timeslot3,
                ConstraintTestHelper.DUMMY_ROOM);


        Timeslot timeslot4 = new Timeslot(4,
                "RF", "7:00AM", "12:00PM", 2f, 5f,
                "", "", "", 0);
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "4-0-2", 2, TEACHER2, timeslot4,
                ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::compressIndividualTeachTime)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(2);
    }



    @Test
    @DisplayName("Reward: teacher prefers one-hour gaps")
    void rewardPreferredHourGap() {
        //no reward if gap is within the timeslot
        Teacher TEACHER1 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.AGREE)
                .build();
        Timeslot timeslot1 = new Timeslot(1,
                "MW", "8:00AM", "11:00AM", 1f, 3f,
                "", "", "", 0);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "2-0-2", 2, TEACHER1, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        //-------- penalty
        Teacher TEACHER2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.AGREE)
                .build();

        Timeslot timeslot2 = new Timeslot(2,
                "M", "12:00PM", "1:00PM", 1f, 1f,
                "W", "12:00PM", "1:00PM", 1f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "2-0-2", 2, TEACHER2, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "1-0-2", 2, TEACHER2, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        //------ no reward if teacher doesn't care
        Teacher TEACHER3 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.NEUTRAL)
                .build();

        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "2-0-2", 2, TEACHER3, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson5 = Lesson.test_buildLesson("5", 1, "", "", "",
                "1-0-2", 2, TEACHER3, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);



        constraintVerifier.verifyThat(TimetableConstraintProvider::rewardPreferredHourGap)
                .given(lesson1,
                        lesson2, lesson3,
                        lesson4, lesson5)
                .rewardsWith(1);
    }

    @Test
    @DisplayName("Penalty: teacher dislikes one-hour gaps")
    void penalizeDislikedHourGap() {
        //no penalty if gap is within the timeslot
        Teacher TEACHER1 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.DISAGREE)
                .build();
        Timeslot timeslot1 = new Timeslot(1,
                "MW", "8:00AM", "11:00AM", 1f, 3f,
                "", "", "", 0);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "2-0-2", 2, TEACHER1, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        //-------- penalty
        Teacher TEACHER2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.DISAGREE)
                .build();

        Timeslot timeslot2 = new Timeslot(2,
                "M", "12:00PM", "1:00PM", 1f, 1f,
                "W", "12:00PM", "1:00PM", 1f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "2-0-2", 2, TEACHER2, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "1-0-2", 2, TEACHER2, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        //------ no reward if teacher doesn't care
        Teacher TEACHER3 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher2")
                .gapPref(Preference.NEUTRAL)
                .build();

        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "2-0-2", 2, TEACHER3, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson5 = Lesson.test_buildLesson("5", 1, "", "", "",
                "1-0-2", 2, TEACHER3, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::penalizeDislikedHourGap)
                .given(lesson1,
                        lesson2, lesson3,
                        lesson4, lesson5)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Penalty: room prescheduling blocks lab and studio lecture")
    void roomPrescheduleConflict() {
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        BitSet blockedLessonBits = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", FORMATTER), 2, MWF);
        BitSet blockedLessonBits2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", FORMATTER), 2, MWF);
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
        Lesson blockedLabOnly = Lesson.test_buildLesson("room-blocked-studio", 3, ConstraintTestHelper.DUMMY_STUDIO,
                "", "", "1-0-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, blockedTimeslot, ConstraintTestHelper.TEST_ROOM_PRESCHEDULED);

        constraintVerifier.verifyThat(TimetableConstraintProvider::roomPreschedule)
                .given(blockedLessonLab, blockedStudio, blockedLabOnly)
                .penalizesBy(3);
    }

    @Test
    @DisplayName("No Penalty: room prescheduling skip branches")
    void noRoomPrescheduleConflict() {
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);

        BitSet roomBlocked = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", FORMATTER), 2, MWF);
        BitSet bs_10AM = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", FORMATTER), 2, MWF);

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

    @Test
    @DisplayName("Teacher preferred time reward")
    void teacherPrefReward(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet preference = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", FORMATTER), 4, MWF);
        Teacher TEACHER1 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(preference)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("teacher with a preference 10-12pm MWF")
                .gapPref(Preference.NEUTRAL)
                .build();

        Timeslot timeslot1 = new Timeslot(1,
                "M", "10:00AM", "11:00AM", 1f, 1f,
                "W", "10:00AM", "11:30AM", 1.5f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "9-9-9", 2, TEACHER1, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::prefTime)
                .given(lesson1)
                .rewardsWith(2);
    }

    @Test
    @DisplayName("Reward: compress global schedule")
    void compressGlobalReward(){
        Timeslot timeslot1 = new Timeslot(1,
                "M", "9:00AM", "10:00AM", 1f, 1f,
                "W", "10:00AM", "11:30AM", 1.5f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "9-9-9", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = new Timeslot(2,
                "MW", "10:00AM", "11:00AM", 1f, 1f,
                "", "", "", 0f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "2-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-0-1", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        //in bad time
        Timeslot timeslot3 = new Timeslot(2,
                "MW", "9:00PM", "10:00PM", 1f, 1f,
                "", "", "", 0f);
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "0-0-1", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot3,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::inBestTime)
                .given(lesson1, lesson2, lesson3, lesson4)
                .rewardsWith(3);
    }

    @Test
    @DisplayName("Penalty: compress global schedule")
    void compressGlobalPenalty(){
        Timeslot timeslot1 = new Timeslot(1,
                "M", "9:00AM", "10:00AM", 1f, 1f,
                "W", "10:00AM", "11:30AM", 1.5f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "9-9-9", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = new Timeslot(2,
                "MW", "2:00PM", "3:00PM", 1f, 1f,
                "", "", "", 0f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "2-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-0-1", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        //in good time
        Timeslot timeslot3 = new Timeslot(2,
                "MW", "10:00AM", "11:00AM", 1f, 1f,
                "", "", "", 0f);
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "0-0-1", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot3,
                ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::outBestTime)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(3);
    }

    @Test
    @DisplayName("No penalty: timeslot pattern match")
    void noPenTimeslotPattern(){
        LessonBuilder builder = new LessonBuilder();

        Timeslot timeslot1 = new Timeslot(1,
                "MTWR", "7:00AM", "8:00AM", 1f, 1f,
                "", "" ,"", 0f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "4-0-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot2 = new Timeslot(2,
                "TR", "7:00AM", "11:00AM", 1.5f, 4f,
                "", "" ,"", 0f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "3-1-0", 2, ConstraintTestHelper.DUMMY_TEACHER, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot3 = new Timeslot(3,
                "MWF", "7:00AM", "8:00AM", 1f, 1f,
                "", "" ,"", 0f);
        Lesson lesson3 = builder
                .clear()
                .id(3)
                .section(10)
                .courseName("lab multiple blocks")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .courseId(1)
                .timeslot(timeslot3)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .labPattern(LabPatterns.MULTIPLE)
                .build();

        Timeslot timeslot4 = new Timeslot(4,
                "M", "7:00AM", "10:00AM", 3f, 3f,
                "", "" ,"", 0f);
        Lesson lesson4 = builder
                .clear()
                .id(4)
                .section(11)
                .courseName("lab one block")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .courseId(1)
                .timeslot(timeslot4)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .labPattern(LabPatterns.ONE)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::timeslotPatternMatch)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(NO_PENALTY);
    }


    @Test
    @DisplayName("No penalty: studios must have lab right after lec")
    void noPenStudioLabSpacing(){
        LessonBuilder builder = new LessonBuilder();
        //course with gap but not studio
        Timeslot timeslot1 = new Timeslot(1,
                "MWF", "12:00PM", "3:00PM", 1f, 3f,
                "", "" ,"", 0f);
        Lesson lesson1 = builder
                .clear()
                .id(1)
                .section(1)
                .courseName("")
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot1)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();
        //course split but do only lec part
        int linkerId = LessonGenerator.nextLinkerID();
        Timeslot timeslot2 = new Timeslot(2,
                "MWF", "12:00PM", "1:00PM", 1f, 1f,
                "", "" ,"", 0f);
        Lesson lesson2 = builder
                .clear()
                .id(2)
                .section(1)
                .courseName(ConstraintTestHelper.DUMMY_STUDIO)
                .courseConfig("3-0-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot2)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .linkerId(linkerId)
                .build();
        Timeslot timeslot3 = new Timeslot(3,
                "T", "12:00PM", "3:00PM", 3f, 3f,
                "", "" ,"", 0f);
        Lesson lesson3 = builder
                .clear()
                .id(3)
                .section(1)
                .courseName(ConstraintTestHelper.DUMMY_STUDIO)
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot3)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .linkerId(linkerId)
                .labPattern(LabPatterns.ONE)
                .build();

        //do a studio with the right spacing
        Timeslot timeslot4 = new Timeslot(4,
                "MWF", "12:00PM", "2:00PM", 1f, 2f,
                "", "" ,"", 0f);
        Lesson lesson4 = builder
                .clear()
                .id(4)
                .section(1)
                .courseName(ConstraintTestHelper.DUMMY_STUDIO)
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot4)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::timeslotPatternMatch)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(NO_PENALTY);
    }

    @Test
    @DisplayName("Penalty: studios must have lab right after lec")
    void penStudioLabSpacing(){
        LessonBuilder builder = new LessonBuilder();
        //do course with lab days < lec
        Timeslot timeslot1 = new Timeslot(1,
                "MWF", "12:00PM", "1:00PM", 1f, 1f,
                "MW", "1:00PM" ,"2:30PM", 1.5f);
        Lesson lesson1 = builder
                .clear()
                .id(1)
                .section(1)
                .courseName("")
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot1)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();
        //do course with lab days > lec
        Timeslot timeslot2 = new Timeslot(2,
                "MWF", "12:00PM", "1:00PM", 1f, 1f,
                "MTWF", "1:00PM" ,"2:00PM", 1f);
        Lesson lesson2 = builder
                .clear()
                .id(2)
                .section(1)
                .courseName(ConstraintTestHelper.DUMMY_STUDIO)
                .courseConfig("4-1-2")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot2)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();
        //do with days == for both but with spacing
        Timeslot timeslot3 = new Timeslot(3,
                "MWF", "12:00PM", "3:00PM", 1f, 3f,
                "", "" ,"", 0f);
        Lesson lesson3 = builder
                .clear()
                .id(3)
                .section(1)
                .courseName(ConstraintTestHelper.DUMMY_STUDIO)
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot3)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::timeslotPatternMatch)
                .given(lesson1, lesson2, lesson3)
                .penalizesBy(NO_PENALTY);
    }

    //new constraints from latest update
    @Test
    @DisplayName("Penalty: timeslot pattern match")
    void penTimeslotPatternMatch(){
        LessonBuilder builder = new LessonBuilder();
        Timeslot timeslot1 = new Timeslot(1,
                "R", "7:00AM", "10:00AM", 3f, 3f,
                "", "" ,"", 0f);
        Lesson lesson1 = builder
                .clear()
                .id(1)
                .section(10)
                .courseName("lab one block timeslot")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .courseId(1)
                .timeslot(timeslot1)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .labPattern(LabPatterns.MULTIPLE)
                .build();

        Timeslot timeslot2 = new Timeslot(2,
                "MWF", "7:00AM", "8:00AM", 1f, 1f,
                "", "" ,"", 0f);
        Lesson lesson2 = builder
                .clear()
                .id(2)
                .section(11)
                .courseName("lab multiple blocks timeslot")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .courseId(1)
                .timeslot(timeslot2)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .labPattern(LabPatterns.ONE)
                .build();

        Lesson lesson3 = builder
                .clear()
                .id(3)
                .section(12)
                .courseName("lec with cont. timeslot")
                .courseConfig("3-0-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .courseId(1)
                .timeslot(timeslot1)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .labPattern(null)
                .build();

        Timeslot timeslot3 = new Timeslot(3,
                "MWF", "7:00AM", "8:00AM", 1f, 1f,
                "M", "8:00AM" ,"11:00AM", 3f);
        Lesson lesson4 = builder
                .clear()
                .id(4)
                .section(12)
                .courseName("lec/lab with lab time continuous => bad")
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .courseId(1)
                .timeslot(timeslot3)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .labPattern(null)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::timeslotPatternMatch)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(4);
    }

    @Test
    @DisplayName("No penalty: prevent long time blocks taught")
    void noPenaltyLongTeaching(){
        Teacher TEACHER1 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("instructor1")
                .gapPref(Preference.NEUTRAL)
                .build();

        Timeslot timeslot1 = new Timeslot(1,
                "MTWR", "7:00AM", "1:00PM", 6f, 6f,
                "", "" ,"", 0f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "0-2-0", 2, TEACHER1, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);


        Timeslot timeslot2 = new Timeslot(2,
                "MTWR", "2:00PM", "8:00PM", 6f, 6f,
                "", "" ,"", 0f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "0-2-0", 2, TEACHER1, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);


        Timeslot timeslot3 = new Timeslot(3,
                "MTWR", "9:00PM", "10:00PM", 1f, 1f,
                "", "" ,"", 0f);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "0-0-2", 2, TEACHER1, timeslot3,
                ConstraintTestHelper.DUMMY_ROOM);

        Teacher TEACHER2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("instructor2")
                .gapPref(Preference.NEUTRAL)
                .build();
        Timeslot timeslot4 = new Timeslot(4,
                "MTWR", "1:00PM", "2:00PM", 1f, 1f,
                "MTWR", "8:00PM", "9:00PM", 1f);
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "4-0-2", 2, TEACHER2, timeslot4,
                ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::continuousTimeTaught)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Penalty: long time blocks taught found")
    void penaltyLongTeaching(){
        Teacher TEACHER1 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("instructor1")
                .gapPref(Preference.NEUTRAL)
                .build();

        Timeslot timeslot1 = new Timeslot(1,
                "MTWR", "7:00AM", "1:00PM", 6f, 6f,
                "", "" ,"", 0f);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "", "", "",
                "1-1-1", 2, TEACHER1, timeslot1,
                ConstraintTestHelper.DUMMY_ROOM);


        Timeslot timeslot2 = new Timeslot(2,
                "MTWR", "3:00PM", "8:00PM", 5f, 5f,
                "", "" ,"", 0f);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "", "", "",
                "1-1-1", 2, TEACHER1, timeslot2,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot3 = new Timeslot(3,
                "MTWR", "1:00PM", "2:00PM", 1f, 1f,
                "", "" ,"", 0f);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "", "", "",
                "1-1-1", 2, TEACHER1, timeslot3,
                ConstraintTestHelper.DUMMY_ROOM);

        Timeslot timeslot4 = new Timeslot(4,
                "MTWR", "8:00PM", "10:00PM", 2f, 2f,
                "", "" ,"", 0f);
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "", "", "",
                "1-1-1", 2, TEACHER1, timeslot4,
                ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::continuousTimeTaught)
                .given(lesson1, lesson2, lesson3, lesson4)
                .penalizesBy(2);
    }


    @Test
    @DisplayName("No reward: check for course different times")
    void noRewardCourseInstances(){
        LessonBuilder builder = new LessonBuilder();
        final int COMMON_COURSE_ID = 1000;

        Timeslot timeslot1 = new Timeslot(1,
                "MWF", "7:00AM", "8:00AM", 1f, 1f,
                "", "" ,"", 0f);
        int link1 = LessonGenerator.nextLinkerID();
        Lesson lesson1 = builder
                .clear()
                .id(1)
                .section(1)
                .courseName("link1")
                .courseConfig("1-0-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot1)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .linkerId(link1)
                .courseId(COMMON_COURSE_ID)
                .build();


        Timeslot timeslot2 = new Timeslot(2,
                "TR", "3:00PM", "6:00PM", 3f, 3f,
                "", "" ,"", 0f);
        Lesson lesson2 = builder
                .clear()
                .id(2)
                .section(1)
                .courseName("link1")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot2)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .linkerId(link1)
                .courseId(COMMON_COURSE_ID)
                .labPattern(LabPatterns.ONE)
                .build();

        Timeslot timeslot3 = new Timeslot(3,
                "F", "12:00PM", "3:00PM", 3f, 3f,
                "", "" ,"", 0f);
        Lesson lesson3 = builder
                .clear()
                .id(3)
                .section(1)
                .courseName("some other course")
                .courseConfig("3-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot3)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .courseId(COMMON_COURSE_ID + 1)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::rewardCoursesDiffTimes)
                .given(lesson1, lesson2, lesson3)
                .rewardsWith(0);
    }

    @Test
    @DisplayName("Reward: check for course different times")
    void rewardCourseInstances(){
        LessonBuilder builder = new LessonBuilder();
        final int COMMON_COURSE_ID = 1000;
        Teacher TEACHER2 = new TeacherBuilder(new DefaultTeachingPolicy())
                .preference(ConstraintTestHelper.EMPTY_BS)
                .acceptable(ConstraintTestHelper.EMPTY_BS)
                .conflict(ConstraintTestHelper.EMPTY_BS)
                .canon("instructor1")
                .gapPref(Preference.NEUTRAL)
                .build();

        Timeslot timeslot1 = new Timeslot(1,
                "MWF", "7:00AM", "8:00AM", 1f, 1f,
                "", "" ,"", 0f);
        int link1 = LessonGenerator.nextLinkerID();
        Lesson lesson1 = builder
                .clear()
                .id(1)
                .section(1)
                .courseName("course1")
                .courseConfig("1-0-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot1)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .linkerId(link1)
                .courseId(COMMON_COURSE_ID)
                .build();


        Timeslot timeslot2 = new Timeslot(2,
                "TR", "3:00PM", "6:00PM", 3f, 3f,
                "", "" ,"", 0f);
        Lesson lesson2 = builder
                .clear()
                .id(2)
                .section(2)
                .courseName("course1")
                .courseConfig("0-1-0")
                .teacherObj(ConstraintTestHelper.DUMMY_TEACHER)
                .timeslot(timeslot2)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .linkerId(link1)
                .courseId(COMMON_COURSE_ID)
                .labPattern(LabPatterns.ONE)
                .build();

        Timeslot timeslot3 = new Timeslot(3,
                "MWF", "12:00PM", "3:00PM", 1f, 3f,
                "", "" ,"", 0f);
        Lesson lesson3 = builder
                .clear()
                .id(3)
                .section(3)
                .courseName("course1")
                .courseConfig("3-1-0")
                .teacherObj(TEACHER2)
                .timeslot(timeslot3)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .courseId(COMMON_COURSE_ID)
                .build();

        Timeslot timeslot4 = new Timeslot(4,
                "MWF", "7:00AM", "8:00AM", 1f, 1f,
                "TR", "3:00PM" ,"4:00PM", 1f);
        Lesson lesson4 = builder
                .clear()
                .id(4)
                .section(4)
                .courseName("course1")
                .courseConfig("3-1-0")
                .teacherObj(TEACHER2)
                .timeslot(timeslot4)
                .room(ConstraintTestHelper.DUMMY_ROOM)
                .courseId(COMMON_COURSE_ID)
                .build();

        constraintVerifier.verifyThat(TimetableConstraintProvider::rewardCoursesDiffTimes)
                .given(lesson1, lesson2, lesson3, lesson4)
                .rewardsWith(3);
    }




}
