package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.acme.schooltimetabling.helperClasses.ScheduleFormat;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class LessonGenerator extends Generator{
    public static boolean OLD_studio_detected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(LessonGenerator.class);
    /**
     * Keeps track of the next available section number available for a course
     */
    private static final HashMap<String, Integer> COURSE_SECTION_COUNTER;
    /**
     * Holds the next available lesson ID. NOTE use the {@link #nxtLessonID()} function to retrieve the next
     * available lesson ID rather than using this attribute directly.
     */
    private static int lessonID = 1;
    private static int availableLinkerID = 1;

    static {
        final String DEPARTMENT = ScheduleConfig.getDepartment().toLowerCase();
        final int STARTING_SECTION_NUMBER = 1;

        COURSE_SECTION_COUNTER = new HashMap<>();
        for(String course: Constants.COURSE_CONFIGS.keySet()){
            if(!course.contains(DEPARTMENT)){
                continue;
            }
            COURSE_SECTION_COUNTER.put(course, STARTING_SECTION_NUMBER);
        }
    }




    /**
     * <p>Generates all lessons and filter out any lesson types specified by the Constants.SKIP_SCHEDULE
     * variable given</p>
     *
     * @param schedules a list of ScheduleFormat objects containing courses that
     *                  will be taught by a teacher
     * @param teacherHashMap HashMap of teacher canon names to their teacher object
     * @return an ArrayList of all courses that a valid object could be made for
     */
    public static ArrayList<Lesson> generateLessons(List<ScheduleFormat> schedules, Map<String, Teacher> teacherHashMap) {
        final int STARTING_SECTION_NUMBER = 1;
        final String CURRENT_TERM = ScheduleConfig.getCurTerm();
        final String DEPARTMENT = ScheduleConfig.getDepartment().toLowerCase();
        ArrayList<Lesson> lessons = new ArrayList<>();
        String teacherName;

        Lesson newLesson;


        /*create a list of courses to section number and remove courses not in the department we
         * are scheduling*/
        HashMap<String, Integer> courseSectionCounter = new HashMap<>();
        for (String course : Constants.COURSE_CONFIGS.keySet()) {
            if (course.contains(DEPARTMENT)) {
                courseSectionCounter.put(course, STARTING_SECTION_NUMBER);
            }
        }

        /*loop through the schedule (list of courses) a teacher is planned
         * to teach*/
        for (ScheduleFormat schedule : schedules) {
            //TODO just make the tacher object and pass that instead of the teacher name
            teacherName = schedule.getName();
            /*This is a list courses that will be scheduled*/
            List<String> coursesToSchedule;

            List<String> potentialCourses = getPotentialCourses(schedule, CURRENT_TERM);

            /*filter out the courses that aren't currently in the department we
             * want to schedule*/
            coursesToSchedule = potentialCourses.stream()
                    .filter(course -> course.contains(DEPARTMENT))
                    .collect(Collectors.toCollection(ArrayList::new));

            /*schedule selected courses for each teacher*/
            for (String course : coursesToSchedule) {
                Pair<String, String> parsedCourse = getCourseDetails(course);
                String courseModifier = parsedCourse.getFirst();
                String courseName = parsedCourse.getSecond();
                String courseConfig = Constants.COURSE_CONFIGS.get(courseName);

                /*check if the course is marked for scheduling*/
                if(skipCourse(courseModifier, courseName, courseConfig)){
                    continue;
                }

                newLesson = generateLesson(teacherHashMap, courseSectionCounter,
                        course, teacherName);
                if(newLesson != null) lessons.add(newLesson);
            }
        }

        return lessons;
    }




    /**
     * <p>Generates a new lesson for the course that a teacher will teach.</p>
     * <p>If the course has a modifier {@link Constants#SKIP_SCHEDULE Constants.SKIP_SCHEDULE} or configuration
     * {@link Constants#SKIP_CONFIGURATIONS Constants.SKIP_CONFIGURATIONS} contains it will not be scheduled</p>
     *
     * @param teacherHashMap Hashmap of teacher's <i>canon name</i> mapped to its respective <i>Teacher</i> object
     * @param courseSectionCounter HashMap of a course name mapped to its next available section number
     * @param course name of the course whose lesson will be created for
     * @param teacherName name of the teacher who will teach the lesson
     * @return returns a new lesson to be scheduled or null if the lesson will be skipped
     * @see Constants#COURSE_CONFIGS
     * @see Constants#COURSE_ID_BIMAP
     */
    private static Lesson generateLesson(Map<String, Teacher> teacherHashMap, Map<String, Integer> courseSectionCounter,
            String course, String teacherName){
        final Integer NO_LESSON_LINKER = null;
        String courseConfig;
        boolean hasLabOrAct;
        int sectionNumber;
        Teacher teacher;

        //first element = modifier; second element = course name
        Pair<String, String> courseDetails = getCourseDetails(course);
        String courseModifier = courseDetails.getFirst();
        String courseName = courseDetails.getSecond();
        courseConfig = Constants.COURSE_CONFIGS.get(courseName);

        //check if the course should be scheduled; if not return null
        if(skipCourse(courseModifier, courseName, courseConfig)){
            return null;
        }

        hasLabOrAct = determineLabOrAct(courseConfig);
        sectionNumber = courseSectionCounter.get(courseName);

        /*We increase the section counter by two if it has a lab because a lesson consists of its lecture
         * and its lab/act and a lab/act section number is separate from its respective lecture section
         * number*/
        courseSectionCounter.replace(courseName, (hasLabOrAct ? sectionNumber + 2 : sectionNumber + 1) );

        teacher = getTeacher(teacherHashMap, teacherName);

        /*create class*/
        return new Lesson(Integer.toString(nxtLessonID()), sectionNumber, courseName,
                courseModifier, courseConfig,  teacher, NO_LESSON_LINKER);
    }




    /**
     * Get the teacher object associated for the given <i>teacher name</i>; if no associated object
     * exists, then one is created, but with no conflict, preference, or acceptable times
     *
     * @param teacherHashMap map with a canon name as a key and teacher object as a value
     * @param teacherName canon teacher name
     * @return teacher object for given teacher name
     * @see LessonGenerator#noSurveyTeacher(String)
     */
    private static Teacher getTeacher(Map<String, Teacher> teacherHashMap, String teacherName){
        /*checking if we can find the teacher; skip teacher if we can't
         * find their teacher object*/
        Teacher teacher = teacherHashMap.get(teacherName);
        if(teacher == null){
            if(Constants.DEBUG){
                LOGGER.warn(String.format("Couldn't find a teacher object for '%s';" +
                        "Creating one for them with now.", teacherName));
            }

            //create teacher object
            teacher = noSurveyTeacher(teacherName);

            teacherHashMap.put(teacherName, teacher);
        }

        return teacher;
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
     * Parses a course string in the format specified for the course param into it's modifier and course name
     * @param course a course string in the format '&lt;modifier&gt;-&lt;course name&gt;' or '&lt;course name&gt;'
     * @return a Pair where the first element is the course modifier (empty string in no modifier was given); the
     * second element is the course name
     * @see Constants#SPECIAL_CODE_CONVERSION
     */
    private static Pair<String, String> getCourseDetails(String course){
        final String DUMMY_COURSE_MODIFIER = "";
        String courseName;
        String courseModifier;

        String[] courseInformation = course.split("-");

        /*we check if the course has any modifiers*/
        if(courseInformation.length == 1){
            courseName = courseInformation[0];
            courseModifier = DUMMY_COURSE_MODIFIER;
        }
        else{
            courseName = courseInformation[1];
            courseModifier = courseInformation[0];
            courseModifier = Constants.SPECIAL_CODE_CONVERSION.get(courseModifier);
        }

        return new Pair<>(courseModifier, courseName);
    }




    /**
     * Checks if the course should be scheduled base on its configuration and modifier.
     *
     * @param modifier course modifier; empty string if none
     * @param name course name
     * @param config course configuration
     * @return Ture if the course with given modifier should be scheduled; False otherwise
     * @see Constants#SKIP_CONFIGURATIONS
     * @see Constants#SKIP_SCHEDULE
     */
    private static boolean skipCourse(String modifier, String name, String config){
        /*we found a modifier so check if we want to schedule it
         * or do anything special*/
        if(Constants.SKIP_SCHEDULE.contains(modifier)){
            if(Constants.DEBUG){
                LOGGER.warn(String.format("Skipping course scheduling due to modifier: name '%s',  " +
                        "modifier '%s'", name, modifier));
            }
            return true;
        }

        if(Constants.SKIP_CONFIGURATIONS.contains(config)){
            if(Constants.DEBUG){
                LOGGER.warn(String.format("Skipping course due to configuration: name '%s', configuration '%s'"
                        , name, config));
            }
            return true;
        }

        return false;
    }




    /** NOTE this not how true studios should be handled
     * Studio style helper to create special lessons for the studio style courses
     * @param name name of the course (i.e. csc457)
     * @param modifier course modifier; if none present use them empty string
     * @param config configuration to use for the split
     * @param teacher instructor teaching the course
     * @return a studio style course split into a lecture and either a lab or activity lesson; First element is the
     *  lecture lesson
     */
    private static Pair<Lesson, Lesson> studioHelper(String name, String modifier, String config, Teacher teacher){
        //leaving out while proper studio implementation
        if(true) throw new UnsupportedOperationException("This is not what a proper studio is. This implementation actually" +
                " is a nice to have.");
        /* units lecture-lab-activity */
        final int LECTURE = 0;
        final int LAB = 1;
        final int ACT = 2;
        String[] configParsed = config.split("-");
        int lecUnits = Integer.parseInt(configParsed[LECTURE]);
        int labUnits = Integer.parseInt(configParsed[LAB]);
        int actUnits = Integer.parseInt(configParsed[ACT]);

        String newConfig;
        int idToUse;
        int sectionNumber;

        //debug comments
        if(lecUnits == 0) {
            LOGGER.error(String.format("studio style course '%s' has no lecture; implement logic for this", name));
            return null;
        }
        if(labUnits == 0 && actUnits == 0){
            LOGGER.error(String.format("studio style course '%s' has no lab or activity; implement logic for this. " +
                    "I don't think this is possible though", name));
            return null;
        }

        /*TODO: I could have sworn I saw a studio style course that had no lecture. If this is possible,
         *  then we will make a course with only a lecture or activity sort of like a normal course but force
         *  studio space to be all on the same day continuously*/
        /*Assuming studio style courses have a lecture and either a lab or activity*/
        /*create a lesson for the lecture portion*/
        final int LINKER_ID = nxtLinkerID();
        newConfig = String.format("%d-0-0", lecUnits);
        sectionNumber = COURSE_SECTION_COUNTER.get(name);
        COURSE_SECTION_COUNTER.replace(name, sectionNumber + 1);
        Lesson lecLesson = new Lesson(Integer.toString(nxtLessonID()), sectionNumber, name
                , modifier, newConfig, teacher, LINKER_ID);


        /*create a lesson for the lab or activity portion of the course*/
        if(labUnits > 0){
            newConfig = String.format("0-%d-0", labUnits);
        }
        else{
            newConfig = String.format("0-0-%d", actUnits);
        }
        sectionNumber = COURSE_SECTION_COUNTER.get(name);
        COURSE_SECTION_COUNTER.replace(name, sectionNumber + 1);
        Lesson labActLesson = new Lesson(Integer.toString(nxtLessonID()), sectionNumber, name
                , modifier, newConfig, teacher, LINKER_ID);

        return new Pair<>(lecLesson, labActLesson);
    }




    /**
     * This function is used to create a teacher object during lesson creation if a teacher object can't be found
     * for the name. It will return a faculty object if the person is found to be a faculty member. Note that the object
     * returned will have empty conflict, preferences, and acceptable BitSets.
     *
     * @param name name of teacher. Assumes it's in canon name format (i.e. &lt;last name&gt, &lt;rest of name&gt;)
     * @return a teacher object; or faculty if found to be a faculty member
     * @see Teacher
     * @see Faculty
     */
    private static Teacher noSurveyTeacher(String name){
        final int LAST_NAME_POS = 0;
        String[] nameFragments = name.split(",");

        if(Constants.FACULTY_LAST_NAMES.contains(nameFragments[LAST_NAME_POS])){
            LOGGER.info(String.format("Found teacher '%s' to be a faculty member. Promoting Teacher obj to Faculty"
                    , name));
            return new Faculty(TeacherGenerator.getNextTeacherID(), name, new BitSet(), new BitSet(), new BitSet());
        }

        return new Teacher(TeacherGenerator.getNextTeacherID(), name, new BitSet(), new BitSet(), new BitSet());
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




    /**
     * helper function to ensure lessonID is always incremented when retrieving the next available lesson ID
     * @return next available lesson ID
     */
    private static int nxtLessonID(){
        return lessonID++;
    }




    /**
     * Helper function to ensure the {@link #availableLinkerID} is updated when the current linker ID is used.
     * Note that only one ID should be used per a pair of lecture and lab/act.
     * @return give the next available linker
     */
    private static int nxtLinkerID(){
            return availableLinkerID++;
        }
}
