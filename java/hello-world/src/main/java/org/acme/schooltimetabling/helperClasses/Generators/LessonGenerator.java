package org.acme.schooltimetabling.helperClasses.Generators;

import com.google.common.collect.BiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleFormat;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LessonGenerator extends Generator{
    private static final Logger LOGGER = LoggerFactory.getLogger(LessonGenerator.class);

    /**
     * <p>Generates all lessons and filter out any lesson types specified by the Constants.SKIP_SCHEDULE
     * variable given</p>
     *
     * @param courseConfigs HashMap of course names to their configurations
     * @param courseIdMapping a bimap of course names to their unique ID
     * @param schedules a list of ScheduleFormat objects containing courses that
     *                  will be taught by a teacher
     * @param teacherHashMap HashMap of teacher canon names to their teacher object
     * @return an ArrayList of all courses that a valid object could be made for
     */
    public static ArrayList<Lesson> generateLessons(HashMap<String, String> courseConfigs, BiMap<String, Integer> courseIdMapping,
                                                    List<ScheduleFormat> schedules, HashMap<String, Teacher> teacherHashMap){
        final int STARTING_SECTION_NUMBER = 1;
        final String CURRENT_TERM = ParseInput.scheduleConfig.curTerm;
        final String DEPARTMENT = ParseInput.scheduleConfig.department.toLowerCase();
        ArrayList<Lesson> lessons = new ArrayList<>();
        int lessonID = 1;
        String teacherName;

        Lesson newLesson;


        /*create a list of courses to section number and remove courses not in the department we
        * are scheduling*/
        HashMap<String, Integer> courseSectionCounter = new HashMap<>();
        for(String course: courseConfigs.keySet()){
            if(!course.contains(DEPARTMENT)){
                continue;
            }
            courseSectionCounter.put(course, STARTING_SECTION_NUMBER);
        }

        /*loop through the schedule (list of courses) a teacher is planned
        * to teach*/
        for(ScheduleFormat schedule: schedules){
            teacherName = schedule.getName();
            /*This is a list courses that will be scheduled*/
            List<String> coursesToSchedule;

            List<String> potentialCourses = getPotentialCourses(schedule, CURRENT_TERM);

            /*filter out the courses that aren't currently in the department we
            * want to schedule*/
            coursesToSchedule = potentialCourses.stream()
                    .filter(course -> course.contains(DEPARTMENT))
                    .collect(Collectors.toCollection(ArrayList::new));

            /*schedule selected courses*/
            for(String course: coursesToSchedule){
                newLesson = generateLesson(courseConfigs, courseIdMapping, teacherHashMap, courseSectionCounter,
                        course, teacherName, lessonID);

                /*If new lesson wasn't created for whatever reason skip modifying the following structures
                * and don't add it to the list of lessons*/
                if(newLesson == null){
                    continue;
                }

                lessonID++;

                lessons.add(newLesson);
            }
        }

        return lessons;
    }


    /**
     * <p>Extracts the list of courses that will be potentially scheduled</p>
     *
     * @param schedule ScheduleFormat object that contains instructor name and courses they will teach
     * @param CURRENT_TERM term to schedule for
     * @return List of potential courses to be scheduled
     */
    private static List<String> getPotentialCourses(ScheduleFormat schedule, String CURRENT_TERM) {
        if("fall".equalsIgnoreCase(CURRENT_TERM)){
            return schedule.getFall();
        }
        else if("winter".equalsIgnoreCase(CURRENT_TERM)){
            return schedule.getWinter();
        }
        else{
            return schedule.getSpring();
        }
    }


    /**
     * <p>Generates a new lesson for the course that a teacher will teach.</p>
     * <p>If the course has a modifier {@link Constants#SKIP_SCHEDULE Constants.SKIP_SCHEDULE} or configuration
     * {@link Constants#SKIP_CONFIGURATIONS Constants.SKIP_CONFIGURATIONS} contains it will not be scheduled</p>
     *
     * @param courseConfigs HashMap of a course name mapped to its course configuration
     * @param courseIdMapping BiMap of a course name mapped to its unique ID
     * @param teacherHashMap Hashmap of teacher's <i>canon name</i> mapped to its respective <i>Teacher</i> object
     * @param courseSectionCounter HashMap of a course name mapped to its next available section number
     * @param course name of the course whose lesson will be created for
     * @param teacherName name of the teacher who will teach the lesson
     * @param lessonID unique ID of the lesson
     * @return returns a new lesson to be scheduled or null if the lesson will be skipped
     */
    private static Lesson generateLesson(HashMap<String, String> courseConfigs, BiMap<String, Integer> courseIdMapping,
                                         HashMap<String, Teacher> teacherHashMap, HashMap<String, Integer> courseSectionCounter,
                                         String course, String teacherName, int lessonID){
        final String DUMMY_COURSE_MODIFIER = "";
        String[] courseInformation;
        String courseName;
        String courseModifier;
        String courseConfig;
        boolean hasLabOrAct;
        int sectionNumber;
        Teacher teacher;

        courseInformation = course.split("-");
        /*we check if the course has any modifiers
         * This is also used to check if we want to skip the course*/
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
                    LOGGER.warn(String.format("Skipping scheduling of course with name %s with " +
                            "modifier %s", courseName, courseModifier));
                }
                return null;
            }
        }

        courseConfig = courseConfigs.get(courseName);
        hasLabOrAct = determineLabOrAct(courseConfig);
        sectionNumber = courseSectionCounter.get(courseName);

        if(Constants.SKIP_CONFIGURATIONS.contains(courseConfig)){
            if(Constants.DEBUG){
                LOGGER.warn(String.format("Skipping course '%s' with configuration %s for %s", courseName, courseConfig
                        , teacherName));
            }
            return null;
        }

        /*We increase the section counter by two if it has a lab because a lesson consists of its lecture
         * and its lab/act and a lab/act section number is separate from its respective lecture section
         * number*/
        courseSectionCounter.replace(courseName, (hasLabOrAct ? sectionNumber + 2 : sectionNumber + 1) );

        /*checking if we can find the teacher; skip teacher if we can't
        * find their teacher object*/
        teacher = teacherHashMap.get(teacherName);
        if(teacher == null){
            if(Constants.DEBUG){
                LOGGER.warn(String.format("Couldn't find a teacher object for '%s';" +
                        "This might mean they don't have a survey. Skipping this teacher or canon mapping is wrong. " +
                        "Skipping course '%s'", teacherName, courseName));
            }
            return null;
        }

        /*create class*/
        return new Lesson(Integer.toString(lessonID), sectionNumber, courseName, teacherName,
                courseModifier, courseConfig, courseIdMapping.get(courseName),
                teacher);
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
        final int NO_UNITS = 0;
        String[] units = courseConfig.split("-");
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        return !(labUnits == NO_UNITS && actUnits == NO_UNITS);
    }
}
