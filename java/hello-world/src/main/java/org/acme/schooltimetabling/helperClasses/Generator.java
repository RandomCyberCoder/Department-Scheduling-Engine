package org.acme.schooltimetabling.helperClasses;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.util.*;
import java.util.stream.Collectors;

/*TODO makes this generator class a abstract parent class:
*   -should have abstract methods: generateObj and generateObjects
*   -child classes will be gen<Obj> where objec can be Class, Teacher, ....
*   -any other non specific class methods can go in the parent class... I think*/
public class Generator {
    /**
     * Generates the Teacher object for the instructor's survey entry. It
     * will initialize the preferred, acceptable, and conflicts <code>BitSet</code>
     * for the instructor
     *
     * @param surveyEntry The HashMap representation of the instructor's survey entry
     * @param times The name of the keys in the <code>surveyEntry</code> parameter
     * that corresponds to times
     * @param instructorID the unique ID for the instructor*/
    private static Teacher generateTeacher(HashMap<String, String>surveyEntry, List<String>times,
                                    int instructorID) throws Exception{
        String instructorName = surveyEntry.get("name");
        BitSet preferred = new BitSet();
        BitSet acceptable = new BitSet();
        BitSet conflicts = new BitSet();

        for(String time : times){
            /*lower case for future-proof*/
            String availability = surveyEntry.get(time).toLowerCase();
            BitSet bitsetAvailability = BitSetHelper.surveyBitset(time);

            /*add the bitset to the right bitset. Default is a conflict,
            * if the person doesn't choose acceptable, preferred or conflict
            * we assume the time slot is a conflict*/
            switch (availability){
                case "acceptable" -> acceptable.or(bitsetAvailability);
                case "preferred" -> preferred.or(bitsetAvailability);
                default -> conflicts.or(bitsetAvailability);
            }
        }

        return new Teacher(instructorID, instructorName, preferred, acceptable, conflicts);
    }

