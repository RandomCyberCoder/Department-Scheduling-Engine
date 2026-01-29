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
import org.acme.schooltimetabling.helperClasses.Generators.LessonGenerator;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.glassfish.jaxb.runtime.v2.runtime.reflect.opt.Const;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

public class TestConstraints {
    ConstraintVerifier<TimetableConstraintProvider, Timetable> constraintVerifier = ConstraintVerifier.build(
            new TimetableConstraintProvider(), Timetable.class, Lesson.class);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
    private static final EnumSet<Days> NO_DAYS = EnumSet.noneOf(Days.class);
    private static final BitSet EMPTY_BS = new BitSet();
    private final static String DUMMY_STUDIO = "dummyStudioCourse";
    private final static Room DUMMY_ROOM = new Room("1", "dummyRoom", 1);
    private final static Teacher DUMMY_TEACHER = new Teacher(
            1, "dummyInstructor", EMPTY_BS, EMPTY_BS, EMPTY_BS);

    private final static int DUMMY_LINKER = 1;
    /**
     * Test: room name; will be used by a specific course {@link #TEST_L_W_LAB_SPECIFIC}
     */
    private final static String TEST_LAB_ROOM = "LabRoom";
    /**
     * Test: name of lab room not assigned to an course specifically
     */
    private final static String TEST_RANDOM_LAB_ROOM = "Random lab room";
    private final static Set<String> TEST_SET_LAB_ROOMS = Set.of(TEST_LAB_ROOM);
    /**
     * Test: name of a course that has a specific lab*/
    private final static String TEST_L_W_LAB_SPECIFIC = "Lab/Act course";
    /**
     * Room that can be used by any lab
     */
    private static Room TEST_ROOM_RANDO_LAB;
    /**
     * Room that is used for a course specifically and can also be used by an lab
     */
    private static Room TEST_ROOM_SPECIFIC;




