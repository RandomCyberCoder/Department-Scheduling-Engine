package org.acme.schooltimetabling.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Timetable;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

public class TestConstraintPropStudio {
    /*Constraints that have special behavior for studio classes are: sameClassSameDays, labActRoomConflict,
    * wrongHoursAmount*/
    ConstraintVerifier<TimetableConstraintProvider, Timetable> constraintVerifier = ConstraintVerifier.build(
            new TimetableConstraintProvider(), Timetable.class, Lesson.class);
    final static int NO_PENALTY = 0;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
    @BeforeAll
    static void setUp() {
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        ConstraintTestHelper.load();
    }

    @Test
    @DisplayName("Penalize course different patterns")
    void pen_PropDiffTchPttrn(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        EnumSet<Days> TR = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        Timeslot ts_MWF_TR = Timeslot.test_CreateWithDaysOnly(1, MWF, TR);
        Timeslot ts_TR_TR = Timeslot.test_CreateWithDaysOnly(2, TR, TR);
        Lesson ls1_MWF_TR = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_TR, ConstraintTestHelper.DUMMY_ROOM);
        Lesson ls2_TR_TR = Lesson.test_buildLesson("2", 3, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_TR_TR, ConstraintTestHelper.DUMMY_ROOM);


        constraintVerifier.verifyThat(TimetableConstraintProvider::sameClassSameDays)
                .given(ls1_MWF_TR, ls2_TR_TR
                )
                /*Note this takes into account weight of rewards*/
                .penalizesBy(1);
    }

    @Test
    @DisplayName("No Pen: course same pattern")
    void noPen_PropSameTchPttrn(){
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        EnumSet<Days> TR = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        Timeslot ts_MWF_TR = Timeslot.test_CreateWithDaysOnly(1, MWF, TR);
        Timeslot ts_MWF_MWF = Timeslot.test_CreateWithDaysOnly(2, MWF, MWF);
        Lesson ls1_MWF_TR = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_TR, ConstraintTestHelper.DUMMY_ROOM);
        Lesson ls2_MWF_MWF = Lesson.test_buildLesson("2", 3, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_MWF, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::sameClassSameDays)
                .given(ls1_MWF_TR, ls2_MWF_MWF
                )
                /*Note this takes into account weight of rewards*/
                .penalizesBy(NO_PENALTY);
    }

    @Test
    @DisplayName("Penalty: courses same room, w/ proper studio in mix")
    void pen_PropCoursesSameRoom() throws Exception{
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        EnumSet<Days> TR = EnumSet.of(Days.TUESDAY, Days.THURSDAY);
        BitSet bs_MWF_7AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_8AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_9AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                2, MWF);

        BitSet bs_TR_7AM_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                3, TR);
        BitSet bs_TR_830AM_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:30AM", formatter),
                3, TR);
        BitSet bs_TR_10AM_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("10:00AM", formatter),
                3, TR);

        Timeslot ts_MWF_7AM_2blcks_8AM_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_MWF_7AM_2blcks, bs_MWF_8AM_2blcks,
                MWF, MWF);
        Timeslot ts_MWF_8AM_2blcks_9AM_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_MWF_8AM_2blcks, bs_MWF_9AM_2blcks,
                MWF, MWF);
        Timeslot ts_TR_7AM_3blcks_830AM_3blcks = Timeslot.test_lecLabBitAndDays(1, bs_TR_7AM_3blcks, bs_TR_830AM_3blcks,
                MWF, MWF);
        Timeslot ts_TR_830AM_3blcks_10AM_3blcks = Timeslot.test_lecLabBitAndDays(2, bs_TR_830AM_3blcks, bs_TR_10AM_3blcks,
                MWF, MWF);

        Lesson combo1_st1 = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_8AM_2blcks_9AM_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson combo1_non_st1 = Lesson.test_buildLesson("2", 1, "non-studio", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_7AM_2blcks_8AM_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        Lesson combo2_st2 = Lesson.test_buildLesson("3", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_TR_7AM_3blcks_830AM_3blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson combo2_st3 = Lesson.test_buildLesson("4", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_TR_830AM_3blcks_10AM_3blcks, ConstraintTestHelper.DUMMY_ROOM);

        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(combo1_st1, combo1_non_st1,
                        combo2_st2, combo2_st3)
                .penalizesBy(2);

    }

    @Test
    @DisplayName("No penalty: courses same room, diff times, w/ proper studio in mix")
    void noPen_PropCoursesSameRoom() throws Exception{
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_MWF_7AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_8AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_9AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                2, MWF);

        Timeslot ts_MWF_7AM_2blcks_8AM_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_MWF_7AM_2blcks, bs_MWF_8AM_2blcks,
                MWF, MWF);
        Timeslot ts_MWF_8AM_2blcks_9AM_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_MWF_8AM_2blcks, bs_MWF_9AM_2blcks,
                MWF, MWF);

        Lesson combo1_st1 = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_7AM_2blcks_8AM_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson combo1_non_st1 = Lesson.test_buildLesson("2", 1, "non-studio", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_8AM_2blcks_9AM_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        //reasoning: proper studios use lab room for lec and lab/act portions, but non proper doesn't use it during lec
        constraintVerifier.verifyThat(TimetableConstraintProvider::labActRoomConflict)
                .given(combo1_st1, combo1_non_st1)
                .penalizesBy(NO_PENALTY);

    }

    @Test
    @DisplayName("Penalty: proper studio wrong hours")
    void penProp_wrongHours() throws Exception{
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_MWF_7AM_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                3, MWF);
        BitSet bs_MWF_8AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_9AM_3blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                3, MWF);

        Timeslot ts_MWF_7AM_3blcks_8AM_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_MWF_7AM_3blcks, bs_MWF_8AM_2blcks,
                MWF, MWF);
        Timeslot ts_MWF_8AM_2blcks_9AM_3blcks = Timeslot.test_lecLabBitAndDays(2, bs_MWF_8AM_2blcks, bs_MWF_9AM_3blcks,
                MWF, MWF);

        //Penalty wrong lecture hours
        Lesson combo1_st1 = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_7AM_3blcks_8AM_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        //Penalty wrong lab hours
        Lesson combo1_non_st1 = Lesson.test_buildLesson("2", 1, "non-studio", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_8AM_2blcks_9AM_3blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        //reasoning: proper studios use lab room for lec and lab/act portions, but non proper doesn't use it during lec
        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(combo1_st1, combo1_non_st1)
                .penalizesBy(2);
    }

    @Test
    @DisplayName("No penalty: proper studio right hours")
    void noPenProp_wrongHours() throws Exception{
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet bs_MWF_7AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("7:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_8AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("8:00AM", formatter),
                2, MWF);
        BitSet bs_MWF_9AM_2blcks = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", formatter),
                2, MWF);

        Timeslot ts_MWF_7AM_2blcks_8AM_2blcks = Timeslot.test_lecLabBitAndDays(1, bs_MWF_7AM_2blcks, bs_MWF_8AM_2blcks,
                MWF, MWF);
        Timeslot ts_MWF_8AM_2blcks_9AM_2blcks = Timeslot.test_lecLabBitAndDays(2, bs_MWF_8AM_2blcks, bs_MWF_9AM_2blcks,
                MWF, MWF);

        Lesson combo1_st1 = Lesson.test_buildLesson("1", 1, ConstraintTestHelper.DUMMY_STUDIO, "",
                "", "3-1-0", Constants.COURSE_ID_BIMAP.get(ConstraintTestHelper.DUMMY_STUDIO),
                ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_7AM_2blcks_8AM_2blcks, ConstraintTestHelper.DUMMY_ROOM);
        Lesson combo1_non_st1 = Lesson.test_buildLesson("2", 1, "non-studio", "",
                "", "3-1-0", 1, ConstraintTestHelper.DUMMY_TEACHER, ts_MWF_8AM_2blcks_9AM_2blcks,
                ConstraintTestHelper.DUMMY_ROOM);

        //reasoning: proper studios use lab room for lec and lab/act portions, but non proper doesn't use it during lec
        constraintVerifier.verifyThat(TimetableConstraintProvider::wrongHoursAmount)
                .given(combo1_st1, combo1_non_st1)
                .penalizesBy(NO_PENALTY);

    }
}
