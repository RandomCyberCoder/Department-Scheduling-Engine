package org.acme.schooltimetabling.constants;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * The unique ID of the room for lecture only courses*/
    public static int LEC_ROOM_ONLY_ID;
    /**
     * This set should contain all lab room names plus the
     * <i>LEC_ONLY</i> class attribute which is needed to
     * assign a room to lessons that are strictly lecture only
     */
    public static final Set<String> POSSIBLE_ROOMS;
    /**
     * This is a map that contains lab/act courses mapped to
     * lab/act rooms those courses are allowed to be in
     */
    public static final Map<String, Set<String>> COURSE_TO_ROOMS;
    public static final Set<String> STUDIO_STYLE_COURSES;
    /**
     * HashMap that maps the teacher name (format FIRST LAST) mapped
     * to the teacher canon name. The canon name is assumed to be the
     * one in the schedule json file and the teacher non-canon name
     * is the one found in the survey csv file*/
    public static final HashMap<String, String> TEACHER_NAME_TO_CANON;
    /**
     * BiMap containing rooms mapped to their unique IDs*/
    public static final BiMap<String, Integer> ROOM_TO_ID_BIMAP;

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
        /*These are the starter mapping we have but have to create more later*/
        /*Ideally all the mapping should be here or in some file that can be read from*/
        /*TODO beard just sent me a excel file with this data so ideally this should
        *  no longer be needed.... ideally*/
        TEACHER_NAME_TO_CANON = new HashMap<>(Map.ofEntries(
                Map.entry("BJ Klingenberg", "Klingenberg, Bernhard J."),
                Map.entry("James Mealy", "Mealy, Bryan J."),
                Map.entry("John Fox", "Fox, J. Kristofer"),
                Map.entry("Dave Parkinson", "Parkinson, David Shawn"),
                Map.entry("Lucas Pierce", "Pierce, Lucas Shane"),
                Map.entry("Kirk Duran", "Duran, Kirk Alberto"),
                Map.entry("Bret Hartman", "Hartman, Bret Andrew")
                ));

        /*TODO look at the studio_style_courses from Python code */
        /*Determine what rooms will be used for labs depending on department being
        * scheduled*/
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

            COURSE_TO_ROOMS = new HashMap<>();

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
        else{COURSE_TO_ROOMS = new HashMap<>();
            
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
    }

    private Constants(){
        throw new UnsupportedOperationException("This class can't be instantiated");
    }
}
