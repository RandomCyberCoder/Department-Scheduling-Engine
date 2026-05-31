package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.builders.lessons.LessonBuilder;
import org.acme.schooltimetabling.builders.teachers.TeacherBuilder;
import org.acme.schooltimetabling.builders.teachers.policies.DefaultTeachingPolicy;
import org.acme.schooltimetabling.builders.teachers.policies.FacultyPolicy;
import org.acme.schooltimetabling.defaultTimes.DefaultTime;
import org.acme.schooltimetabling.defaultTimes.DefaultTimeRegistry;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.fileObjects.*;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.acme.schooltimetabling.helperClasses.ParseInput.readCoursePatterns;

public class LessonGenerator extends Generator{
    public static boolean OLD_studio_detected = false;
    public static boolean proper_studio_detected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(LessonGenerator.class);
    private static final PrescheduleObject PRESCHED_TIMES = ScheduleConfig.getPrescheduledFileName() != null ?
            ParseInput.readPrescheduledFile() : null;
    private static final CoursePatterns COURSE_PATTERNS;
    /**
     * Keeps track of the next available section number available for a course
     */
    private static final HashMap<String, Integer> COURSE_SECTION_COUNTER;
    /**
     * Holds the next available lesson ID. NOTE use the {@link #nextLessonID()} function to retrieve the next
     * available lesson ID rather than using this attribute directly.
     */
    private static int lessonID = 1;
    private static List<Lesson> skippedLessons = new ArrayList<>();

