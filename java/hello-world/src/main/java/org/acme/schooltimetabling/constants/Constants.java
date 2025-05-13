package org.acme.schooltimetabling.constants;

import java.util.Map;

/**
 * This class is should contain constants that are used throughout the program that don't
 * fit anywhere else, like a class.
 */
public class Constants {
    public static final Map<String, String> SPECIAL_CODE_CONVERSION;
    public static final Map<String, String> SKIP_SCHEDULE;
    static{
        SPECIAL_CODE_CONVERSION = Map.ofEntries(
                Map.entry("",""),
                Map.entry("R", "Remote"),
                Map.entry("S2", "SecondSplit"),
                Map.entry("H", "HandSchedule"),
                Map.entry("M", "Double"),
                Map.entry("MM", "Triple")
        );

        SKIP_SCHEDULE = Map.ofEntries(
                Map.entry("Remote", ""),
                Map.entry("SecondSplit", ""),
                Map.entry("HandSchedule", ""),
                Map.entry("Double", ""),
                Map.entry("Triple", "")
        );
    }
}
