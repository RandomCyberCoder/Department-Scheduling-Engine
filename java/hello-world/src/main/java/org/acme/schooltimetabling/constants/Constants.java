package org.acme.schooltimetabling.constants;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.apiCalls.teacherEndpoint.TeacherCalls;
import org.acme.schooltimetabling.apiCalls.teacherEndpoint.TeacherRecord;
import org.acme.schooltimetabling.helperClasses.Generators.Generator;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class should contain constants that are used throughout the program that don"t
 * fit anywhere else, like a class.
 */
public class Constants {
    /**
    * Set to True for testing. Helps by pass some checks
    * */
    public static boolean TESTING = false;
    /**
     * Needed for logging
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
    /**
     * This class attribute is used for debug print statements. I kept it
     * from being constant to allow the flexibility to turn on/off debug print
     * statements as needed.
     */
    public static boolean DEBUG = true;
    /**
     * Course names mapped to their respective course configuration
     * @see ParseInput#readCourseConfigs(String)
     */
    public static final HashMap<String, String> COURSE_CONFIGS;
    /**
     * Maps a course to a unique ID, this ID will never change. i.e. csc101 -> 1;
     */
    public static final BiMap<String, Integer> COURSE_ID_BIMAP;
    /**
     * Map of abbreviated course modifiers mapped to non-abbreviated course
     * modifiers
     */
    public static final Map<String, String> SPECIAL_CODE_CONVERSION;
    /**
     * Set of course non-abbreviated modifiers that should be skipped*/
    public static final Set<String> SKIP_SCHEDULE;
    /**
     * Set of course configurations that should be skipped
     */
    public static final Set<String> SKIP_CONFIGURATIONS;
    /**
     * Lecture only room name
     */
    public static final String LEC_ONLY = "LECTURE_ONLY";
    /**
     * This set should contain all lab room names plus the
     * <i>LEC_ONLY</i> class attribute which is needed to
     * assign a room to lessons that are strictly lecture only
     */
    //TODO switch to IDs? this const is used during solving
    public static final Set<String> POSSIBLE_ROOMS;
    /**
     * Contains lab/act course names mapped to
     * lab/act rooms those courses are allowed to be in. Courses
     * included should be those that have lab/act room restrictions
     */
    //TODO we should change this to use the courseID and roomID this const is used during solving
    public static final Map<String, Set<String>> COURSE_TO_ROOMS;
    /**
     * BiMap containing room names mapped to their unique IDs
     * */
    public static final BiMap<String, Integer> ROOM_TO_ID_BIMAP;
    /**
     * Set of studio style course names
     * */
    //TODO switch to IDs? this const is used during solving
    public static final Set<String> STUDIO_STYLE_COURSES;
    /**
     * HashMap that maps the teacher name (ex. format FIRST LAST) mapped
     * to the teacher canon name. The canon name is assumed to be the
     * one in the schedule json file and the teacher non-canon name
     * is the one found in the survey csv file
     * */
    public static final BiMap<String, String> TEACHER_NAME_TO_CANON;

    /**
     * A set of the last names of faculty members
     * */
    public static Set<String> FACULTY_LAST_NAMES;

