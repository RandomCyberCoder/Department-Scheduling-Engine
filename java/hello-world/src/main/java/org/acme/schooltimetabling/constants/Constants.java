package org.acme.schooltimetabling.constants;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class should contain constants that are used throughout the program that don"t
 * fit anywhere else, like a class.
 */
public class Constants {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
    /**
     * This class attribute is used for debug print statements. I kept it
     * from being constant to allow the flexibility to turn on/off debug print
     * statements as needed.
     */
    public static boolean DEBUG = true;
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
     * name of room that will be used for courses with that only
     * have a lecture
     */
    public static final String LEC_ONLY = "LECTURE_ONLY";
    /**
     * This set should contain all lab room names plus the
     * <i>LEC_ONLY</i> class attribute which is needed to
     * assign a room to lessons that are strictly lecture only
     */
    public static final Set<String> POSSIBLE_ROOMS;
    /* TODO explore using ids for the rooms in the COURSE_TO_ROOMS map
     *   leave out for now. Might be bad for maintainability*/
    /**
     * This is a map that contains lab/act courses mapped to
     * lab/act rooms those courses are allowed to be in
     */
    public static final Map<String, Set<String>> COURSE_TO_ROOMS;
    /**
     * BiMap containing rooms mapped to their unique IDs*/
    public static final BiMap<String, Integer> ROOM_TO_ID_BIMAP;

    public static final Set<String> STUDIO_STYLE_COURSES;
    /**
     * HashMap that maps the teacher name (format FIRST LAST) mapped
     * to the teacher canon name. The canon name is assumed to be the
     * one in the schedule json file and the teacher non-canon name
     * is the one found in the survey csv file*/
    public static final BiMap<String, String> NEW_TEACHER_NAME_TO_CANON;

    /**
     * A set of the last names of faculty members*/
    public static Set<String> FACULTY_LAST_NAMES;

    static{
        int counter;

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

        if(ParseInput.scheduleConfig.department.equalsIgnoreCase("csc")){
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

            STUDIO_STYLE_COURSES = Set.of();

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

            STUDIO_STYLE_COURSES = Stream.of(
                    microControllerRooms,
                    generalCpeCourses,
                    capstoneCourses
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

        NEW_TEACHER_NAME_TO_CANON = getInstructorNameMapping("constants/faculty_names_use.xlsx");
    }

    private Constants(){
        throw new UnsupportedOperationException("This class can't be instantiated");
    }


    private static InputStream getResourceAsStream(String filePath){
        return ParseInput.class.getClassLoader().getResourceAsStream(filePath);
    }

    /**
     * <p>Assumes there is a header and that the second cell in a row is the "name" and that
     * the third name is the "canon" name.</p>
     * <p>BiMap returned is in the format non-canon -> canon. Meaning the reverse BiMap
     * is in the format canon -> non-canon</p>
     * @param resourceFilePath
     * @return BiMap of instructor's names, non-canon -> canon.
     */
    static private BiMap<String, String> getInstructorNameMapping(String resourceFilePath){
        BiMap<String, String> instructorNameMapping = HashBiMap.create();
//        resourceFilePath = "constants/faculty_names_use.xlsx";
        //zero indexed
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
