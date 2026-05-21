package org.acme.schooltimetabling.TestClasses;

import com.google.common.collect.BiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.helperClasses.Generators.*;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.acme.schooltimetabling.fileObjects.ScheduleFormat;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestLessons {
    static ArrayList<Lesson> lessonList;
    static BiMap<String, Integer> courseIdMapping;
    static HashMap<String, String> courseConfigs;
    @BeforeAll
    static void setUp() throws Exception{
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        Map<String, Teacher> teacherMap = TeacherGenerator.teacherGenDriver();
        System.out.println(String.format("%s, %s, %s, %s\n", ScheduleConfig.getDepartment(),
                ScheduleConfig.getCurTerm(), ScheduleConfig.getPrevTerm(),
                ScheduleConfig.getSeasonTerm()));

        /*parse schedules*/
        List<ScheduleFormat> parsedSchedules = ParseInput.readScheduleClasses(String.format("input/schedule-%s-%s.json"
                , ScheduleConfig.getCurTerm(), ScheduleConfig.getDepartment()));

        /*Creating Lessons*/
        lessonList = LessonGenerator.generateLessons(parsedSchedules, teacherMap);
    }

    @Test
    @DisplayName("Checking course IDs")
    void checkCourseID(){
        assertAll("checking course IDs",
                lessonList.stream().map(lesson -> (Executable) () -> {
                    assertEquals(Constants.COURSE_ID_BIMAP.get(lesson.courseName), lesson.courseID);
                }));
    }

    @Test
    @DisplayName("Making sure lessons have a teach object")
    void checkTeachers(){
        assertAll("Check for teach existence",
                lessonList.stream().map(lesson -> (Executable) () -> {
                    assertNotNull(lesson.teacherObj);
                }));

    }
    @Test
    @DisplayName("Check right classes are scheduled")
    void checkScheduledClasses(){
        assertAll("Checking Lessons to make sure they are skipped",
                lessonList.stream().map(lesson -> (Executable) () -> {
                    assertAll("",
                            () -> assertFalse(Constants.SKIP_CONFIGURATIONS.contains(Constants.COURSE_CONFIGS.get(lesson.courseName))),
                            () -> assertFalse(Constants.SKIP_SCHEDULE.contains(lesson.modifiers)));
                }));
    }

}
