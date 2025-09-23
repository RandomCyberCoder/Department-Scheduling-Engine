package org.acme.schooltimetabling.TestClasses;

import org.acme.schooltimetabling.helperClasses.Generators.TeacherGenerator;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestTeachers {
    static HashMap<String, Teacher> teacherHashMap;
    @BeforeAll
    static void setup() throws Exception{
        try {
            /*New headers for the survey*/
            ArrayList<String> newSurveyHeaders = new ArrayList<>(
                    Arrays.asList("id", "start", "complete", "email", "name", "use_old",
                            "7 AM", "8 AM", "9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM",
                            "3 PM", "4 PM", "5 PM", "6 PM", "7 PM", "8 PM", "9 PM", "7 AM2",
                            "8 AM2", "9 AM2", "10 AM2", "11 AM2", "12 PM2", "1 PM2", "2 PM2",
                            "3 PM2", "4 PM2", "5 PM2", "6 PM2", "7 PM2", "8 PM2", "9 PM2",
                            "mwf_1", "tr_1", "mwf_2", "mwf_tr",
                            "tr_2", "mwf_3", "mwf_2_tr_1", "mwf_1_tr_2",
                            "tr_3", "mwrf", "mtwr", "mw", "tr",
                            "back_to_back", "gap", "constraint", "require",
                            "pref", "comment", "stars")
            );



            /*read the current quarter survey*/
            String curQuarterSurveyPath = "java/hello-world/src/main/java/org/acme/schooltimetabling/input/2254-survey.csv";
            String prevQuarterSurveyPath = "java/hello-world/src/main/java/org/acme/schooltimetabling/input/2252-survey.csv";
            System.out.println("Reading the current quarter teacher survey");
            ArrayList<HashMap<String, String>> curQuarterSurveys = ParseInput.readCSV(curQuarterSurveyPath, newSurveyHeaders);
            System.out.println("Reading the previous quarter teacher survey");
            /*read the prev quarter survey*/
            ArrayList<HashMap<String, String>> prevQuarterSurveys = ParseInput.readCSV(prevQuarterSurveyPath, newSurveyHeaders);
            /*teacher name -> teacher object*/
            teacherHashMap = TeacherGenerator.generateTeachers(curQuarterSurveys, prevQuarterSurveys);
        }
        catch (Exception e){
            System.out.println("Something went wrong");
            e.printStackTrace();
            throw new Exception();

        }
    }

    @Test
    @DisplayName("Checking all Teacher Objects for no overlap in bitsets")
    void noBitsetOverlap(){
        // Validate each item in the list
        assertAll("Checking teacher",
                teacherHashMap.values().stream().map(teacher -> (Executable) () -> {
                    BitSet bitSet = new BitSet();
                    bitSet.or(teacher.acceptable);
                    bitSet.and(teacher.conflict);
                    bitSet.and(teacher.preferences);
                    assertAll("Checking bitsets",
                            () -> assertEquals(0, bitSet.cardinality()),
                            () -> assertEquals(0, bitSet.length()));
                }).toList());

    }

    @Test
    @DisplayName("Check All bits are set")
    void allBitsSet(){
        //Check all bits are set
        assertAll("Checking for all bits being set",
                teacherHashMap.values().stream().map(teacher -> (Executable) () -> {
                    BitSet bitSet = new BitSet();
                    bitSet.or(teacher.acceptable);
                    bitSet.or(teacher.conflict);
                    bitSet.or(teacher.preferences);
                    assertAll("Checking bitsets",
                            () -> assertEquals(150, bitSet.cardinality()),
                            () -> assertEquals(150, bitSet.length()));
                }));
    }
}
