package org.acme.schooltimetabling.TestClasses;

import com.google.common.collect.BiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.helperClasses.Generators.*;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleFormat;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import java.util.*;

import static org.acme.schooltimetabling.TimetableApp.remapName;
import static org.junit.jupiter.api.Assertions.*;

public class TestLessons {
    static ArrayList<Lesson> lessonList;
    static BiMap<String, Integer> courseIdMapping;
    static HashMap<String, String> courseConfigs;
    @BeforeAll
    static void setUp() throws Exception{
        /*New headers for the survey*/
        ArrayList<String> newSurveyHeaders = new ArrayList<>(
                Arrays.asList("id", "start", "complete", "email", "name", "use_old",
                        "7 AM","8 AM","9 AM","10 AM","11 AM","12 PM","1 PM","2 PM",
                        "3 PM","4 PM","5 PM","6 PM","7 PM","8 PM","9 PM","7 AM2",
                        "8 AM2","9 AM2","10 AM2","11 AM2","12 PM2","1 PM2","2 PM2",
                        "3 PM2","4 PM2","5 PM2","6 PM2","7 PM2","8 PM2","9 PM2",
                        "mwf_1", "tr_1", "mwf_2", "mwf_tr",
                        "tr_2", "mwf_3","mwf_2_tr_1", "mwf_1_tr_2",
                        "tr_3", "mwrf", "mtwr", "mw", "tr",
                        "back_to_back", "gap", "constraint", "require",
                        "pref", "comment", "stars")
        );



        /*read the current quarter survey
         * and then create Teacher objects*/
        String curQuarterSurveyPath = "java/hello-world/src/main/java/org/acme/schooltimetabling/input/2254-survey.csv";
        String prevQuarterSurveyPath = "java/hello-world/src/main/java/org/acme/schooltimetabling/input/2252-survey.csv";
        System.out.println("Reading the current quarter teacher survey");
        ArrayList<HashMap<String, String>> curQuarterSurveys = ParseInput.readCSV(curQuarterSurveyPath, newSurveyHeaders);
        System.out.println("Reading the previous quarter teacher survey");
        /*read the prev quarter survey*/
        ArrayList<HashMap<String, String>> prevQuarterSurveys  = ParseInput.readCSV(prevQuarterSurveyPath,newSurveyHeaders);
        /*teacher name -> teacher object*/
        HashMap<String, Teacher>teacherHashMap = TeacherGenerator.generateTeachers(curQuarterSurveys, prevQuarterSurveys);

        /*parse schedules*/
        List<ScheduleFormat> parsedSchedules = ParseInput.readScheduleClasses("input/schedule-2254-CSC.json");

        /*create mapping of all the possible names a teacher has to their cannon name*/
        Generator.createTeacherNameMapping(teacherHashMap, parsedSchedules);

        /*redo mapping of teacherHashMap to use canon names instead.
         *  Needed for when we create the Lessons... what a headache*/
        teacherHashMap = remapName(teacherHashMap);



        /*Creating Lessons*/
        courseConfigs = ParseInput.readCourseConfigs("constants/configurations.tsv");
        courseIdMapping = Generator.genCourseToIdMapping(courseConfigs.keySet().iterator());
        lessonList = LessonGenerator.generateLessons(courseConfigs, courseIdMapping, parsedSchedules, teacherHashMap);
    }

    @Test
    @DisplayName("Checking course IDs")
    void checkCourseID(){
        assertAll("checking course IDs",
                lessonList.stream().map(lesson -> (Executable) () -> {
                    assertEquals(courseIdMapping.get(lesson.courseName), lesson.courseID);
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
                            () -> assertFalse(Constants.SKIP_CONFIGURATIONS.contains(courseConfigs.get(lesson.courseName))),
                            () -> assertFalse(Constants.SKIP_SCHEDULE.contains(lesson.modifiers)));
                }));
    }

    @Test
    @DisplayName("Check correct teacher object")
    void checkCorrectTeacher(){
        assertAll("Does it have the correct teacher",
                lessonList.stream().map(lesson -> (Executable) () -> {
                    assertEquals(lesson.teacherName, Constants.TEACHER_NAME_TO_CANON.get(lesson.teacherObj.name));
                }));
    }

}
