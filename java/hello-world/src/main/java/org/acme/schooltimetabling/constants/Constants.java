package org.acme.schooltimetabling.constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is should contain constants that are used throughout the program that don"t
 * fit anywhere else, like a class.
 */
public class Constants {
    /**
     * This class attribute is used for debug print statements. I kept it
     * from being constant to allow the flexibility to turn on/off debug print
     * statements as needed.
     */
    public static boolean DEBUG = true;
    public static final Map<String, String> SPECIAL_CODE_CONVERSION;
    public static final Set<String> SKIP_SCHEDULE;
    public static final HashMap<String, String> TEACHER_NAME_TO_CANON;
    static{
        SPECIAL_CODE_CONVERSION = Map.ofEntries(
                Map.entry("",""),
                Map.entry("R", "Remote"),
                Map.entry("S2", "SecondSplit"),
                Map.entry("H", "HandSchedule"),
                Map.entry("M", "Double"),
                Map.entry("MM", "Triple")
        );

        SKIP_SCHEDULE =  Set.of("Remote", "SecondSplit", "HandSchedule",
                "Double", "Triple");

        /*These are the starter mapping we have but have to create more later*/
        /*Ideally all the mapping should be here or in some file that can be read from*/
        TEACHER_NAME_TO_CANON = new HashMap<>(Map.ofEntries(
                Map.entry("BJ Klingenberg", "Klingenberg, Bernhard J."),
                Map.entry("James Mealy", "Mealy, Bryan J."),
                Map.entry("John Fox", "Fox, J. Kristofer"),
                Map.entry("Dave Parkinson", "Parkinson, David Shawn"),
                Map.entry("Lucas Pierce", "Pierce, Lucas Shane"),
                Map.entry("Kirk Duran", "Duran, Kirk Alberto"),
                Map.entry("Bret Hartman", "Hartman, Bret Andrew")
                ));

    }
}
