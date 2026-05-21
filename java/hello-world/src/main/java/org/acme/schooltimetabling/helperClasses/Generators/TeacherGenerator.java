package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.apiCalls.surveyEndpoint.SurveyCalls;
import org.acme.schooltimetabling.apiCalls.surveyEndpoint.SurveyRecord;
import org.acme.schooltimetabling.apiCalls.teacherEndpoint.TeacherRecord;
import org.acme.schooltimetabling.builders.teachers.TeacherBuilder;
import org.acme.schooltimetabling.builders.teachers.policies.DefaultTeachingPolicy;
import org.acme.schooltimetabling.builders.teachers.policies.FacultyPolicy;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.defaultTimes.DefaultTime;
import org.acme.schooltimetabling.defaultTimes.DefaultTimeRegistry;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.fileObjects.PrescheduleObject;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
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
    public static Map<String, Teacher> teacherGenDriver(){
        ScheduleConfig.loadConfig("constants/config.yaml");
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
                        //just make getTEacherRecord the only one called
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
                            "F_7_PM","F_8_PM","F_9_PM",

                            "pref_minDays", "pref_5days", "pref_TPD", "back_to_back", "gap", "lecAct_1hrLec", "lecAct_2hrAct",
                            "lecAct_noPref", "lecAct_notSure",

                            "constraint", "require", "pref", "comment", "stars")
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

        if(ScheduleConfig.getPrescheduledFileName() != null){
            LOGGER.info("Reading in prescheduled time file");
            PrescheduleObject preschedTimes = ParseInput.readPrescheduledFile();
            if(preschedTimes.hasTeachers()) {
                prescheduleUpdate(teacherMap, preschedTimes.getTeachers());
            }
        }

        return teacherMap;
    }


    /**
     * Returns a hashmap of the teacher's canon name mapped to their teacher's object. If the person is a faculty
     * member the teacher object will actually be a <i>Faculty</i> object.  The function will try to bleed an old survey
     * if the professor chooses. If a professor bleeds forward in the old survey, the teacher is skipped.
     *
     * If a teacher has no survey for the current quarter, then we will try to use the survey from the previous quarter
     *
     * @param curQuarterSurvey current quarter survey file path assuming it's in src directory
     * @param prevQuarterSurvey prev quarter survey file path assuming it's in src directory
     * @return Hashmap mapping a teacher's canon name to their teacher object
     * @throws
     * */
    public static HashMap<String, Teacher> generateTeachers(List<HashMap<String, String>> curQuarterSurvey,
                                                     List<HashMap<String, String>> prevQuarterSurvey){
        /* This Hash map will map the teacher's CANON name to their teacher object */
        HashMap<String, Teacher> teacherHashMap = new HashMap<> ();
        final String BLEED_FORWARD_STRING = "Yes, use the same as last term";
        final String BLEED_FORWARD_KEY = "use_old";

        /*we will use a hashmap to keep track of who want to bleed forward for
        * easy lookup*/
        HashMap<String, String> teacherBleed = new HashMap<>();

        /*read the current quarter's survey entries*/
        for(HashMap<String, String> surveyEntry : curQuarterSurvey){
            String instructorName = surveyEntry.get("name");

            /*check if the instructor wants to bleed forward in current quarter's survey*/
            if(BLEED_FORWARD_STRING.equals(surveyEntry.get(BLEED_FORWARD_KEY))){
                LOGGER.info(String.format("Bleeding forward %s", instructorName));
                teacherBleed.put(instructorName, null);

            }
            else{
                /*if the instructor didn't want to bleed forward create the instructor's
                 * Teacher instance*/
                LOGGER.info(String.format("Teacher %s did not bleed forward", instructorName));
                Teacher curTeacher = generateTeacher(surveyEntry);

                if(curTeacher == null) continue;

                /*add the Teacher instance to our HashMap to be later used for creating
                 * the lessons*/
                teacherHashMap.put(Constants.TEACHER_NAME_TO_CANON.get(instructorName), curTeacher);
            }

        }

        /*read the previous quarter survey entries in case anyone bled forward*/
        for(HashMap<String, String> surveyEntry : prevQuarterSurvey){
            String instructorName = surveyEntry.get("name").strip();
            final String canonName = Constants.TEACHER_NAME_TO_CANON.get(instructorName);

            /*Check if the instructor wanted to bleed forward*/
            if(teacherBleed.containsKey(instructorName)){

                LOGGER.info(String.format("Trying to use %s's old survey so they can bleed forward", instructorName));
                /*If the instructor choose to bleed forward in the previous survey
                * we will be forced to skip them :( */
                if(BLEED_FORWARD_STRING.equals(surveyEntry.get(BLEED_FORWARD_KEY))){
                    LOGGER.warn(String.format("Previous survey also bleeds forward. SKIPPING %s", instructorName));
                    continue;
                }

                LOGGER.info(String.format("Old survey found for %s, creating their teacher object instance"
                        , instructorName));

                /*if they bled forward, and we have a survey entry then we create their
                * Teacher instance*/
                Teacher curTeacher = generateTeacher(surveyEntry);
                if(curTeacher == null) continue;
                teacherHashMap.put(canonName, curTeacher);
                /*Remove from teachers left to bleed*/
                teacherBleed.remove(instructorName);
            }
            //try to use an instructor's old survey if they are missing one for the current term
            //make sure they didn't bleed forward though
            else if(!teacherHashMap.containsKey(canonName) &&
                    !BLEED_FORWARD_STRING.equals(surveyEntry.get(BLEED_FORWARD_KEY))){
                LOGGER.warn("When reading the previous term's survey, instructor, with name '{}', was found but with no survey " +
                        "for the current term. Trying to use the previous term's survey for them", instructorName);
                Teacher teacher = generateTeacher(surveyEntry);
                if(teacher == null) continue;
                teacherHashMap.put(canonName, teacher);
            }
        }

        //go through teachers that didn't bleed and notify
        if(!teacherBleed.isEmpty()){
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
     * Generates the Teacher/Faculty object for the instructor's survey entry. It
     * will initialize the preferred, acceptable, and conflicts <code>BitSet</code>
     * for the instructor
     *
     * @param surveyEntry The HashMap representation of the instructor's survey entry
     * @return Faculty object if teacher is found to be a faculty member; otherwise a
     * Teacher object is returned
     * @throws
     * @see #nextTeacherID*/
    private static Teacher generateTeacher(HashMap<String, String> surveyEntry){
        String instructorName = surveyEntry.get("name");
        String canonName = Constants.TEACHER_NAME_TO_CANON.get(instructorName);
        BitSet preferred = new BitSet();
        BitSet acceptable = new BitSet();
        BitSet conflicts = new BitSet();
        /*List of the time headers that are key's in the survey
         * entry HashMaps*/
        /*make it unmodifiable because this list should never change*/
        final List<String> surveyTimes =
                Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
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
                        "F_7_PM","F_8_PM","F_9_PM")));

        if(canonName == null){
            LOGGER.error(String.format("Couldn't find canon name for %s. SKIPPING", instructorName));
            return null;
        }

        for(String time : surveyTimes){
            /*lower case for future-proof*/
            String availability = surveyEntry.get(time).toLowerCase();

            BitSet bitsetAvailability = BitSetHelper.srvHdrToBs(time);

            /*add the bitset to the right bitset. Default is a conflict,
             * if the person doesn't choose acceptable, preferred or conflict
             * we assume the time slot is a conflict*/
            switch (availability){
                case "acceptable" -> acceptable.or(bitsetAvailability);
                case "preferred" -> preferred.or(bitsetAvailability);
                default -> conflicts.or(bitsetAvailability);
            }
        }

        //TODO; CHECK UPDATE
        String[] splitName = canonName.split(",");
        Preference gapPref = Preference.parsePref(surveyEntry.get("gap"));
        boolean isFaculty = Constants.FACULTY_LAST_NAMES.contains(splitName[0].strip());
        TeacherBuilder builder = new TeacherBuilder(new DefaultTeachingPolicy());
        if(isFaculty){
            LOGGER.info(String.format("Instructor '%s' identified as faculty. Promoting to faculty.....", canonName));
            builder.setPolicy(new FacultyPolicy());
        }
        return builder.preference(preferred)
                .acceptable(acceptable)
                .conflict(conflicts)
                .canon(canonName)
                .gapPref(gapPref)
                .build();
    }


    /**
     * Preschedule a teacher. If not teacher object has been created previously
     * @param teacherMap map of canon name to <i>Teacher</i> object
     * @param presched map of non canon name to their
     */
    private static void prescheduleUpdate(Map<String, Teacher> teacherMap,
                                          Map<String, List<PrescheduleObject.PrescheduledWindow>> presched){
        Iterator<Map.Entry<String, List<PrescheduleObject.PrescheduledWindow>>> iterator = presched.entrySet().iterator();
        //TODO use builder here for teacher creation; we should use a default time here actually; lets update; CHECK UPDATE
        while (iterator.hasNext()) {
            Map.Entry<String, List<PrescheduleObject.PrescheduledWindow>> entry = iterator.next();
            String name = entry.getKey();
            BitSet prescheduleBs;

            try {
                prescheduleBs = createPreschedBs(entry.getValue());
                iterator.remove(); // safe removal while iterating
            } catch (Exception e) {
                LOGGER.info("Couldn't parse prescheduled times for '{}' because of error: '{}'",
                        name, e.getMessage());
                continue;
            }

            Teacher teacher = teacherMap.get(name);
            //create the teacher object with a default schedule to ensure their preschedule conflicts are taken into account
            if(teacher == null){
                LOGGER.warn("Couldn't find a teacher object for {} during prescheduling setup; creating one...", name);
                DefaultTime defaultTime = DefaultTimeRegistry.getRandomDefault();
                boolean isFaculty = Constants.FACULTY_LAST_NAMES.contains(name.split(",")[0].strip());
                TeacherBuilder builder = new TeacherBuilder(new DefaultTeachingPolicy());

                if(isFaculty){
                    LOGGER.info("promoting {} to faculty", name);
                    builder.setPolicy(new FacultyPolicy());
                }

                teacher = builder.preference(defaultTime.getPreference())
                        .acceptable(defaultTime.getAcceptable())
                        .conflict(defaultTime.getConflict())
                        .canon(name)
                        .gapPref(Preference.NEUTRAL)
                        .preschedule(prescheduleBs)
                        .build();

                teacherMap.put(name, teacher);
            }
            else{
                teacher.preschedule(prescheduleBs);
            }

        }

        if(!presched.isEmpty()){
            LOGGER.info("When updating teachers with their prescheduled conflicts, the following teachers " +
                    "had no corresponding object: {}", presched.keySet());
        }
    }

    public static BitSet createPreschedBs(List<PrescheduleObject.PrescheduledWindow> timeJson){
        BitSet bs = new BitSet();
        for(PrescheduleObject.PrescheduledWindow time: timeJson){
            bs.or(BitSetHelper.timeJsonToBs(time));
        }

        return bs;
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
