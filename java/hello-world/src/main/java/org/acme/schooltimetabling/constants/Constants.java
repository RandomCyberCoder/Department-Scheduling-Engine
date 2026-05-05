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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class should contain constants that are used throughout the program that don"t
 * fit anywhere else, like a class.
 */
public class Constants {
    /**
     * Global time formatter
     */
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mma");
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
     * Set of course configuration we only want to schedule
     */
    public static final Set<String> CHOSEN_CONFIGURATIONS;
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

        COURSE_CONFIGS = ParseInput.readCourseConfigs("constants/semester-configurations.tsv");

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

        SKIP_CONFIGURATIONS = Set.of();
        CHOSEN_CONFIGURATIONS = Set.of("3-1-0", "2-0-1", "1-0-0", "3-0-0", "4-0-0");

        /*Determine what rooms will be used for labs depending on department being
        * scheduled*/
        COURSE_TO_ROOMS = new HashMap<>();

        if(ScheduleConfig.getDepartment().equalsIgnoreCase("csc")){
            List<String> introCourses = List.of("csc1001", "csc2002", "csc2050");
            Set<String> introRooms = Set.of("Room 301", "Room 302", "Room 232A");

            //TODO non_sec_courses
            List<String> non_sec_courses = List.of("csc2050", "csc3300");
            Set<String> non_sec_rooms = Set.of("Room 301", "Room 302", "Room 232A", "Room 255", "Room 256", "Room 257",
                    "Room 20-127");

            List<String> graphicCourses = List.of("csc4710", "csc4730", "csc4740", "csc4760");
            Set<String> graphicRooms = Set.of("Room 255");

            List<String> securityCourses = List.of("csc3210", "csc4210", "csc4212", "csc4214", "csc4230");
            Set<String> securityRooms = Set.of("Room 192-206", "Room 192-333");

            List<String> phoenixCourses = List.of("csc3250");
            Set<String> phoenixRooms = Set.of("Room 192-333");

            List<String> hwSecurityCourses = List.of("csc5281");
            Set<String> hwSecurityRooms = Set.of("Room 192-206");

            List<String> seCourses = List.of("csc5281");
            Set<String> seRooms = Set.of("Room 192-333");

            introCourses.forEach(course -> COURSE_TO_ROOMS.put(course, introRooms));
            graphicCourses.forEach(course -> COURSE_TO_ROOMS.put(course, graphicRooms));
            securityCourses.forEach(course -> COURSE_TO_ROOMS.put(course, securityRooms));
            phoenixCourses.forEach(course -> COURSE_TO_ROOMS.put(course, phoenixRooms));
            hwSecurityCourses.forEach(course -> COURSE_TO_ROOMS.put(course, hwSecurityRooms));
            seCourses.forEach(course -> COURSE_TO_ROOMS.put(course, seRooms));
            non_sec_courses.forEach(course -> COURSE_TO_ROOMS.put(course, non_sec_rooms));

            POSSIBLE_ROOMS = Stream.of(
                            introRooms,
                            graphicRooms,
                            securityRooms,
                            phoenixRooms,
                            hwSecurityRooms,
                            seRooms,
                            non_sec_rooms
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
            List<String> microControllerCourses = List.of("cpe3160", "cpe4390");
            Set<String> microControllerRooms = Set.of("Room 20-132");

            List<String> capstoneCourses = List.of("cpe4260", "cpe4261");
            Set<String> capstoneRooms = Set.of("Room 20-145");

            List<String> generalCpeCourses = List.of("cpe2301", "cpe3300");
            Set<String> generalCpeRooms = Set.of("Room 20-132", "Room 20-100");

            List<String> roboticsCourses = List.of("cpe4160");
            Set<String> roboticsRooms = Set.of("Room 20-100", "Room 20-145");

            List<String> cscStyleCourses = List.of("cpe4190", "cpe4280", "cpe4420", "cpe4390", "cpe4669");
            Set<String> cscStyleRooms = Set.of("Room 20-121", "Room 14-303");

            microControllerCourses.forEach(course -> COURSE_TO_ROOMS.put(course, microControllerRooms));
            capstoneCourses.forEach(course -> COURSE_TO_ROOMS.put(course, capstoneRooms));
            generalCpeCourses.forEach(course -> COURSE_TO_ROOMS.put(course, generalCpeRooms));
            roboticsCourses.forEach(course -> COURSE_TO_ROOMS.put(course, roboticsRooms));
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
                            , capstoneCourses
                            , generalCpeCourses
                            , cscStyleCourses
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

        TEACHER_NAME_TO_CANON = getInstructorNameMapping("constants/name_mappings.xlsx");
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
                if(!teacherRecords.isEmpty()){
                    for(TeacherRecord record: teacherRecords){
                        instructorNameMapping.put(record.getNonCanon(), record.getCanon());
                    }
                    LOGGER.info("Succeeded generating teacher name mapping using DB");
                    apiSuccess = true;
                }
            } catch(Exception e){
                LOGGER.error("Failed to create teacher mapping using DB. Falling back to file based creation");
                instructorNameMapping = HashBiMap.create();
            }
        }

        //Read from files  if reading from api does not work, or we don't want to use it
        if(!apiSuccess){
            LOGGER.info("reading teacher name file");
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
                e.printStackTrace();
                Constants.LOGGER.error("Critical issue reading file containing mapping of instructor names");
                Constants.LOGGER.error(String.format("Error reading the file %s", resourceFilePath));
                System.exit(ParseInput.PROGRAM_FAILURE);
            }
        }

        return instructorNameMapping;
    }

    public static void load(){}
}
