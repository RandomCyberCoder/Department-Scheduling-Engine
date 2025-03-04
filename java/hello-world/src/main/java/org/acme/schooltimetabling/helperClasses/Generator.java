package org.acme.schooltimetabling.helperClasses;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.Teacher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.HashMap;

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

        for(HashMap<String, String> surveyEntry : curQuarterSurvey){
            String instructorName = surveyEntry.get("name");
            BitSet preferences = new BitSet();
            BitSet acceptable = new BitSet();
            BitSet conflicts = new BitSet();
            Teacher curTeacher = new Teacher(teacherId, instructorName, preferences, acceptable, conflicts);
            teacherHashMap.put(instructorName, curTeacher);
        }
        /* Read all the teacher surveys for the current quarter*/
        //ParseInput.readCSV(, replacementHeaders);
        /* Read all the teacher survey for the previous quarter*/

        return teacherHashMap;
    }

}
