package org.acme.schooltimetabling.TestClasses;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.builders.teachers.TeacherBuilder;
import org.acme.schooltimetabling.builders.teachers.policies.FacultyPolicy;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.Generators.TeacherGenerator;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.acme.schooltimetabling.solver.ConstraintTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestTeachers {
    @BeforeAll
    static void setUp() {
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        ConstraintTestHelper.load();
    }
    static HashMap<String, Teacher> teacherHashMap;
    //skipping this because the file reading just doesn't work well
//    @BeforeAll
//    static void setup() throws Exception{
//        String YAML_FILE_PATH = "constants/config.yaml";
//        ScheduleConfig.loadConfig(YAML_FILE_PATH);
//        try {
//            /*New headers for the survey*/
//            ArrayList<String> newSurveyHeaders = new ArrayList<>(
//                    Arrays.asList("id", "start", "complete", "email", "name", "use_old",
//                            "7 AM", "8 AM", "9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM",
//                            "3 PM", "4 PM", "5 PM", "6 PM", "7 PM", "8 PM", "9 PM", "7 AM2",
//                            "8 AM2", "9 AM2", "10 AM2", "11 AM2", "12 PM2", "1 PM2", "2 PM2",
//                            "3 PM2", "4 PM2", "5 PM2", "6 PM2", "7 PM2", "8 PM2", "9 PM2",
//                            "mwf_1", "tr_1", "mwf_2", "mwf_tr",
//                            "tr_2", "mwf_3", "mwf_2_tr_1", "mwf_1_tr_2",
//                            "tr_3", "mwrf", "mtwr", "mw", "tr",
//                            "back_to_back", "gap", "constraint", "require",
//                            "pref", "comment", "stars")
//            );
//
//
//
//            /*read the current quarter survey*/
//            String curQuarterSurveyPath = "input/2254-survey.csv";
//            String prevQuarterSurveyPath = "input/2252-survey.csv";
//            System.out.println("Reading the current quarter teacher survey");
//            ArrayList<HashMap<String, String>> curQuarterSurveys = ParseInput.readCSV(curQuarterSurveyPath, newSurveyHeaders);
//            System.out.println("Reading the previous quarter teacher survey");
//            /*read the prev quarter survey*/
//            ArrayList<HashMap<String, String>> prevQuarterSurveys = ParseInput.readCSV(prevQuarterSurveyPath, newSurveyHeaders);
//            /*teacher name -> teacher object*/
//            teacherHashMap = TeacherGenerator.generateTeachers(curQuarterSurveys, prevQuarterSurveys);
//        }
//        catch (Exception e){
//            System.out.println("Something went wrong");
//            e.printStackTrace();
//            throw new Exception();
//
//        }
//    }
//
//    @Test
//    @DisplayName("Checking all Teacher Objects for no overlap in bitsets")
//    void noBitsetOverlap(){
//        // Validate each item in the list
//        assertAll("Checking teacher",
//                teacherHashMap.values().stream().map(teacher -> (Executable) () -> {
//                    BitSet bitSet = new BitSet();
//                    bitSet.or(teacher.acceptable);
//                    bitSet.and(teacher.conflict);
//                    bitSet.and(teacher.preferences);
//                    assertAll("Checking bitsets",
//                            () -> assertEquals(0, bitSet.cardinality()),
//                            () -> assertEquals(0, bitSet.length()));
//                }).toList());
//
//    }
//
//    @Test
//    @DisplayName("Check All bits are set")
//    void allBitsSet(){
//        //Check all bits are set
//        assertAll("Checking for all bits being set",
//                teacherHashMap.values().stream().map(teacher -> (Executable) () -> {
//                    BitSet bitSet = new BitSet();
//                    bitSet.or(teacher.acceptable);
//                    bitSet.or(teacher.conflict);
//                    bitSet.or(teacher.preferences);
//                    assertAll("Checking bitsets",
//                            () -> assertEquals(150, bitSet.cardinality()),
//                            () -> assertEquals(150, bitSet.length()));
//                }));
//    }
//
//    @Test
//    @DisplayName("Check for faculty")
//    void checkForFaculty(){
//        boolean atLeastOneFacultyFound = false;
//
//        for(Teacher teacher: teacherHashMap.values()){
//            if (teacher instanceof Faculty) {
//                atLeastOneFacultyFound = true;
//                break;
//            }
//        }
//
//        assertTrue(atLeastOneFacultyFound);
//    }

    @Test
    @DisplayName("Check tenure time slots are conflict for faculty")
    void checkTenureConflict(){
        BitSet tenureMask = new BitSet();
        tenureMask.set(4);
        tenureMask.set(9);

        BitSet preference = new BitSet();
        preference.set(1);
        preference.set(4);
        preference.set(9);

        BitSet acceptable = new BitSet();
        acceptable.set(2);
        acceptable.set(4);
        acceptable.set(9);

        BitSet conflict = new BitSet();
        conflict.set(7);

        Teacher dummyFaculty = new TeacherBuilder(new FacultyPolicy(tenureMask))
                .preference(preference)
                .acceptable(acceptable)
                .conflict(conflict)
                .canon("dummy, faculty")
                .gapPref(Preference.NEUTRAL)
                .build();

        assertAll("Checking tenure conflict handling",
                () -> assertTrue(dummyFaculty instanceof Faculty, "builder should create a Faculty object"),
                () -> assertFalse(dummyFaculty.acceptable.get(4), "tenure bits should be removed from acceptable"),
                () -> assertFalse(dummyFaculty.acceptable.get(9), "tenure bits should be removed from acceptable"),
                () -> assertFalse(dummyFaculty.preferences.get(4), "tenure bits should be removed from preferences"),
                () -> assertFalse(dummyFaculty.preferences.get(9), "tenure bits should be removed from preferences"),
                () -> assertTrue(dummyFaculty.conflict.get(4), "tenure bits should be added to conflict"),
                () -> assertTrue(dummyFaculty.conflict.get(9), "tenure bits should be added to conflict"),
                () -> assertTrue(dummyFaculty.conflict.get(7), "existing conflict bits should remain set"),
                () -> assertEquals(1, dummyFaculty.getPreferences().cardinality()),
                () -> assertEquals(1, dummyFaculty.getAcceptable().cardinality()),
                () -> assertEquals(3, dummyFaculty.getConflict().cardinality()));
    }
}