    static {
        final String DEPARTMENT = ScheduleConfig.getDepartment().toLowerCase();
        final int STARTING_SECTION_NUMBER = 1;

        COURSE_SECTION_COUNTER = new HashMap<>();
        for(String course: Constants.COURSE_CONFIGS.keySet()){
            if(course.toLowerCase().contains(DEPARTMENT)){
                COURSE_SECTION_COUNTER.put(course, STARTING_SECTION_NUMBER);
            }
        }

        //read in course patterns if they exist; make an empty object if no file name was given
        String patternsFileName = ScheduleConfig.getPatternsFileName();

        if (patternsFileName == null) {
            COURSE_PATTERNS = CoursePatterns.empty();
        } else {
            String filePath = "input/" + patternsFileName.strip();

            try (InputStream inputStream = ParseInput.getResourceAsStream(filePath)) {
                if (inputStream == null) {
                    COURSE_PATTERNS = CoursePatterns.empty();
                } else {
                    COURSE_PATTERNS = readCoursePatterns(inputStream);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load course patterns from " + filePath, e);
            }
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

        /*loop through the schedule (list of courses) a teacher is planned
         * to teach*/
        for (ScheduleFormat schedule : schedules) {
            teacherName = schedule.getName();
            Teacher teacher = getTeacher(teacherHashMap, teacherName);
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

                if(courseConfig == null) throw new RuntimeException(String.format("Unable to find a configuration for " +
                        "the course '%s'", courseName));

                //if we have a remote course we need to set it here

                /*check if the course is marked for scheduling*/
                if(skipCourse(courseModifier, courseName, courseConfig)){
                    LOGGER.info(String.format("Creating a dummy course for the skipped course %s, to keep a record of it"
                            , courseName));
                    newLesson = Lesson.dummyRecord(courseName, courseModifier, getTeacher(teacherHashMap, teacherName));
                    skippedLessons.add(newLesson);
                }
                else{
                    //check if we have to split
                    LabPatterns defaultPattern = getLabPattern(courseName, teacherName);
                    if(LabPatterns.ONE.equals(defaultPattern)){
                        //if we are in this code block, the lesson must have a lab
                        //get the parsed config
                        int[] courseUnits = parseConfig(courseName);
                        final int LEC_POS = 0;
                        final int LAB_POS = 1;
                        final int ACT_POS = 2;

                        //check if this course has a lecture portion
                        if(courseUnits[LEC_POS] != 0){
                            lessons.add(
                                    generateLesson(teacher, course,
                                            String.format("%d-0-0", courseUnits[LEC_POS]),
                                            null)
                            );
                        }
                        /*TODO make sure we can make a lab only lesson.
                           checks below:
                           -the lesson geneorator take into account the section numbers well
                           -Does the lesson object take this into account well? done
                           - DO THIS BEFORE THE BELOW: Update the timeslots
                           +check over constraints
                           +how does the above effect print out? It doesn't I already check before printing if the
                           lesson has lecture or lab. ACUTALLY update the timeslots first. This will determine how
                           the print out and the constraints will have to be updated
                           */
                        lessons.add(
                                generateLesson(teacher, course,
                                        String.format("0-%d-%d", courseUnits[LAB_POS], courseUnits[ACT_POS]),
                                        LabPatterns.ONE)
                        );
                    }
                    //if not we do the below
                    else{
                        newLesson = generateLesson(teacher, course);
                        if(newLesson.isStudio()) proper_studio_detected = true;
                        lessons.add(newLesson);
                    }
                }
            }
        }

        return lessons;
    }


    /**
     * <p>Generates a new lesson for the course that a teacher will teach.</p>
     *
     * @param teacher Object for teacher who will be scheduled
     * @param course course slug for the lesson that will be created; expected format is <i>modifier-courseName</i> or
     *               <i>courseName</i>
     * @return returns a new lesson to be scheduled
     * @see Constants#COURSE_CONFIGS
     * @see Constants#COURSE_ID_BIMAP
     */
    private static Lesson generateLesson(Teacher teacher, String course){
        //first element = modifier; second element = course name
        Pair<String, String> courseDetails = getCourseDetails(course);
        String courseModifier = courseDetails.getFirst();
        String courseName = courseDetails.getSecond();
        String courseConfig = Constants.COURSE_CONFIGS.get(courseName);

        if(courseConfig == null) throw new IllegalArgumentException(String.format("Couldn't find a section number for the " +
                "course '%s'. Likely due to it having no configuration. Check the your configuration file.", courseName));

        return new LessonBuilder()
                .id(nextLessonID())
                .section(nextSectionNum(courseName, courseConfig))
                .courseName(courseName)
                .courseConfig(courseConfig)
                .modifier(courseModifier)
                .teacherObj(teacher)
                .labPattern(getLabPattern(courseName, courseModifier))
                .build();
    }

    /**
     * used only for when we split a course into two lesson objects
     */
    private static Lesson generateLesson(Teacher teacher, String course, String config, LabPatterns labPattern){
       //first element = modifier; second element = course name
        Pair<String, String> courseDetails = getCourseDetails(course);
        String courseModifier = courseDetails.getFirst();
        String courseName = courseDetails.getSecond();

        return new LessonBuilder()
                .id(nextLessonID())
                .section(nextSectionNum(courseName, config))
                .courseName(courseName)
                .courseConfig(config)
                .modifier(courseModifier)
                .teacherObj(teacher)
                .labPattern(labPattern)
                .build();
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
        boolean isFaculty = Constants.FACULTY_LAST_NAMES.contains(nameFragments[LAST_NAME_POS].strip());
        TeacherBuilder builder = new TeacherBuilder(new DefaultTeachingPolicy());
        //TODO; CHECK UPDATE
        if(isFaculty){
            LOGGER.info(String.format("Found teacher '%s' to be a faculty member. Promoting to Faculty"
                    , name));
            builder.setPolicy(new FacultyPolicy());
        }

        return builder.preference(defaultTime.getPreference())
                .acceptable(defaultTime.getAcceptable())
                .conflict(defaultTime.getConflict())
                .canon(name)
                .gapPref(Preference.NEUTRAL)
                .build();
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
    public static int nextLessonID(){
        return lessonID++;
    }

    /** This method requires the config to determine if the course will need a section for a lecture,
     * lab/act, or both. This function should be used for pulling section numbers. In the case that the
     * configuration has both a lecture and lab, the section number returned will be for the lesson and the
     * section number after the one returned is reserved for the lab/act portion of the course.
     */
    public static int nextSectionNum(String course, String courseConfig){
        if(!COURSE_SECTION_COUNTER.containsKey(course)) throw new IllegalArgumentException(
                String.format("Couldn't find a section number for the course '%s'. Likely due to it having no " +
                        "configuration. Check the your configuration file.", course));

        int sectionNumber = COURSE_SECTION_COUNTER.get(course);

        //update the section number accordingly
        String[] units = courseConfig.split("-");
        int lecUnits = Integer.parseInt(units[0]);
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        int sectionIncrement = 0;
        if(lecUnits != 0) sectionIncrement++;
        if(labUnits != 0 || actUnits != 0) sectionIncrement++;
        COURSE_SECTION_COUNTER.put(course, sectionNumber + sectionIncrement);

        return sectionNumber;
    }

    /**
     * If no pattern for a lab has been found then we will default to the {@link LabPatterns#MULTIPLE} pattern.
     * Three different possibilities will give considered: no pattern default, global default, teacher preference. With
     * <i>teacher preference</i> having the highest authority and <i>no pattern default</i> having the lowest authority.
     *
     * @param course Name of the course whose labPattern we want
     * @param name Canon name of the teacher
     * @return Pattern we want for the lab; <i>null</i> if the course has no lab
     */
    public static LabPatterns getLabPattern(String course, String name){
        final int LEC_POS = 0;
        final int LAB_POS = 1;
        final int ACT_POS = 2;
        int[] parsedConfig = parseConfig(course);

        //if only lecture return null;
        if(parsedConfig[LAB_POS] == 0 && parsedConfig[ACT_POS] == 0) return null;

        LabPatterns global = COURSE_PATTERNS.getCourseDefault(course);
        LabPatterns specific = COURSE_PATTERNS.getTeacherPref(course, name);

        //takes precedence
        if (specific != null) {
            return specific;
        }
        //fall back to global if the teacher has no preference
        if (global != null) {
            return global;
        }

        //if no lab pattern was found we default to the following
        return LabPatterns.MULTIPLE;
    }


    /**
     *
     * @param course name of the course whose configuration will be parsed
     * @return parsed config with the element order being: LECTURE, LAB, ACTIVITY.
     */
    public static int[] parseConfig(String course) {
        final int PARTITIONS = 3;
        String courseConfig = Constants.COURSE_CONFIGS.get(course);

        if (courseConfig == null) {
            throw new IllegalArgumentException(String.format(
                    "For the course '%s', no configuration was found",
                    course
            ));
        }

        String[] parts = courseConfig.strip().split("-");

        if (parts.length != PARTITIONS) {
            throw new IllegalArgumentException(String.format(
                    "For the course '%s', a configuration in the form d-d-d must be present, where d is a nonnegative integer.",
                    course
            ));
        }

        int[] parsedConfig = new int[PARTITIONS];

        for (int i = 0; i < PARTITIONS; i++) {
            try {
                parsedConfig[i] = Integer.parseInt(parts[i].trim());
                if (parsedConfig[i] < 0) {
                    throw new NumberFormatException("Negative value");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "For the course '%s', a configuration in the form d-d-d must be present, where d is a nonnegative integer.",
                        course
                ), e);
            }
        }

        return parsedConfig;
    }

    public static List<Lesson> getSkippedLessons(){ return skippedLessons; }

}
