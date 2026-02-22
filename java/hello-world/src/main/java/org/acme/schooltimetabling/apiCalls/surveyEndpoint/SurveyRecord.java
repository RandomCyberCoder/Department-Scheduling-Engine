package org.acme.schooltimetabling.apiCalls.surveyEndpoint;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.acme.schooltimetabling.TimetableApp;
import org.acme.schooltimetabling.apiCalls.teacherEndpoint.TeacherRecord;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.TeacherGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SurveyRecord {
    private static final Logger LOGGER = LoggerFactory.getLogger(SurveyRecord.class);
    /*Column names in the DB*/
    private final Set<String> timePrefKeys = Set.of(
            "M_7_AM","M_8_AM","M_9_AM","M_10_AM","M_11_AM","M_12_PM",
            "M_1_PM","M_2_PM","M_3_PM","M_4_PM","M_5_PM","M_6_PM",
            "M_7_PM","M_8_PM","M_9_PM",

            "T_7_AM","T_8_AM","T_9_AM","T_10_AM","T_11_AM","T_12_PM",
            "T_1_PM","T_2_PM","T_3_PM","T_4_PM","T_5_PM","T_6_PM",
            "T_7_PM","T_8_PM","T_9_PM",

            "W_7_AM","W_8_AM","W_9_AM","W_10_AM","W_11_AM","W_12_PM",
            "W_1_PM","W_2_PM","W_3_PM","W_4_PM","W_5_PM","W_6_PM",
            "W_7_PM","W_8_PM","W_9_PM",

            "R_7_AM","R_8_AM","R_9_AM","R_10_AM","R_11_AM","R_12_PM",
            "R_1_PM","R_2_PM","R_3_PM","R_4_PM","R_5_PM","R_6_PM",
            "R_7_PM","R_8_PM","R_9_PM",

            "F_7_AM","F_8_AM","F_9_AM","F_10_AM","F_11_AM","F_12_PM",
            "F_1_PM","F_2_PM","F_3_PM","F_4_PM","F_5_PM","F_6_PM",
            "F_7_PM","F_8_PM","F_9_PM"
    );
    private Teacher teacher_rep = null;
    @JsonProperty("id")
    private int surveyID;
    @JsonProperty("name")
    private String nonCanonName;
    @JsonProperty("teacher_id")
    private int teacherFK;
    @JsonProperty("teacher_detail")
    private TeacherRecord teacherRecord;
    /**All extra Json properties not captured by class members with the @JsonProperty tag will
     * be placed into this variable*/
    private Map<String, Object> extraFields = new HashMap<>();

    @JsonAnySetter
    public void setExtraField(String key, Object value) {
        extraFields.put(key, value);
    }

    /**
     * Checks if  all time preference fields are present.
     * @return True if all fields exist; False otherwise
     */
    public boolean valid(){
        Set<String> extraFieldKeys = extraFields.keySet();
        extraFieldKeys.retainAll(timePrefKeys);
        return extraFieldKeys.size() == timePrefKeys.size();
    }

    /**
     * Only one Teacher(or Faculty) object will be if and only if this function is called. The class
     * will hold on to the created object to prevent the teacher from existing multiple times.
     * @return Teacher representation of the survey. In other words, they have their preferences set.
     * @throws Exception
     */
    public Teacher toTeacher(){
        if(teacher_rep == null) teacher_rep = createTeacherRep();
        return  teacher_rep;
    }

    private Teacher createTeacherRep(){
        BitSet conflict = new BitSet();
        BitSet preferences = new BitSet();
        BitSet acceptable = new BitSet();

        for(String timePrefKey: timePrefKeys){
            String val = extraFields.get(timePrefKey).toString();
            BitSet bsRep = BitSetHelper.srvHdrToBs(timePrefKey);
            if(val.equalsIgnoreCase("preferred")){
                preferences.or(bsRep);
            }
            else if(val.equalsIgnoreCase("acceptable")){
                acceptable.or(bsRep);
            }
            else {
                //assume it's marked for conflict
                conflict.or(bsRep);
            }
        }
        Teacher teacher = new Teacher(TeacherGenerator.getNextTeacherID(), nonCanonName, preferences, acceptable, conflict);
        if(teacherRecord.isFaculty()) teacher = new Faculty(teacher);
        return teacher;
    }

    /**
     * Gives the teacher record associated with the survey record in the DB at the time the api call
     * was made to retrieve the survey record.
     * @return
     */
    public TeacherRecord getTeacherRecord() {
        return teacherRecord;
    }

    @Override
    public String toString() {
        return String.format("DB Survey object: primary key '%d'; teacher name %s", surveyID, nonCanonName);
    }
}
