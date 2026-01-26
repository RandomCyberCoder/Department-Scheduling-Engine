package org.acme.schooltimetabling.apiCalls.surveyEndpoint;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.acme.schooltimetabling.TimetableApp;
import org.acme.schooltimetabling.constants.Days;
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
            "mwf_7_am", "mwf_8_am", "mwf_9_am", "mwf_10_am", "mwf_11_am", "mwf_12_pm", "mwf_1_pm", "mwf_2_pm",
            "mwf_3_pm", "mwf_4_pm", "mwf_5_pm", "mwf_6_pm", "mwf_7_pm", "mwf_8_pm", "mwf_9_pm",
            "tr_7_am", "tr_8_am", "tr_9_am", "tr_10_am", "tr_11_am", "tr_12_pm", "tr_1_pm", "tr_2_pm",
            "tr_3_pm", "tr_4_pm", "tr_5_pm", "tr_6_pm", "tr_7_pm", "tr_8_pm", "tr_9_pm"
    );
    @JsonProperty("id")
    private int surveyID;
    @JsonProperty("name")
    private String nonCanonName;
    @JsonProperty("teacher_id")
    private int teacherFK;
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

    public Teacher toTeacher() throws Exception{
        BitSet conflict = new BitSet();
        BitSet preferences = new BitSet();
        BitSet acceptable = new BitSet();

        for(String timePrefKey: timePrefKeys){
            String val = extraFields.get(timePrefKey).toString().toLowerCase();
            BitSet bsRep = fieldNameToBitSet(timePrefKey);
            if(val.equals("preferred")){
                preferences.or(bsRep);
            }
            else if(val.equals("acceptable")){
                acceptable.or(bsRep);
            }
            else {
                //assume it's marked for conflict
                conflict.or(bsRep);
            }
        }

        return new Teacher(TeacherGenerator.getNextTeacherID(), nonCanonName, preferences, acceptable, conflict);
    }

    private BitSet fieldNameToBitSet(String fieldName) throws Exception{
        final int DAYS_IDX = 0;
        final int HOUR_IDX = 1;
        final int MERIDIEM_IDX = 2;
        final int ONE_HOUR_BLOCK = 2;
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("h:mma");

        String[] fieldNameParsed = fieldName.split("_");
        String formattedTime = String.format("%d:00%s", Integer.valueOf(fieldNameParsed[HOUR_IDX]),
                fieldNameParsed[MERIDIEM_IDX].toUpperCase());
        LocalTime localTime = LocalTime.parse(formattedTime, FORMATTER);
        EnumSet<Days> days = EnumSet.noneOf(Days.class);

        for(Character day: fieldNameParsed[DAYS_IDX].toUpperCase().toCharArray()){
            if(day == 'M') days.add(Days.MONDAY);
            else if(day == 'T') days.add(Days.TUESDAY);
            else if(day == 'W') days.add(Days.WEDNESDAY);
            else if(day == 'R') days.add(Days.THURSDAY);
            else if(day == 'F') days.add(Days.FRIDAY);
        }

        return BitSetHelper.timeSlotBitSet(localTime, ONE_HOUR_BLOCK, days);
    }

    @Override
    public String toString() {
        return String.format("DB Survey object: primary key '%d'; teacher name %s", surveyID, nonCanonName);
    }
}