    @BeforeAll
    static void setUp(){
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        LessonGenerator.studio_detected = true;
        /*This is set for special behavior needed for testing*/
//        Constants.TESTING = true;
        Constants.POSSIBLE_ROOMS.add(TEST_LAB_ROOM);
        Constants.POSSIBLE_ROOMS.add(TEST_RANDOM_LAB_ROOM);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_RANDOM_LAB_ROOM, 1001);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_LAB_ROOM, 1000);

        Constants.COURSE_ID_BIMAP.put(DUMMY_STUDIO, 2000);
        Constants.STUDIO_STYLE_COURSES.add(DUMMY_STUDIO);
        Constants.STUDIO_STYLE_COURSES.add(TEST_L_W_LAB_SPECIFIC);
        Constants.COURSE_TO_ROOMS.put(TEST_L_W_LAB_SPECIFIC, TEST_SET_LAB_ROOMS);

        TEST_ROOM_RANDO_LAB = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_RANDOM_LAB_ROOM)),
                TEST_RANDOM_LAB_ROOM, Constants.ROOM_TO_ID_BIMAP.get(TEST_RANDOM_LAB_ROOM));
        TEST_ROOM_SPECIFIC = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_LAB_ROOM)),
                TEST_LAB_ROOM, Constants.ROOM_TO_ID_BIMAP.get(TEST_LAB_ROOM));
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
                "3-1-0", 1, DUMMY_TEACHER, timeslot1, DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_CreateWithDaysOnly(2, MW, MT);
        Lesson ls2 = Lesson.test_buildLesson("2", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, DUMMY_TEACHER, timeslot2, DUMMY_ROOM);

        Timeslot timeslot3 = Timeslot.test_CreateWithDaysOnly(2, MWF, MWF);
        Lesson ls3 = Lesson.test_buildLesson("3", 3, "csc201", "dummyInstructor", "",
                "3-1-0", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM);


        //studio check
        //penalize st_ls4-st_ls5 combo. Penalty +1
        Timeslot ts_MW = Timeslot.test_CreateWithDaysOnly(3, MW, NO_DAYS);
        Timeslot ts_MWF = Timeslot.test_CreateWithDaysOnly(4, MWF, NO_DAYS);
        Timeslot ts_T = Timeslot.test_CreateWithDaysOnly(5, soloT, NO_DAYS);
        Lesson st_ls4 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "", "",
                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_MW, DUMMY_ROOM, DUMMY_LINKER);
        Lesson st_ls5 = Lesson.test_buildLesson("5", 1, DUMMY_STUDIO, "", "",
                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_MWF, DUMMY_ROOM, DUMMY_LINKER);
        Lesson st_ls6_lab = Lesson.test_buildLesson("6", 1, DUMMY_STUDIO, "", "",
                "0-0-1", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_T, DUMMY_ROOM, DUMMY_LINKER);

        constraintVerifier.verifyThat(TimetableConstraintProvider::sameClassSameDays)
                .given(ls1, ls2, ls3,
                        st_ls4, st_ls5, st_ls6_lab)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(3);

    }




    @Test
    @DisplayName("teacher conflict times & timeslot conflict")
    void teacherAndTimeslot() throws Exception{
        //No penalty for teacher1
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet teacher1Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 4, MW);
        Teacher teacher1 = new Teacher(1, "instructor1", EMPTY_BS, EMPTY_BS, teacher1Bits);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter)
                , 4, MW);
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, ts1, EMPTY_BS, MW, NO_DAYS);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, timeslot1, DUMMY_ROOM);

        //No Penalty 1+ for teacher2
        BitSet teacher2Bits = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter)
                , 4, MW);
        Teacher teacher2 = new Teacher(1, "instructor1", EMPTY_BS, EMPTY_BS, teacher2Bits);
        BitSet bs_MW_7AM_blck2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter), 2, MW);
        BitSet bs_MW_8AM_blcks2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter), 2, MW);
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bs_MW_7AM_blck2, bs_MW_8AM_blcks2, MW, MW);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher2, timeslot2, DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherLessonConflict)
                .given(lesson1, lesson2)
                .penalizesBy(1);
    }




    @Test
    @DisplayName("Faculty override conflict & timeslot conflict")
    void facultyAndTimeslot() throws Exception{
        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        Room room = new Room("1", "dummyRoom", 1);

        Teacher teacher2 = new Faculty(1, "instructor1", EMPTY_BS, EMPTY_BS, EMPTY_BS);
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter)
                , 1, enumSet);
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, ts2, EMPTY_BS, enumSet, enumSet);
        Lesson lesson2 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher2, timeslot2, room);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherLessonConflict)
                .given(lesson2)
                .penalizesBy(1);
    }




    @Test
    @DisplayName("Test: teacher can't teach two lessons at the same time")
    void teacherLessonSameTime() throws Exception{
//        Room DUMMY_ROOM = new Room("1", "dummyRoom", 1);
        Teacher teacher1 = new Faculty(1, "instructor1", EMPTY_BS, EMPTY_BS, EMPTY_BS);

        EnumSet<Days> enumSet = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        BitSet ts1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter)
                , 6, enumSet);
        //1PM-4 MW
        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, ts1, EMPTY_BS, enumSet, enumSet);
        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName",
                "", "3-1-0", 2,  teacher1, timeslot1, DUMMY_ROOM);


        EnumSet<Days> enumSet2 = EnumSet.of(Days.WEDNESDAY);
        BitSet ts2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("3:30PM", formatter)
                , 2, enumSet2);
        //3:30PM-4:30PM W
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, EMPTY_BS, ts2, NO_DAYS, enumSet2);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName",
                "", "3-1-0", 3, teacher1, timeslot2, DUMMY_ROOM);


        EnumSet<Days> soloDay = EnumSet.of(Days.MONDAY);
        BitSet bs3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("2:00PM", formatter), 2, soloDay);
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_1PM_MWF = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter), 2, soloDay);
        BitSet bs_2PM_MWF = BitSetHelper.timeSlotBitSet(LocalTime.parse("2:00PM", formatter), 2, soloDay);
        Timeslot ts_1PM_MWF_blks4 = Timeslot.test_lecLabBitAndDays(1, bs_1PM_MWF, bs_2PM_MWF, MWF
                , MWF);

        Timeslot ts3 = Timeslot.test_lecLabBitAndDays(1, bs3, EMPTY_BS, soloDay, NO_DAYS);
        Teacher teacher2 = new Faculty(2, "instructor2", EMPTY_BS, EMPTY_BS, EMPTY_BS);
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName",
                "", "3-1-0", 3,  teacher2, ts_1PM_MWF_blks4, DUMMY_ROOM);
        Lesson st_lesson4 = Lesson.test_buildLesson("4", 4, "dummmyName", "noName",
                "", "0-1-0", 1, teacher2, ts3, TestConstraints.DUMMY_ROOM);

        //penalizes lesson1 and lesson2 grouping; penalizes lesson3 and st_lesson4 grouping
        constraintVerifier.verifyThat(TimetableConstraintProvider::lessonConflict)
                .given(lesson1, lesson2, lesson3, st_lesson4)
                .penalizesBy(2);
    }




    @Test
    @DisplayName("A room accommodates only one lesson at a time")
    void roomMultiLessons() throws Exception{
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> soloDay = EnumSet.of(Days.MONDAY);
        Room room = new Room("1", "dummyRoom", 1);

        //9-11 MWF
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 4, days);
        //10-11 MWF
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, days);
        EnumSet<Days> days2 = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        //9-11:30 TR
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                3, days2);
        //9AM-12PM M
        BitSet ts_bs1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM",formatter), 6, soloDay);
        //4pm-7pm M
        BitSet ts_bs2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("4:00PM", formatter), 6, soloDay);

        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, EMPTY_BS, bitSet1, NO_DAYS, days);
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(2, EMPTY_BS, bitSet2, NO_DAYS, days);
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(3, EMPTY_BS, bitSet3, NO_DAYS, days2);
        Timeslot st_ts_conflict = Timeslot.test_lecLabBitAndDays(4, ts_bs1, EMPTY_BS, soloDay, NO_DAYS);
        Timeslot st_ts_no_conflict = Timeslot.test_lecLabBitAndDays(6, ts_bs2, EMPTY_BS, soloDay, NO_DAYS);

        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "dummyName", "noName"
                , "", "3-1-0", 1, DUMMY_TEACHER, timeslot1, room);
        Lesson lesson2 = Lesson.test_buildLesson("2", 2, "dummyName", "noName"
                , "", "3-1-0", 1, DUMMY_TEACHER, timeslot2, room);
        Lesson lesson3 = Lesson.test_buildLesson("3", 3, "dummyName", "noName"
                , "", "3-1-0", 1, DUMMY_TEACHER, timeslot3, room);


        //make a studio split
        //no penalty (this is a lecture only)
        Lesson st_lec = Lesson.test_buildLesson("1", 1, "dummyName", "noName", "",
                "3-0-0", 1, DUMMY_TEACHER, st_ts_conflict, room, DUMMY_LINKER);
        //penalty 1+ (lab)
        Lesson st_lab_collision = Lesson.test_buildLesson("1", 1, "dummyName", "noName", "",
                "0-1-0", 1, DUMMY_TEACHER, st_ts_conflict, room, DUMMY_LINKER);
        //no penalty
        Lesson st_lab_no_collision = Lesson.test_buildLesson("1", 1, "dummyName", "noName", "",
                "0-1-0", 1, DUMMY_TEACHER, st_ts_no_conflict, room, DUMMY_LINKER);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(lesson1, lesson2, lesson3, st_lec, st_lab_collision, st_lab_no_collision)
                .penalizesBy(1);
    }




    @Test
    @DisplayName("Correct time slot hours for course type")
    void timeslotAndLessonTimeMatch() throws Exception{ //TODO test with studio split
        // TODO IMPLEMENT CONSTRAINT STILL!!!
        EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY,Days.FRIDAY);
        EnumSet<Days> days2 = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> days3 = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
        Room room = new Room("1", "dummyRoom", 1);
        Teacher teacher = new Faculty(1, "instructor1", EMPTY_BS, EMPTY_BS, EMPTY_BS);

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
        Timeslot timeslot4 = Timeslot.test_lecLabBitAndDays(4, bitSet5, EMPTY_BS, days3, NO_DAYS);
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
    @DisplayName("Constraint wrong hours for studio")
    void timeslotStudioMath() throws Exception{
        //no teache
        //no rooom
        EnumSet<Days> MTWR = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);
        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> M = EnumSet.of(Days.MONDAY);

        BitSet bs_M_7_to_11AM = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                8, M);
        BitSet bs_MW_7_to_9AM = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                4, MW);
        BitSet bs_MTWR_7_to_8_AM = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, MTWR);
        BitSet bs_M_7_to_8_AM = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, M);

        Timeslot ts_M_7_to_11AM = Timeslot.test_lecLabBitAndDays(1, bs_M_7_to_11AM, EMPTY_BS, M, NO_DAYS);
        Timeslot ts_MW_7_to_9AM = Timeslot.test_lecLabBitAndDays(2, bs_MW_7_to_9AM, EMPTY_BS, M, NO_DAYS);
        Timeslot ts_MTWR_7_to_8_AM = Timeslot.test_lecLabBitAndDays(3, bs_MTWR_7_to_8_AM, EMPTY_BS, MTWR, NO_DAYS);
        Timeslot ts_M_7_to_8_AM = Timeslot.test_lecLabBitAndDays(4, bs_M_7_to_8_AM, EMPTY_BS, M, NO_DAYS);

        //no penalty; lab section hours continuous
        Lesson st_lab_right_hrs = Lesson.test_buildLesson("1", 1, DUMMY_STUDIO, "", "",
                "0-0-2", 1, DUMMY_TEACHER, ts_M_7_to_11AM, DUMMY_ROOM, DUMMY_LINKER);
        //penalty +1; lab section hours not continuous
        Lesson st_lab_wrong_hrs = Lesson.test_buildLesson("2", 1, DUMMY_STUDIO, "", "",
                "0-0-2", 1, DUMMY_TEACHER, ts_MW_7_to_9AM, DUMMY_ROOM, DUMMY_LINKER);
        //penalty +1; lec only non-studio course has continuous lecture time
        Lesson ls_lec_only_wrong_ts = Lesson.test_buildLesson("3", 1, "notStudio", "", "",
                "4-0-0", 1, DUMMY_TEACHER, ts_M_7_to_11AM, DUMMY_ROOM);
        //no penalty; hours spread out
        Lesson ls_lec_only_right_ts = Lesson.test_buildLesson("4", 1, "notStudio", "", "",
                "4-0-0", 1, DUMMY_TEACHER, ts_MTWR_7_to_8_AM, DUMMY_ROOM);

        //no penalty
        Lesson st_lec_only_oneHR_right = Lesson.test_buildLesson("5", 1, DUMMY_STUDIO, "", "",
                "1-0-0", 1, DUMMY_TEACHER, ts_M_7_to_8_AM, DUMMY_ROOM, 2);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(st_lab_right_hrs, st_lab_wrong_hrs,
                        ls_lec_only_wrong_ts, ls_lec_only_right_ts,
                        st_lec_only_oneHR_right)
                .penalizesBy(2);
    }


    @Test
    @DisplayName("CSC Lesson should be in the right Room")
    void lessonRoomCheck(){
        /*TODO make this not rely on this assumption. GETTING ANNYOING. USE WHAT WE HAVE DONE FOR STUDIO COURSES
        *  IN THE SETUP*/

        //NOTE: this test assumes the department being scheduled is "CSC"
        int room301ID = Constants.ROOM_TO_ID_BIMAP.get("301");
        int lecOnlyID = Constants.ROOM_TO_ID_BIMAP.get(Constants.LEC_ONLY);
        /*Tests assumes that we are scheduling the CSC courses*/
        Room room1 = new Room(Integer.toString(room301ID), "301", 3);
        Room room2 = new Room("9999", "badRoom", 9999);
        Room lecOnlyRoom = new Room(Integer.toString(lecOnlyID), Constants.LEC_ONLY, lecOnlyID);
//        Teacher DUMMY_TEACHER = new Teacher(1, "noName", EMPTY_BS, EMPTY_BS, EMPTY_BS);
        Timeslot DUMMY_TS = Timeslot.test_minSetUp("1");

        Lesson lesson1 = Lesson.test_buildLesson("1", 1, "csc101", "noName"
                , "", "0-1-1", 1, DUMMY_TEACHER, DUMMY_TS, room1);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "csc101", "noName"
                , "", "0-1-1", 1, DUMMY_TEACHER, DUMMY_TS, room2);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "csc101", "noName"
                , "", "0-1-1", 1, DUMMY_TEACHER, DUMMY_TS, lecOnlyRoom);
        Lesson lesson4 = Lesson.test_buildLesson("4", 1, "csc445", "noName"
                , "", "0-0-0", 1, DUMMY_TEACHER, DUMMY_TS, lecOnlyRoom);
        Lesson lesson5 = Lesson.test_buildLesson("5", 1, "csc445", "noName"
                , "", "0-0-0", 1, DUMMY_TEACHER, DUMMY_TS, room1);

        //Test Studio splits
        //(lab) no penalty
        Lesson st_lab_spe_correct = Lesson.test_buildLesson("6", 1, TEST_L_W_LAB_SPECIFIC, "noName",
                "", "0-1-0", 1, DUMMY_TEACHER, DUMMY_TS, TEST_ROOM_SPECIFIC, DUMMY_LINKER);
        //penalty +1 (lab)
        Lesson st_lab_spe_wrong = Lesson.test_buildLesson("7", 1, TEST_L_W_LAB_SPECIFIC, "noName",
                "", "0-1-0", 1, DUMMY_TEACHER, DUMMY_TS, TEST_ROOM_RANDO_LAB, DUMMY_LINKER);
        //no penalty (lec)
        Lesson st_lec_right = Lesson.test_buildLesson("8", 1, TEST_L_W_LAB_SPECIFIC, "noName",
                "", "3-0-0", 1, DUMMY_TEACHER, DUMMY_TS, lecOnlyRoom, DUMMY_LINKER);
        //penalty +1 (lec)
        Lesson st_lec_wrong = Lesson.test_buildLesson("9", 1, TEST_L_W_LAB_SPECIFIC, "noName",
                "", "3-0-0", 1, DUMMY_TEACHER, DUMMY_TS, TEST_ROOM_RANDO_LAB, DUMMY_LINKER);

        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongRoomType)
                .given(lesson1, lesson2, lesson3, lesson4, lesson5
                        ,st_lab_spe_correct, st_lab_spe_wrong, st_lec_wrong, st_lec_right
                )
                .penalizesBy(5);
    }




    @Test
    @DisplayName("Studio Space test")
    void studioSpace() throws Exception{
        //simulate studio split using lesson generator
        //make a studio split. lecture only, then the lessons with a combo of lec and lab/act

        EnumSet<Days> MW = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
        EnumSet<Days> F = EnumSet.of(Days.FRIDAY);
        EnumSet<Days> MTWR = EnumSet.of(Days.MONDAY, Days.TUESDAY, Days.WEDNESDAY, Days.THURSDAY);

        /*7-9:30 MW*/
        BitSet bitSet1 = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter)
                , 5, MW);
        /*10-11 MW*/
        BitSet bitSet2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                2, MW);
        /*8:30-10:00 F*/
        BitSet bitSet3 = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter)
                , 3, F);
        /*9-10 MTWR*/
        BitSet bitSet4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter)
                , 1, MTWR);

        Timeslot timeslot1 = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet2, MW, MW);
        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bitSet1, bitSet3, MW, F);
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(1, bitSet4, EMPTY_BS, MTWR, NO_DAYS);
        Timeslot oneDay = Timeslot.test_lecLabBitAndDays(1, bitSet3, EMPTY_BS, F, NO_DAYS);

        //no penalty for lessons 1-3
        Lesson lesson = Lesson.test_buildLesson("1", 1, "nonStudio", "noName", "",
                "3-0-1", 1, DUMMY_TEACHER, timeslot1, DUMMY_ROOM);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "nonStudio", "noName", "",
                "2-1-0", 1, DUMMY_TEACHER, timeslot2, DUMMY_ROOM);
        Lesson lesson3 = Lesson.test_buildLesson("3", 1, "nonStudio", "noName", "",
                "4-0-0", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM);
        //mimic studio style split
        //no penalty
        Lesson studioLL1 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "doesn't matter", "",
                "1-0-0", 1, DUMMY_TEACHER, timeslot1, DUMMY_ROOM, DUMMY_LINKER);
        //non consec time; penalty +1
        Lesson studioLLA1 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "doesn't matter", "",
                "0-0-1", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM, DUMMY_LINKER);
        //consec time; no penalty
        Lesson studioLLA1_2 = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "doesn't matter", "",
                "0-0-1", 1, DUMMY_TEACHER, oneDay, DUMMY_ROOM, DUMMY_LINKER);

        constraintVerifier.verifyThat(TimetableConstraintProvider::studioSpace)
                .given(lesson, lesson2, lesson3, studioLL1, studioLLA1, studioLLA1_2)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(1);
    }


    @Test
    @DisplayName("Studio: lesson and lab order")
    void check_studioLabAfterLesson() throws Exception{
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        EnumSet<Days> M = EnumSet.of(Days.MONDAY);
        EnumSet<Days> T = EnumSet.of(Days.TUESDAY);

        BitSet bs_MWF_1PM_blcks2 = BitSetHelper.timeSlotBitSet(LocalTime.parse("1:00PM", formatter), 2, MWF);
        BitSet bs_M_9AM_blcks4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                4, M);
        BitSet bs_T_9AM_blcks4 = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                4, T);

        Timeslot ts_MWF_1PM = Timeslot.test_lecLabBitAndDays(1, bs_MWF_1PM_blcks2, EMPTY_BS, MWF, NO_DAYS);
        Timeslot ts_M_9AM = Timeslot.test_lecLabBitAndDays(2, bs_M_9AM_blcks4, EMPTY_BS, M, NO_DAYS);
        Timeslot ts_T_9AM = Timeslot.test_lecLabBitAndDays(3, bs_T_9AM_blcks4, EMPTY_BS, T, NO_DAYS);

        //penalize pair, +1
        Lesson st_lec1_MWF_1PM = Lesson.test_buildLesson("1", 1, DUMMY_STUDIO, "", "",
                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_MWF_1PM, DUMMY_ROOM,
                1);
        Lesson st_lab_M_9AM = Lesson.test_buildLesson("2", 1, DUMMY_STUDIO, "", "",
                "0-0-1", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_M_9AM, DUMMY_ROOM,
                1);

        //non penalty pair
        Lesson st_lec2_MWF_1PM = Lesson.test_buildLesson("3", 1, DUMMY_STUDIO, "", "",
                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_MWF_1PM, DUMMY_ROOM,
                2);
        Lesson st_lab_T_9AM = Lesson.test_buildLesson("4", 1, DUMMY_STUDIO, "", "",
                "3-0-0", Constants.COURSE_ID_BIMAP.get(DUMMY_STUDIO), DUMMY_TEACHER, ts_T_9AM, DUMMY_ROOM,
                2);

        constraintVerifier.verifyThat(TimetableConstraintProvider::studioLabAfterLesson)
                .given(st_lec1_MWF_1PM, st_lab_M_9AM,
                        st_lec2_MWF_1PM, st_lab_T_9AM)
                .penalizesBy(1);
    }


    @Test
    @DisplayName("PrimeTime reward")
    void primeTimeReward() throws Exception{
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
                "1-0-0", 1, DUMMY_TEACHER, timeslot, DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bitSet3, bitSet2, days, days);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "someCourse", "noName", "",
                "1-0-0", 1, DUMMY_TEACHER, timeslot2, DUMMY_ROOM, DUMMY_LINKER);

        //simulate studio lab/act split
        EnumSet<Days> oneDay = EnumSet.of(Days.MONDAY);
        BitSet onlyLecSetOneDay = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter), 6,
                oneDay);
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(1, onlyLecSetOneDay, EMPTY_BS, oneDay, NO_DAYS);
        Lesson lesson3 = Lesson.test_buildLesson("3", 2, "someCourse", "noName", "",
                "0-1-0", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM, DUMMY_LINKER);
        Lesson lesson4 = Lesson.test_buildLesson("4", 2, "someCourse", "noName", "",
                "3-0-0", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM, DUMMY_LINKER);

        constraintVerifier.verifyThat(TimetableConstraintProvider::outPrimeTime)
                .given(lesson, lesson2, lesson3, lesson4)
                /*Note this takes into account weight of rewards*/
                .rewardsWith(12 + 0 + 0 + 4);
    }




    @Test
    @DisplayName("PrimeTime penalty")
    void primeTimePenalty() throws Exception{EnumSet<Days> days = EnumSet.of(Days.MONDAY, Days.WEDNESDAY);
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
                "1-0-0", 1, DUMMY_TEACHER, timeslot, DUMMY_ROOM);

        Timeslot timeslot2 = Timeslot.test_lecLabBitAndDays(1, bitSet3, bitSet2, days2, days);
        Lesson lesson2 = Lesson.test_buildLesson("2", 1, "someCourse", "noName", "",
                "1-0-0", 1, DUMMY_TEACHER, timeslot2, DUMMY_ROOM);

        //studio lesson lab/act split
        EnumSet<Days> M = EnumSet.of(Days.MONDAY);
        BitSet onlyLecSetOneDay = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter), 6,
                M);
        Timeslot timeslot3 = Timeslot.test_lecLabBitAndDays(1, onlyLecSetOneDay, EMPTY_BS, M, NO_DAYS);
        Lesson lesson3 = Lesson.test_buildLesson("3", 2, "someCourse", "noName", "",
                "0-1-0", 1, DUMMY_TEACHER, timeslot3, DUMMY_ROOM, DUMMY_LINKER);


        constraintVerifier.verifyThat(TimetableConstraintProvider::inPrimeTime)
                .given(lesson1, lesson2, lesson3)
                /*Note this takes into account weight of rewards*/
                .penalizesBy(  2 + 6 + 0);
    }
}
