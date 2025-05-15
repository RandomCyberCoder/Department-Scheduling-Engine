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



    public static void generateLessons(HashMap<String, String> courseConfigs, BiMap<String, Integer> courseIdMapping,
                                       List<ScheduleFormat> schedules, HashMap<String, Teacher> teacherHashMap){
        final int STARTING_SECTION_NUMBER = 1;
        final String CURRENT_TERM = ParseInput.scheduleConfig.curTerm;
        final String DEPARTMENT = ParseInput.scheduleConfig.department;
        LinkedList<Lesson> lessons = new LinkedList<>();
        int lessonID = 1;
        String[] courseInformation;
        String courseName;
        String coureseModifier;

        /*create a list of courses to section number*/
        HashMap<String, Integer> courseSectionCounter = new HashMap<>();
        for(String course: courseConfigs.keySet()){
            if(!course.contains(ParseInput.scheduleConfig.department)){
                continue;
            }
            courseSectionCounter.put(course, STARTING_SECTION_NUMBER);
        }

        /*loop through what schedule (list of courses) a teacher is planned
        * to teach*/
        for(ScheduleFormat schedule: schedules){
            final String teacherName = schedule.getName();
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


            /*once list has been made schedule */
            for(String course: coursesToSchedule){
                /*we check if the course has any modifiers
                * This is also used to check if we want to skip the course*/
                courseInformation = course.split("-");
                if(courseInformation.length == 1){
                    courseName = courseInformation[0];
                    /*create class*/
                }
                else{
                    courseName = courseInformation[1];
                    coureseModifier = courseInformation[0];
                    /*we found a modifier so check if we want to schedule it
                    * or do anything special*/
                    if(Constants.SKIP_SCHEDULE.contains(courseInformation[0])){
                        continue;
                    }
                    Lesson newLesson = new Lesson(Integer.toString(lessonID++), courseName, teacherName,
                            coureseModifier, courseConfigs.get(courseName), courseIdMapping.get(courseName),
                            teacherHashMap.get());
                }
            }


        }
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
        for(String teacherName: teacherHashMap.keySet()){
            /*I'm assuming here that the names in the survey file are in the format
            * "<First> <Last>" where first and last are both char sequences with no spaces*/
            nameSplit = teacherName.split(" ");
            firstName = nameSplit[0];
            lastName = nameSplit[1];
            for(ScheduleFormat schedule: schedules){
                teacherName_schedule = schedule.getName();
                if(teacherName_schedule.contains(firstName) && teacherName_schedule.contains(lastName)){
                    teacherNameToCanon.put(teacherName, teacherName_schedule);
                }
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
        BiMap<String, Integer> courseIdMapping = HashBiMap.create();
        int count = 1;

        while(courses.hasNext()) {
            String course = courses.next();
            if(!course.contains(ParseInput.scheduleConfig.department)){
                continue;
            }
            courseIdMapping.put(course, count++);
        }

        return courseIdMapping;
    }

}
