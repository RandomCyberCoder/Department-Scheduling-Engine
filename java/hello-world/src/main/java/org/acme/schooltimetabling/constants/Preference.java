package org.acme.schooltimetabling.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Preference {

    AGREE, NEUTRAL, DISAGREE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Process.class);

    /**
     * <p>case insensitive maps ["", "disagree"] -> {@link #DISAGREE}; ["agree"] -> {@link #AGREE};
     * ["neutral"] -> {@link #NEUTRAL}</p>
     * <p>if the string doesn't match any of these options. The program will exit until resolved</p>
     * @param pref String representation of preference
     * @return enum representation or error/exits if no match is found
     */
    public static Preference parsePref(String pref){
        if("".equals(pref) || "disagree".equalsIgnoreCase(pref)) return Preference.DISAGREE;
        else if("agree".equalsIgnoreCase(pref)) return Preference.AGREE;
        else if("neutral".equalsIgnoreCase(pref)) return Preference.NEUTRAL;
        LOGGER.error("When reading a survey unknown preference was encountered '{}'.... EXITING", pref);
        System.exit(1);
        throw new IllegalStateException(String.format("Unknown preference '%s' encountered while reading survey", pref));
    }
}