    /**
     * Returns a hashmap of the teachers name mapped to their teacher's object. The
     * object contains members such as a unique teacher id, availability, and their
     * name
     *
     * @param curQuarterSurvey current quarter survey file path assuming it's in src directory
     * @param prevQuarterSurvey prev quarter survey file path assuming it's in src directory*/
    public static HashMap<String, Teacher> generateTeachers(List<HashMap<String, String>> curQuarterSurvey,
                                                     ArrayList<HashMap<String, String>>  prevQuarterSurvey) throws Exception
    {
        /* This Hash map will map the teacher's name to the teacher's object */
        HashMap<String, Teacher> teacherHashMap = new HashMap<> ();
        /*ID for a teacher. Will increment everytime */
        int teacherId = 0;
        final String bleedForward = "Yes, use the same as last term";
        /*List of the time headers that are key's in the survey
        * entry HashMaps*/
        /*make it unmodifiable because this list should never change*/
        final List<String> surveyTimes =
                Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
                "7 AM","8 AM","9 AM","10 AM","11 AM","12 PM","1 PM","2 PM",
                "3 PM","4 PM","5 PM","6 PM","7 PM","8 PM","9 PM","7 AM2",
                "8 AM2","9 AM2","10 AM2","11 AM2","12 PM2","1 PM2","2 PM2",
                "3 PM2","4 PM2","5 PM2","6 PM2","7 PM2","8 PM2","9 PM2")));

        /*we will use a hashmap to keep track of who want to bleed forward for
        * easy lookup*/
        HashMap<String, String> teacherBleed = new HashMap<>();

        /*read the current quarter's survey entries*/
        for(HashMap<String, String> surveyEntry : curQuarterSurvey){
            String instructorName = surveyEntry.get("name");

            /*check if the instructor wants to bleed forward in current quarter's survey*/
            if(bleedForward.equals(surveyEntry.get("use_old"))){
                System.out.printf("Bleeding forward %s%n", instructorName);
                teacherBleed.put(instructorName, null);
                continue;
            }

            /*if the instructor didn't want to bleed forward create the instructor's
            * Teacher instance*/
            System.out.printf("Creating teacher object instance for %s%n", instructorName);
            Teacher curTeacher = generateTeacher(surveyEntry, surveyTimes, teacherId++);

            /*add the Teacher instance to our HashMap to be later used for creating
            * the lessons*/
            teacherHashMap.put(instructorName, curTeacher);
        }

        /*read the previous quarter survey entries in case anyone bled forward*/
        for(HashMap<String, String> surveyEntry : prevQuarterSurvey){
            String instructorName = surveyEntry.get("name");
            /*Check if the instructor wanted to bleed forward*/
            if(teacherBleed.containsKey(instructorName)){

                System.out.printf("Trying to use %s old survey%n", instructorName);
                /*If the instructor choose to bleed forward in the previous survey
                * we will be forced to skip them :( */
                if(bleedForward.equals(surveyEntry.get("use_old"))){
                    System.out.printf("Previous survey also bleeds forward. Skipping %s%n", instructorName);
                    continue;
                }

                System.out.printf("Old survey found for %s, creating their teacher object instance%n", instructorName);

                /*if they bled forward and we have a survey entry then we create their
                * Teacher instance*/
                Teacher curTeacher = generateTeacher(surveyEntry, surveyTimes, teacherId++);
                teacherHashMap.put(instructorName, curTeacher);
            }
        }

        return teacherHashMap;
    }

    private static Timeslot generateTimeslot(int ID, HashMap<String, String> timeslotMap){
        Timeslot newTimeslot = null;
        String days = timeslotMap.get("days");
        String timeStart = timeslotMap.get("time_start");
        String timeEnd = timeslotMap.get("time_end");
        float lectureHours = Float.parseFloat(timeslotMap.get("lecture_hours"));
        float totalHours = Float.parseFloat(timeslotMap.get("total_hours"));
        String days2 = timeslotMap.get("days2");
        String timeStart2 = timeslotMap.get("time_start2");
        String timeEnd2 = timeslotMap.get("time_end2");
        /*if the there is a valid entry for the lecture_hours2 or total_hours2 we use the value
        * if there isn't a valid value we just generate a random one because this second timeslot
        * isn't meant to be used*/
        float lecture_hours2 = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("lecture_hours2"))
                        .filter(s -> !s.isBlank())
                        .orElse(String.valueOf(Math.random() * 10))
        );
        float total_hours2 = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("total_hours2"))
                        .filter(s -> !s.isBlank())
                        .orElse(String.valueOf(Math.random() * 10))
        );

        try {
            newTimeslot = new Timeslot(ID, days, timeStart, timeEnd, lectureHours, totalHours,
                    days2, timeStart2, timeEnd2, lecture_hours2, total_hours2);
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }
        return newTimeslot;
    }

    /*TODO change this to take in the file name instead*/
    public static ArrayList<Timeslot> generateTimeslots(){
        int timeslotID = 0;
        //parse csv
        String timeslotsFile = "java/hello-world/src/main/java/org/acme/schooltimetabling/constants/possibleTimes.csv";
        ArrayList<Timeslot> timeslotList = new ArrayList<>();
        ArrayList<HashMap<String, String>> timeslotCSV= null;
        try{
            timeslotCSV = ParseInput.readCSV(timeslotsFile, null);
            if(timeslotCSV.isEmpty()){
                throw new Exception("timeslot list is empty");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }
        //loop through entries
        for(HashMap<String, String > timeslotMap: timeslotCSV) {
            //instantiate a timeslot instance for every entry
            Timeslot newTimeslot = generateTimeslot(timeslotID++, timeslotMap);
            //add the timeslot instant to our list
            timeslotList.add(newTimeslot);
        }
        return timeslotList;
    }


    /**
     * <p>Generates all lessons and filter out any lesson types specified by the Constants.SKIP_SCHEDULE
     * variable given</p>
     * @param courseConfigs HashMap of course names to their configurations
     * @param courseIdMapping a bimap of course names to their unique ID
     * @param schedules a list of ScheduleFormat objects containing courses that
     *                  will be taught by a teacher
     * @param teacherHashMap HashMap of teacher canon names to their teacher object
     */
    public static LinkedList<Lesson> generateLessons(HashMap<String, String> courseConfigs, BiMap<String, Integer> courseIdMapping,
                                       List<ScheduleFormat> schedules, HashMap<String, Teacher> teacherHashMap){
        final int STARTING_SECTION_NUMBER = 1;
        final String DUMMY_COURSE_MODIFIER = "";
        final String CURRENT_TERM = ParseInput.scheduleConfig.curTerm;
        final String DEPARTMENT = ParseInput.scheduleConfig.department.toLowerCase();
        LinkedList<Lesson> lessons = new LinkedList<>();
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
            *  the same but this might not be possible*/
            /*once list has been made schedule */
            for(String course: coursesToSchedule){
                /*we check if the course has any modifiers
                * This is also used to check if we want to skip the course*/
                courseInformation = course.split("-");
                if(courseInformation.length == 1){
                    courseName = courseInformation[0];
                    courseModifier = DUMMY_COURSE_MODIFIER;
                    courseConfig = courseConfigs.get(courseName);
                    hasLabOrAct = determineLabOrAct(courseConfig);
                    sectionNumber = courseSectionCounter.get(courseName);
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
                else{
                    courseName = courseInformation[1];
                    courseModifier = courseInformation[0];
                    courseModifier = Constants.SPECIAL_CODE_CONVERSION.get(courseModifier);
                    courseConfig = courseConfigs.get(courseName);
                    hasLabOrAct = determineLabOrAct(courseConfig);
                    sectionNumber = courseSectionCounter.get(courseName);
                    /*We increase the section counter by two if it has a lab because a lesson consists of it lecture
                    * and its lab/act and a lab/act section number is separate from its respective lecture section
                    * number*/
                    courseSectionCounter.replace(courseName, (hasLabOrAct ? sectionNumber + 2 : sectionNumber + 1) );


                    /*we found a modifier so check if we want to schedule it
                    * or do anything special*/
                    /*TODO realized this doesn't actually currently work because the course has the abbreviation
                    *  and the SKiP_SCHEDULE set contains the non-abbreviated terms. Fix this and add debug
                    *  statements when we skip a course*/
                    if(Constants.SKIP_SCHEDULE.contains(courseModifier)){
                        if(Constants.DEBUG){
                            System.out.printf("Skipping scheduling of course with name %s with " +
                                    "modifier %s\n", courseName, courseModifier);
                        }
                        continue;
                    }
                    Lesson newLesson = new Lesson(Integer.toString(lessonID++), sectionNumber, courseName, teacherName,
                            courseModifier, courseConfig, courseIdMapping.get(courseName),
                            teacherHashMap.get(teacherName));
                    lessons.add(newLesson);
                }
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
        final int NO_UNITS = 0;
        String[] units = courseConfig.split("-");
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        return !(labUnits == NO_UNITS && actUnits == NO_UNITS);
    }

    /**
     * <p>The function creates a mapping from the names in the teacher HashMap to the respective
     * name in the schedules List. We need this so we can link the names in the teachers survey file
     * to the names in the schedule-DEPARTMENT-TERM file because they names are not the same. The name
     * in the latter file will be the cannon name which all names for the teacher must map to.</p>
     *
     * @param teacherHashMap Hashmap of the teacher's name to their Teacher object
     * @param schedules List of ScheduleFormat objects that contain what courses each teacher will teach
     * @return A mapping of the names from the teacher HashMap to the respective name in the List parameter
     */
    public static void createTeacherNameMapping(
            HashMap<String, Teacher> teacherHashMap, List<ScheduleFormat> schedules){
        String teacherName_schedule;
        String[] nameSplit;
        String firstName;
        String lastName;
        HashMap<String, String> teacherNameToCanon = Constants.TEACHER_NAME_TO_CANON;
        boolean mappingFound;
        String preexistingName;

        for(String teacherName: teacherHashMap.keySet()){
            mappingFound = false;
            /*I'm assuming here that the names in the survey file are in the format
            * "<First> <Last>" where First is a char sequences with no spaces and
            * Last a char sequence the can have spaces but only the first portion of
            * it is considered. If it violates this I assume it's some generic schedule and skip it*/
            nameSplit = teacherName.split(" ");
            if(Constants.DEBUG && nameSplit.length < 2){
                System.out.println((String.format("In createTeacherNameMapping skipping teacher key in HashMap " +
                        "that has value %s", teacherName)));
                continue;
            }
            firstName = nameSplit[0];
            lastName = nameSplit[1];
            /*If the name is already in the name remapping then it means that they
            * are a special case, and already exists in the mapping, so we can skip*/
            if(teacherNameToCanon.get(teacherName) != null){
                continue;
            }
            for(ScheduleFormat schedule: schedules){
                teacherName_schedule = schedule.getName();
                if(teacherName_schedule.contains(firstName) && teacherName_schedule.contains(lastName)){
                    mappingFound = true;
                    teacherNameToCanon.put(teacherName, teacherName_schedule);
                    break;
                }
            }
            if(Constants.DEBUG && !mappingFound){
                System.out.printf("Couldn't find a mapping for %s\n", teacherName);
            }
        }
    }

    private static Teacher findTeacher(String teacherName, HashMap<String, Teacher> teacherHashMap) throws Exception{
        /*Later on we should include more sophisticated code to do extra searches to find a teacher*/
        Teacher teacherFound = teacherHashMap.get(teacherName);
        if(teacherFound == null){
            throw new Exception(String.format("Couldn't find a teacher object for teacher '%s'", teacherName));
        }
        return teacherFound;
    }

    /**
     * <p>This method will take an iterator of all possible courses and will
     * return a bidirectional map of courses in the current department we want
     * to schedule mapped to an ID created for them (an integer)</p>
     *
     * @param courses an iterator for all courses
     * @return A bidirectional map of a course to its ID
     */
    public static BiMap<String, Integer> genCourseToIdMapping(Iterator<String> courses){
        final String DEPARTMENT = ParseInput.scheduleConfig.department.toLowerCase();
        BiMap<String, Integer> courseIdMapping = HashBiMap.create();
        int count = 1;
        String course;

        while(courses.hasNext()){
            course = courses.next();
            if(!course.contains(DEPARTMENT)){
                continue;
            }
            courseIdMapping.put(course, count++);
        }

        return courseIdMapping;
    }

}
