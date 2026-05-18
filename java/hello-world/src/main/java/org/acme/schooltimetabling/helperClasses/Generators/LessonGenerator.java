package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.defaultTimes.DefaultTime;
import org.acme.schooltimetabling.defaultTimes.DefaultTimeRegistry;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.PrescheduleObject;
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
    public static boolean proper_studio_detected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(LessonGenerator.class);
    private static final PrescheduleObject PRESCHED_TIMES = ScheduleConfig.getPrescheduledFileName() != null ?
            ParseInput.readPrescheduledFile() : null;
    /**
     * Keeps track of the next available section number available for a course
     */
    private static final HashMap<String, Integer> COURSE_SECTION_COUNTER;
    /**
     * Holds the next available lesson ID. NOTE use the {@link #nxtLessonID()} function to retrieve the next
     * available lesson ID rather than using this attribute directly.
     */
    private static int lessonID = 1;
    private static List<Lesson> skippedLessons = new ArrayList<>();

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

            List<String> potentialCourses = getPotentialCourses(schedule, ScheduleConfig.getSeasonTerm().toLowerCase());

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
                    LOGGER.info(String.format("Creating a dummy course for the skipped course %s, to keep a record of it"
                            , courseName));
                    newLesson = Lesson.dummyRecord(courseName, courseModifier, getTeacher(teacherHashMap, teacherName));
                    skippedLessons.add(newLesson);
                }
                else{
                    newLesson = generateLesson(teacherHashMap, courseSectionCounter,
                            course, teacherName);
                    if(newLesson.isStudio()) proper_studio_detected = true;
                    lessons.add(newLesson);
                }
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
     * @return returns a new lesson to be scheduled
     * @see Constants#COURSE_CONFIGS
     * @see Constants#COURSE_ID_BIMAP
     */
    private static Lesson generateLesson(Map<String, Teacher> teacherHashMap, Map<String, Integer> courseSectionCounter,
            String course, String teacherName){
        String courseConfig;
        boolean hasLabOrAct;
        int sectionNumber;
        Teacher teacher;

        //first element = modifier; second element = course name
        Pair<String, String> courseDetails = getCourseDetails(course);
        String courseModifier = courseDetails.getFirst();
        String courseName = courseDetails.getSecond();
        courseConfig = Constants.COURSE_CONFIGS.get(courseName);


        //just in case we have some course config like "various"
        hasLabOrAct = determineLabOrAct(courseConfig);
        sectionNumber = courseSectionCounter.get(courseName);

        /*We increase the section counter by two if it has a lab because a lesson consists of its lecture
         * and its lab/act and a lab/act section number is separate from its respective lecture section
         * number*/
        courseSectionCounter.replace(courseName, (hasLabOrAct ? sectionNumber + 2 : sectionNumber + 1) );

        teacher = getTeacher(teacherHashMap, teacherName);

        /*create class*/
        return new Lesson(Integer.toString(nxtLessonID()), sectionNumber, courseName,
                courseModifier, courseConfig,  teacher);
    }




    /**
     * Get the teacher object associated for the given <i>teacher name</i>; if no associated object
     * exists, then one is created, using a default schedule from {@link DefaultTimeRegistry}
     *
     * @param teacherHashMap map with a canon name as a key and teacher object as a value
     * @param teacherName canon teacher name
     * @return teacher object for given teacher name
     * @see LessonGenerator#noSurveyTeacher(String, DefaultTime)
     */
    private static Teacher getTeacher(Map<String, Teacher> teacherHashMap, String teacherName){
        /*checking if we can find the teacher; skip teacher if we can't
         * find their teacher object*/
        Teacher teacher = teacherHashMap.get(teacherName);
        if(teacher == null){
            DefaultTime defaultTime = DefaultTimeRegistry.getRandomDefault();

            if(Constants.DEBUG){
                LOGGER.warn(String.format("Couldn't find a teacher object for '%s'. Most likely due to them not having" +
                        " a survey filled out or old noncanon-canon mapping is used;" +
                        "Creating one for them with now using the default time %s.", teacherName,
                        defaultTime.getClass().getSimpleName()));
            }

            //create teacher object
            teacher = noSurveyTeacher(teacherName, defaultTime);

            //add prescheduled times if possible
            if(PRESCHED_TIMES != null && PRESCHED_TIMES.getTeachers().containsKey(teacherName)){
                LOGGER.info(String.format("Found a prescheduled time for '%s'. Adding the time to their conflict bitset.",
                        teacherName));

                try {
                    BitSet addConflict = TeacherGenerator.createPreschedBs(PRESCHED_TIMES.getTeachers().get(teacherName));
                    teacher.getAcceptable().andNot(addConflict);
                    teacher.getPreferences().andNot(addConflict);
                    teacher.getConflict().or(addConflict);
                } catch (Exception e) {
                    LOGGER.info("Couldn't parse prescheduled times for '{}' because of error: '{}'",
                            teacherName, e.getMessage());
                }
            }

            teacherHashMap.put(teacherName, teacher);
        }

        return teacher;
    }




    /**
     * <p>Extracts the list of courses that will be potentially scheduled</p>
     *
     * @param schedule ScheduleFormat object that contains instructor name and courses they will teach
     * @param season season we are scheduling; i.e. fall, winter, spring
     * @return List of potential courses to be scheduled
     */
    private static List<String> getPotentialCourses(ScheduleFormat schedule, String season) {
        if("fall".equalsIgnoreCase(season)){
            return schedule.getFall();
        }
        else if("winter".equalsIgnoreCase(season)){
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

        if(!Constants.CHOSEN_CONFIGURATIONS.contains(config)){
            if(Constants.DEBUG){
                LOGGER.warn(String.format("Skipping course because it's not in the configurations that are being" +
                        " scheduled. Course info: name '%s', config '%s'", name, config));
            }
            return true;
        }

        return false;
    }


    /**
     * This function is used to create a teacher object during lesson creation if a teacher object can't be found
     * for the name. It will return a faculty object if the person is found to be a faculty member. The object returned
     * will have a copy of the preference, acceptable, and conflict {@link BitSet} as the {@link DefaultTime} prototype
     * object
     *
     * @param name name of teacher. Assumes it's in canon name format (i.e. &lt;last name&gt, &lt;rest of name&gt;)
     * @return a teacher object; or faculty if found to be a faculty member
     * @see Teacher
     * @see Faculty
     */
    private static Teacher noSurveyTeacher(String name, DefaultTime defaultTime){
        final int LAST_NAME_POS = 0;
        String[] nameFragments = name.split(",");
        boolean isFaculty = Constants.FACULTY_LAST_NAMES.contains(nameFragments[LAST_NAME_POS]);

        if(isFaculty){
            LOGGER.info(String.format("Found teacher '%s' to be a faculty member. Promoting to Faculty"
                    , name));
            return new Faculty(TeacherGenerator.getNextTeacherID(), name,
                    defaultTime.getPreference(),
                    defaultTime.getAcceptable(),
                    defaultTime.getConflict(),
                    Preference.NEUTRAL);
        }
        else{
            return new Teacher(TeacherGenerator.getNextTeacherID(), name,
                    defaultTime.getPreference(),
                    defaultTime.getAcceptable(),
                    defaultTime.getConflict(),
                    Preference.NEUTRAL);
        }
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


    public static List<Lesson> getSkippedLessons(){ return skippedLessons; }

}
