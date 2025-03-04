package org.acme.schooltimetabling.helperClasses;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.Teacher;

import java.util.*;

public class Generator {
    /**
     * Returns a hashmap of the teachers name mapped to their teacher's object. The
     * object contains members such as a unique teacher id, availability, and their
     * name
     *
     * @param curQuarterSurvey current quarter survey file path assuming it's in src directory
     * @param prevQuarterSurvey prev quarter survey file path assuming it's in src directory
     * @param replacementHeaders a list of headers to replace current file headers */
    public HashMap<String, Teacher> generateTeachers(ArrayList<HashMap<String, String>> curQuarterSurvey,
                                                     ArrayList<HashMap<String, String>>  prevQuarterSurvey,
                                                     ArrayList<String> replacementHeaders)
    {
        /* This Hash map will map the teacher's name to the teacher's object */
        HashMap<String, Teacher> teacherHashMap = new HashMap<> ();
        /*ID for a teacher. Will increment everytime */
        int teacherId = 0;
        String bleedForward = "Yes, use the same as last term";
        Stack<String> surveyTimes = new Stack<>();
        surveyTimes.addAll(Arrays.asList("7 AM","8 AM","9 AM","10 AM","11 AM","12 PM","1 PM","2 PM",
                "3 PM","4 PM","5 PM","6 PM","7 PM","8 PM","9 PM","7 AM2",
                "8 AM2","9 AM2","10 AM2","11 AM2","12 PM2","1 PM2","2 PM2",
                "3 PM2","4 PM2","5 PM2","6 PM2","7 PM2","8 PM2","9 PM2"));

        HashMap<String, String> teacherBleed = new HashMap<>();
        for(HashMap<String, String> surveyEntry : curQuarterSurvey){
            String instructorName = surveyEntry.get("name");
            BitSet preferences = new BitSet();
            BitSet acceptable = new BitSet();
            BitSet conflicts = new BitSet();

            if(bleedForward.equals(surveyEntry.get("use_old"))){
                System.out.printf("Bleeding forward %s%n", instructorName);
                teacherBleed.put(instructorName, null);
                continue;
            }

            Teacher curTeacher = new Teacher(teacherId, instructorName, preferences, acceptable, conflicts);
            teacherHashMap.put(instructorName, curTeacher);
        }

        for(HashMap<String, String> surveyEntry : prevQuarterSurvey){
            if(teacherBleed.containsKey(surveyEntry.get("name"))){
                String instructorName = surveyEntry.get("name");
                BitSet preferences = new BitSet();
                BitSet acceptable = new BitSet();
                BitSet conflicts = new BitSet();

                System.out.printf("Trying to use %s old survey%n", instructorName);
                if(bleedForward.equals(surveyEntry.get("use_old"))){
                    System.out.printf("Previous survey also bleeds forward. Skipping %s%n", instructorName);
                    continue;
                }



            }
        }
        /* Read all the teacher surveys for the current quarter*/
        //ParseInput.readCSV(, replacementHeaders);
        /* Read all the teacher survey for the previous quarter*/

        return teacherHashMap;
    }

}
