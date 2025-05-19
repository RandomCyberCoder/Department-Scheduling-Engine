package org.acme.schooltimetabling.helperClasses.Generators;

import com.google.common.collect.BiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleFormat;
import org.acme.schooltimetabling.helperClasses.Teacher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LessonGenerator extends Generator{
    /**
     * <p>Generates all lessons and filter out any lesson types specified by the Constants.SKIP_SCHEDULE
     * variable given</p>
     *
     * @param courseConfigs HashMap of course names to their configurations
     * @param courseIdMapping a bimap of course names to their unique ID
     * @param schedules a list of ScheduleFormat objects containing courses that
     *                  will be taught by a teacher
     * @param teacherHashMap HashMap of teacher canon names to their teacher object
     */
    public static ArrayList<Lesson> generateLessons(HashMap<String, String> courseConfigs, BiMap<String, Integer> courseIdMapping,
                                                    List<ScheduleFormat> schedules, HashMap<String, Teacher> teacherHashMap){
        final int STARTING_SECTION_NUMBER = 1;
        final String DUMMY_COURSE_MODIFIER = "";
        final String CURRENT_TERM = ParseInput.scheduleConfig.curTerm;
        final String DEPARTMENT = ParseInput.scheduleConfig.department.toLowerCase();
        ArrayList<Lesson> lessons = new ArrayList<>();
        int lessonID = 1;
        String[] courseInformation;
        String courseName;
        String courseModifier;
        String courseConfig;
        boolean hasLabOrAct;
        String teacherName;
        int sectionNumber;


        /*create a list of courses to section number*/
        HashMap<String, Integer> courseSectionCounter = new HashMap<>();
        for(String course: courseConfigs.keySet()){
            if(!course.contains(DEPARTMENT)){
                continue;
            }
            courseSectionCounter.put(course, STARTING_SECTION_NUMBER);
        }

        /*loop through what schedule (list of courses) a teacher is planned
        * to teach*/
        for(ScheduleFormat schedule: schedules){
            teacherName = schedule.getName();
            /*This is a list courses that will be scheduled*/
            List<String> coursesToSchedule;

            /*some instructors might be in multiple departments, so we may not want
            * to make sure we only schedule courses for the department we are only concerned
            * about */
            List<String> potentialCourses = new ArrayList<>();
            if("fall".equals(CURRENT_TERM)){
                potentialCourses = schedule.getFall();
            }
            else if("winter".equals(CURRENT_TERM)){
                potentialCourses = schedule.getWinter();
            }
            else{
                potentialCourses = schedule.getSpring();
            }

            /*filter out the courses that aren't currently in the department we
            * want to schedule*/
            coursesToSchedule = potentialCourses.stream()
                    .filter(course -> course.contains(DEPARTMENT)).
                    collect(Collectors.toCollection(ArrayList::new));

            /*TODO extract this logic of creating a single lesson in a function like other object generation functions
            *  to a function that will generate just the objectd
            *  Not actually sure if this is possible because this would require that the function signatures to be
            *  the same but this might not be possible
            *  or maybe we can but the parent class won't be abstract */
            /*once list has been made schedule */
            for(String course: coursesToSchedule){
                /*we check if the course has any modifiers
                * This is also used to check if we want to skip the course*/
                courseInformation = course.split("-");
                if(courseInformation.length == 1){
                    courseName = courseInformation[0];
                    courseModifier = DUMMY_COURSE_MODIFIER;
                }
                else{
                    courseName = courseInformation[1];
                    courseModifier = courseInformation[0];
                    courseModifier = Constants.SPECIAL_CODE_CONVERSION.get(courseModifier);


                    /*we found a modifier so check if we want to schedule it
                    * or do anything special*/
                    if(Constants.SKIP_SCHEDULE.contains(courseModifier)){
                        if(Constants.DEBUG){
                            System.out.printf("Skipping scheduling of course with name %s with " +
                                    "modifier %s\n", courseName, courseModifier);
                        }
                        continue;
                    }
                }

                courseConfig = courseConfigs.get(courseName);
                hasLabOrAct = determineLabOrAct(courseConfig);
                sectionNumber = courseSectionCounter.get(courseName);

                if(Constants.SKIP_CONFIGURATIONS.contains(courseConfig)){
                    if(Constants.DEBUG){
                        System.out.printf("Skipping course '%s' with configuration %s\n", courseName, courseConfig);
                    }
                    continue;
                }

                /*We increase the section counter by two if it has a lab because a lesson consists of it lecture
                 * and its lab/act and a lab/act section number is separate from its respective lecture section
                 * number*/
                courseSectionCounter.replace(courseName, (hasLabOrAct ? sectionNumber + 2 : sectionNumber + 1) );

                /*create class*/
                Lesson newLesson = new Lesson(Integer.toString(lessonID++), sectionNumber, courseName, teacherName,
                        courseModifier, courseConfig, courseIdMapping.get(courseName),
                        teacherHashMap.get(teacherName));
                lessons.add(newLesson);
            }


        }

        return lessons;
    }

    /**
     * <p>The function determines if the giving course configuration has
     * a lab/activity</p>
     *
     * @param courseConfig a course configuration in format E-L-A where
     *                     E = lecture units, L = lab units, and
     *                     A = activity units
     * @return returns true if the course configuration contains a lab
     * or activity
     */
    private static boolean determineLabOrAct(String courseConfig){
        System.out.printf("parsing course config %s\n", courseConfig);
        final int NO_UNITS = 0;
        String[] units = courseConfig.split("-");
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        return !(labUnits == NO_UNITS && actUnits == NO_UNITS);
    }
}
