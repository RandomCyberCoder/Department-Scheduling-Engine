package org.acme.schooltimetabling.constants;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public static final LinkedHashSet<String> POSSIBLE_ROOMS;
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

        /*Determine what rooms will be used for labs depending on department being
        * scheduled*/
        if(ParseInput.scheduleConfig.department.equalsIgnoreCase("csc")){
            POSSIBLE_ROOMS = new LinkedHashSet<>(Set.of(
                    "301", "302", "255", "256", "257", "20-127", "232A", "192-206",
                    "192-333", LEC_ONLY
            ));
        }
        else{
            /*These are the CPE lab rooms*/
            POSSIBLE_ROOMS = new LinkedHashSet<>(Set.of(
                    "20-100", "20-132", "14-303", "20-145", LEC_ONLY
            ));
        }

        counter = 1;
        ROOM_TO_ID_BIMAP = HashBiMap.create();
        for(String room: POSSIBLE_ROOMS){
            ROOM_TO_ID_BIMAP.put(room, counter++);
        }


        try{
            /*This will throw an error if the LEC_ONLY constant is not added
            * to the set of possible room; lecture room is needed*/
            LEC_ROOM_ONLY_ID = ROOM_TO_ID_BIMAP.get(LEC_ONLY);
        }
        catch (Exception e){
            LOGGER.error("Terminating Program. In Constants class include 'LEC_ONLY (variable) " +
                    "room to the LinkedHashSet exiting program until fixed");
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }
    }

    private Constants(){
        throw new UnsupportedOperationException("This class can't be instantiated");
    }
}
