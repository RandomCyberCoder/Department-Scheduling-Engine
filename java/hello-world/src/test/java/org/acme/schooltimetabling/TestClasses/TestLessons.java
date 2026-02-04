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
