package org.acme.schooltimetabling.helperClasses;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.util.*;

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

    private static Timeslot generateTimeslot(HashMap<String, String> timeslotMap, List<String> timeslotCSVHeaders){
        Timeslot generatedTimeslot;
        return new Timeslot();
    }
    public static ArrayList<Timeslot> generateTimeslots(){
        //parse csv
        String timeslotsFile = "java/hello-world/src/main/java/org/acme/schooltimetabling/constants/possibleTimes.csv";
        List<String> timeslotCSVHeaders = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("days", "time_start",
                "time_end", "lecture_hours", "total_hours", "days2", "time_start2", "time_end2", "lecture_hours2", "total_hours2")));
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
            Timeslot newTimeslot = generateTimeslot(timeslotMap, timeslotCSVHeaders);
            //add the timeslot instant to our list
            timeslotList.add(newTimeslot);
        }
        return timeslotList;
    }
}