    static{
        int counter;

        COURSE_CONFIGS = ParseInput.readCourseConfigs("constants/configurations.tsv");

        COURSE_ID_BIMAP = Generator.genCourseToIdMapping(COURSE_CONFIGS.keySet().iterator());

        SPECIAL_CODE_CONVERSION = Map.ofEntries(
                Map.entry("",""),
                Map.entry("R", "Remote"),
                Map.entry("S", "Split"),
                Map.entry("S2", "SecondSplit"),
                Map.entry("H", "HandSchedule"),
                Map.entry("M", "Double"),
                Map.entry("MM", "Triple")
        );

        SKIP_SCHEDULE =  Set.of("Remote", "SecondSplit", "HandSchedule",
                "Double", "Triple");

        SKIP_CONFIGURATIONS = Set.of("various", "non-standard", "0-0-2");

        /*Determine what rooms will be used for labs depending on department being
        * scheduled*/
        COURSE_TO_ROOMS = new HashMap<>();

        if(ScheduleConfig.getDepartment().equalsIgnoreCase("csc")){
            List<String> introCourses = List.of("csc101", "csc202", "csc203", "csc357");
            Set<String> introRooms = Set.of("301", "302", "232A");

            List<String> graphicCourses = List.of("csc474", "csc476", "csc378", "csc582");
            Set<String> graphicRooms = Set.of("255");

            List<String> securityCourses = List.of("csc320", "csc321", "csc421", "csc521");
            Set<String> securityRooms = Set.of("192-206", "192-333");

            List<String> phoenixCourses = List.of("csc325");
            Set<String> phoenixRooms = Set.of("192-333");

            List<String> hwSecurityCourses = List.of("csc524");
            Set<String> hwSecurityRooms = Set.of("192-206");

            List<String> seCourses = List.of("csc305", "csc307", "csc309", "csc402", "csc405", "csc406");
            Set<String> seRooms = Set.of("256");

            List<String> uiCourses = List.of("csc484");
            Set<String> uiRooms = Set.of("257");


            introCourses.forEach(course -> COURSE_TO_ROOMS.put(course, introRooms));
            graphicCourses.forEach(course -> COURSE_TO_ROOMS.put(course, graphicRooms));
            securityCourses.forEach(course -> COURSE_TO_ROOMS.put(course, securityRooms));
            phoenixCourses.forEach(course -> COURSE_TO_ROOMS.put(course, phoenixRooms));
            hwSecurityCourses.forEach(course -> COURSE_TO_ROOMS.put(course, hwSecurityRooms));
            seCourses.forEach(course -> COURSE_TO_ROOMS.put(course, seRooms));
            uiCourses.forEach(course -> COURSE_TO_ROOMS.put(course, uiRooms));

            POSSIBLE_ROOMS = Stream.of(
                            introRooms,
                            graphicRooms,
                            securityRooms,
                            phoenixRooms,
                            hwSecurityRooms,
                            seRooms,
                            uiRooms
                    )
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            //All studio style course for the CSC department go here
            //Enter them as a list into the Stream.of() as a parameter
            STUDIO_STYLE_COURSES = Stream.of(
                    List.<String>of()
                    )
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

        }
        else{
            List<String> microControllerCourses = List.of("cpe316", "cpe439");
            Set<String> microControllerRooms = Set.of("20-132");

            List<String> capstoneCourses = List.of("cpe350", "cpe450");
            Set<String> capstoneRooms = Set.of("20-145");

            List<String> generalCpeCourses = List.of(
                    "cpe133", "cpe233", "cpe333", "cpe414", "cpe416",
                    "cpe442", "cpe446", "cpe521", "cpe522", "cpe523", "cpe542"
            );
            Set<String> generalCpeRooms = Set.of("20-132", "20-100", "14-303");

            List<String> cscStyleCourses = List.of("cpe225", "cpe315", "cpe321", "cpe426", "cpe515");
            Set<String> cscStyleRooms = Set.of("14-303");

            microControllerCourses.forEach(course -> COURSE_TO_ROOMS.put(course, microControllerRooms));
            capstoneCourses.forEach(course -> COURSE_TO_ROOMS.put(course, capstoneRooms));
            generalCpeCourses.forEach(course -> COURSE_TO_ROOMS.put(course, generalCpeRooms));
            cscStyleCourses.forEach(course -> COURSE_TO_ROOMS.put(course, cscStyleRooms));

            POSSIBLE_ROOMS = Stream.of(
                            microControllerRooms,
                            capstoneRooms,
                            generalCpeRooms,
                            cscStyleRooms
                    )
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            //All studio style course for the CSC department go here
            //Enter them as a list into the Stream.of() as a parameter
            STUDIO_STYLE_COURSES = Stream.of(
                    microControllerCourses
                    , generalCpeCourses
                    , capstoneCourses
//                    , cscStyleCourses
                    )
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }

        POSSIBLE_ROOMS.add(LEC_ONLY);

        counter = 1;
        ROOM_TO_ID_BIMAP = HashBiMap.create();
        for(String room: POSSIBLE_ROOMS){
            ROOM_TO_ID_BIMAP.put(room, counter++);
        }


        if(!ROOM_TO_ID_BIMAP.containsKey(LEC_ONLY)){
            LOGGER.error("TERMINATING. In Constants class, include 'LEC_ONLY' (constant) " +
                    "room to constant 'POSSIBLE_ROOMS'");
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

        FACULTY_LAST_NAMES = ParseInput.getFaculty("constants/faculty_website_list.tsv");

        TEACHER_NAME_TO_CANON = getInstructorNameMapping("constants/faculty_names_use.xlsx");
    }

    private Constants(){
        throw new UnsupportedOperationException("This class can't be instantiated");
    }

    /**
     *
     * @param filePath path to file in the <i>resources</i> directory
     * @return A stream for the given file
     */
    private static InputStream getResourceAsStream(String filePath){
        return ParseInput.class.getClassLoader().getResourceAsStream(filePath);
    }

    /**
     * <p>Assumes there is a header and that the second cell in a row is the "name" and that
     * the third name is the "canon" name.</p>
     * <p>BiMap returned is in the format non-canon -> canon. Meaning the reverse BiMap
     * is in the format canon -> non-canon</p>
     * @param resourceFilePath file path to Excel file. Assumes column order is email
     * @return BiMap of instructor's names, non-canon -> canon.
     */
    static private BiMap<String, String> getInstructorNameMapping(String resourceFilePath){
        BiMap<String, String> instructorNameMapping = HashBiMap.create();
        boolean apiSuccess = false;
        if(ScheduleConfig.isUseApi()){
            LOGGER.info("Attempting to read teacher records from DB");
            try{
                List<TeacherRecord> teacherRecords = TeacherCalls.getAllTeachers();
                for(TeacherRecord record: teacherRecords){
                    instructorNameMapping.put(record.getNonCanon(), record.getNonCanon());
                }
                LOGGER.info("Succeeded generating teacher name mapping using DB");
                return instructorNameMapping;
            } catch(Exception e){
                LOGGER.error("Failed to create teacher mapping using DB. Falling back to file based creation");
                instructorNameMapping = HashBiMap.create();
            }
        }

        final int NAME_CELL_POS = 1;
        final int CANON_CELL_POS = 2;
        boolean headerRead = false;

        try(InputStream inputStream = getResourceAsStream(resourceFilePath);){
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if(!headerRead){
                    headerRead = true;
                    continue;
                }
                String nonCanonName = row.getCell(NAME_CELL_POS).getStringCellValue().strip();
                String canonName = row.getCell(CANON_CELL_POS).getStringCellValue().strip();
                instructorNameMapping.put(nonCanonName, canonName);
            }
        }
        catch (Exception e){
            Constants.LOGGER.error("Critical issue reading file containing mapping of instructor names");
            Constants.LOGGER.error(String.format("Error reading the file %s", resourceFilePath));
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

        return instructorNameMapping;
    }
}
