package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.apiCalls.surveyEndpoint.SurveyCalls;
import org.acme.schooltimetabling.apiCalls.surveyEndpoint.SurveyRecord;
import org.acme.schooltimetabling.apiCalls.teacherEndpoint.TeacherRecord;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TeacherGenerator extends Generator{
    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherGenerator.class);
    private static int nextTeacherID = 0;

    /**
     *
     * @return A Map mapping a teacher's canon name to their teacher object
     */
    public static Map<String, Teacher> teacherGenDriver() throws Exception{
        String YAML_FILE_PATH = "constants/config.yaml";
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        Map<String, Teacher> teacherMap = new HashMap<>();
        boolean success = false;
        if(ScheduleConfig.isUseApi()){
            try{
                List<SurveyRecord> surveys = SurveyCalls.termSurveysToRecord();
                if(surveys.isEmpty()){
                    LOGGER.warn(String.format("No surveys found in the DB for the term %s. Falling back to files",
                            ScheduleConfig.getCurTerm()));
                }
                else{
                    for(SurveyRecord surveyRecord: surveys){
                        surveyRecord.toTeacher();
                        TeacherRecord teacherRecord = surveyRecord.getTeacherRecord();
                        teacherMap.put(teacherRecord.getCanon(), surveyRecord.toTeacher());
                    }
                    success = true;
                    LOGGER.info("Succeeded generating teachers using API call");
                }
            }
            catch(Exception e){
                LOGGER.error(String.format("Error reading surveys from api falling back to files. Error message: %s",
                        e.getMessage()));
            }
        }

        //Read surveys from files if API usage is not used, or it failed
        if(!success){
            /*New headers for the survey*/
            ArrayList<String> newSurveyHeaders = new ArrayList<>(
                    Arrays.asList("id", "start", "complete", "email", "name", "use_old",
                            "7 AM","8 AM","9 AM","10 AM","11 AM","12 PM","1 PM","2 PM",
                            "3 PM","4 PM","5 PM","6 PM","7 PM","8 PM","9 PM","7 AM2",
                            "8 AM2","9 AM2","10 AM2","11 AM2","12 PM2","1 PM2","2 PM2",
                            "3 PM2","4 PM2","5 PM2","6 PM2","7 PM2","8 PM2","9 PM2",
                            "mwf_1", "tr_1", "mwf_2", "mwf_tr",
                            "tr_2", "mwf_3","mwf_2_tr_1", "mwf_1_tr_2",
                            "tr_3", "mwrf", "mtwr", "mw", "tr",
                            "back_to_back", "gap", "constraint", "require",
                            "pref", "comment", "stars")
            );

            /*read the cur & prev quarter survey and then create Teacher objects*/
            String curQuarterSurveyPath = String.format("input/%s-survey.csv", ScheduleConfig.getCurTerm());
            String prevQuarterSurveyPath = String.format("input/%s-survey.csv", ScheduleConfig.getPrevTerm());
            LOGGER.info("Reading the current quarter teacher survey");
            ArrayList<HashMap<String, String>> curQuarterSurveys = ParseInput.readCSV(curQuarterSurveyPath, newSurveyHeaders);
            LOGGER.info("Reading the previous quarter teacher survey");
            /*read the prev quarter survey*/
            ArrayList<HashMap<String, String>> prevQuarterSurveys  = ParseInput.readCSV(prevQuarterSurveyPath,newSurveyHeaders);
            LOGGER.info("Creating teacher objects from files");
            /*teacher name -> teacher object*/
            teacherMap = TeacherGenerator.generateTeachers(curQuarterSurveys, prevQuarterSurveys);
        }

        return teacherMap;
    }

    /**
     * Generates the Teacher/Faculty object for the instructor's survey entry. It
     * will initialize the preferred, acceptable, and conflicts <code>BitSet</code>
     * for the instructor
     *
     * @param surveyEntry The HashMap representation of the instructor's survey entry
     * @param times The name of the keys in the <code>surveyEntry</code> parameter
     * that corresponds to times
     * @return Faculty object if teacher is found to be a faculty member; otherwise a
     * Teacher object is returned
     * @throws
     * @see #nextTeacherID*/
    private static Teacher generateTeacher(HashMap<String, String> surveyEntry, List<String> times) throws Exception{
        String instructorName = surveyEntry.get("name");
        String canonName = Constants.TEACHER_NAME_TO_CANON.get(instructorName);
        BitSet preferred = new BitSet();
        BitSet acceptable = new BitSet();
        BitSet conflicts = new BitSet();


        if(canonName == null){
            LOGGER.error(String.format("Couldn't find canon name for %s. SKIPPING", instructorName));
            return null;
        }

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

        String[] splitName = canonName.split(",");

        if(Constants.FACULTY_LAST_NAMES.contains(splitName[0].strip())){
            LOGGER.info(String.format("Instructor '%s' identified as faculty", canonName));
            return new Faculty(getNextTeacherID(), instructorName, preferred, acceptable, conflicts);
        }
        return new Teacher(getNextTeacherID(), canonName, preferred, acceptable, conflicts);
    }

    /**
     * Returns a hashmap of the teacher's canon name mapped to their teacher's object. If the person is a faculty
     * member the teacher object will actually be a <i>Faculty</i> object.  The function will try to bleed an old survey
     * if the professor chooses. If a professor bleeds forward in the old survey, the teacher is skipped
     *
     * @param curQuarterSurvey current quarter survey file path assuming it's in src directory
     * @param prevQuarterSurvey prev quarter survey file path assuming it's in src directory
     * @return Hashmap mapping a teacher's canon name to their teacher object
     * @throws
     * */
    public static HashMap<String, Teacher> generateTeachers(List<HashMap<String, String>> curQuarterSurvey,
                                                     List<HashMap<String, String>> prevQuarterSurvey) throws Exception
    {
        /* This Hash map will map the teacher's name to the teacher's object */
        HashMap<String, Teacher> teacherHashMap = new HashMap<> ();
        final String BLEED_FORWARD_STRING = "Yes, use the same as last term";
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
            if(BLEED_FORWARD_STRING.equals(surveyEntry.get("use_old"))){
                LOGGER.info(String.format("Bleeding forward %s", instructorName));
                teacherBleed.put(instructorName, null);

            }
            else{
                /*if the instructor didn't want to bleed forward create the instructor's
                 * Teacher instance*/
                LOGGER.info(String.format("Teacher %s did not bleed forward", instructorName));
                Teacher curTeacher = generateTeacher(surveyEntry, surveyTimes);

                if(curTeacher == null) continue;

                /*add the Teacher instance to our HashMap to be later used for creating
                 * the lessons*/
                teacherHashMap.put(Constants.TEACHER_NAME_TO_CANON.get(instructorName), curTeacher);
            }

        }

        /*read the previous quarter survey entries in case anyone bled forward*/
        for(HashMap<String, String> surveyEntry : prevQuarterSurvey){
            String instructorName = surveyEntry.get("name").strip();
            /*Check if the instructor wanted to bleed forward*/
            if(teacherBleed.containsKey(instructorName)){

                LOGGER.info(String.format("Trying to use %s's old survey", instructorName));
                /*If the instructor choose to bleed forward in the previous survey
                * we will be forced to skip them :( */
                if(BLEED_FORWARD_STRING.equals(surveyEntry.get("use_old"))){
                    LOGGER.warn(String.format("Previous survey also bleeds forward. SKIPPING %s", instructorName));
                    continue;
                }

                LOGGER.info(String.format("Old survey found for %s, creating their teacher object instance"
                        , instructorName));

                /*if they bled forward, and we have a survey entry then we create their
                * Teacher instance*/
                Teacher curTeacher = generateTeacher(surveyEntry, surveyTimes);
                if(curTeacher == null) continue;
                teacherHashMap.put(Constants.TEACHER_NAME_TO_CANON.get(instructorName), curTeacher);
                /*Remove from teachers left to bleed*/
                teacherBleed.remove(instructorName);
            }
        }

        //go through teachers that didn't bleed and notify
        if(!teacherBleed.keySet().isEmpty()){
            StringBuilder names = new StringBuilder();
            for(String teacherName: teacherBleed.keySet()){
                names.append(String.format("%s; ", teacherName));
            }
            LOGGER.warn("Couldn't bleed The following teachers. No survey from the previous quarter, " +
                    " previous quarter previous quarter bled or" +
                    " no canon name associated with the survey (aka non-canon) name was found.\n" +
                    names);
        }

        return teacherHashMap;
    }

    /**
     * The class keeps an internal counter for the next available teacher ID. The next valid teacher ID
     * is what should be used for a newly created teacher object
     * @return valid teacher ID
     * @see Teacher
     */
    static public int getNextTeacherID(){
        return nextTeacherID++;
    }
}
