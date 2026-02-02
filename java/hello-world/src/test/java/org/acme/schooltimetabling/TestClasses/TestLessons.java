package org.acme.schooltimetabling.TestClasses;

import com.google.common.collect.BiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.helperClasses.Generators.*;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.acme.schooltimetabling.helperClasses.ScheduleFormat;
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
        String curQuarterSurveyPath = String.format("input/%s-survey.csv", ScheduleConfig.getCurTerm());
        String prevQuarterSurveyPath = String.format("input/%s-survey.csv", ScheduleConfig.getPrevTerm());
        System.out.println("Reading the current quarter teacher survey");
        ArrayList<HashMap<String, String>> curQuarterSurveys = ParseInput.readCSV(curQuarterSurveyPath, newSurveyHeaders);
        System.out.println("Reading the previous quarter teacher survey");
        /*read the prev quarter survey*/
        ArrayList<HashMap<String, String>> prevQuarterSurveys  = ParseInput.readCSV(prevQuarterSurveyPath,newSurveyHeaders);
        /*teacher name -> teacher object*/
        HashMap<String, Teacher>teacherHashMap = TeacherGenerator.generateTeachers(curQuarterSurveys, prevQuarterSurveys);

        /*parse schedules*/
        List<ScheduleFormat> parsedSchedules = ParseInput.readScheduleClasses(
                String.format("input/schedule-%s-%s.json", ScheduleConfig.getCurTerm(),
                        ScheduleConfig.getDepartment()));

        /*Creating Lessons*/
        lessonList = LessonGenerator.generateLessons(parsedSchedules, teacherHashMap);
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

    //Note this test is currently not applicable using a different definition of a studio style course
    @Disabled
    @Test
    @DisplayName("Check studio courses")
    void checkStudio(){
        //set once the pair has been verified
        Set<Integer> linked = new HashSet<>();
        //holds lesson whose pair needs to be found
        Map<Integer, Lesson> findPair = new HashMap<>();
        assertAll("Loop checking all potential studio style courses",
                lessonList.stream()
                        .filter(lesson ->
                                Constants.STUDIO_STYLE_COURSES.contains(lesson.courseName.toLowerCase())
                        )
                        .map(lesson -> (Executable) () -> {
                            //checks for a specific lesson
                            assertAll("Checking studio course",
                                    //checks that studio courses have a link
                                    () -> assertNotNull(lesson.getLinker()),
                                    //check if lesson's link has a pair already
                                    () -> assertFalse(linked.contains(lesson.getLinker())),
                                    //check for pair
                                    () -> assertTrue(() -> {
                                        Lesson prev = findPair.getOrDefault(lesson.getLinker(), null);
                                        if(prev != null){
                                            //make sure the one lesson is lab/act and the other is the lecture
                                            if(prev.hasLecture == lesson.hasLecture
                                                    || prev.hasLabAct == lesson.hasLabAct) return false;
                                            else{
                                                //if pair has been validated add it to paired lesson verified
                                                linked.add(lesson.getLinker());
                                                return true;
                                            }
                                        }
                                        else{
                                            findPair.put(lesson.getLinker(), lesson);
                                            return true;
                                        }
                                    })

                            );
                }).toList());
    }

}
