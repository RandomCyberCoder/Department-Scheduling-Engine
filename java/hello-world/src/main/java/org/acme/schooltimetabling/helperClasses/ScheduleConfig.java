package org.acme.schooltimetabling.helperClasses;

import org.acme.schooltimetabling.constants.Days;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

/**
 * <p>This is a singleton class that will hold the configuration once it is loaded in
 * using {@link #loadConfig(String)}</p>
 */
public class ScheduleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleConfig.class);
    //------------ values read in from yaml file ------------
    public String department;
    public String curTerm;
    public String prevTerm;
    public   String seasonTerm;
    public boolean aggressiveSolver;
    public boolean testing;
    public boolean useApi;
    public String compressStart;
    public String compressEnd;
    //------------ values calculated ------------
    private BitSet cpmrsInBs;
    private BitSet cmprsOutBs;

    private static class HOLDER{
        private static ScheduleConfig scheduleConfig = null;
    }
    private ScheduleConfig(){}

    /**
     * <p>Takes in a time and rounds it to the nearest half hour; 0-14 min -> down to x:00; 15-44 -> x:30;
     * 45-59 -> x+1:00</p>
     * <p>The specification for the times given will require the time to be at a half hour but this is mostly
     * just for my peace of mind</p>
     * @param time time to normalize
     * @return time rounded to the nearest half hour
     */
    private static LocalTime roundToNearestHalfHour(LocalTime time) {
        int minutes = time.getMinute();

        // Determine nearest half hour: 0 or 30
        int roundedMinutes;
        if (minutes < 15) {
            roundedMinutes = 0;
        } else if (minutes < 45) {
            roundedMinutes = 30;
        } else {
            // If 45+ minutes, round up to next hour
            roundedMinutes = 0;
            time = time.plusHours(1);
        }

        return LocalTime.of(time.getHour(), roundedMinutes);
    }

    /**
     * Loads the configuration for the solver
     * @param yamlPath path to the yaml file in the resources directory
     */
    public static void loadConfig(String yamlPath){
        try(InputStream inputStream = ParseInput.getResourceAsStream(yamlPath)){
            Yaml yaml = new Yaml();
            HOLDER.scheduleConfig = yaml.loadAs(inputStream, ScheduleConfig.class);

            //BitSet setup for times we want to compress into
            EnumSet<Days> MTWRF = EnumSet.allOf(Days.class);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
            LocalTime startTime = LocalTime.parse(HOLDER.scheduleConfig.compressStart, formatter);
            LocalTime endTime = LocalTime.parse(HOLDER.scheduleConfig.compressEnd, formatter);
            //normalize times
            startTime = roundToNearestHalfHour(startTime);
            endTime = roundToNearestHalfHour(endTime);
            if(startTime.isAfter(endTime)){
                LOGGER.error("The start time for the compressed start is after the end time; Exiting program");
                System.exit(1);
            }
            //get number of 30 minute blocks
            long numBlcks = Duration.between(startTime,endTime).toMinutes() / 30;
            final int bsSizeRep = 150;
            BitSet buildCmprsIn = new BitSet(bsSizeRep);
            buildCmprsIn.or(BitSetHelper.timeSlotBitSet(startTime, (int) numBlcks, MTWRF));
            BitSet buildCmprsOut = (BitSet) buildCmprsIn.clone();
            buildCmprsOut.flip(0, bsSizeRep);

            HOLDER.scheduleConfig.cpmrsInBs = buildCmprsIn;
            HOLDER.scheduleConfig.cmprsOutBs = buildCmprsOut;
        } catch (Exception e){
            final int PROGRAM_FAILURE = 1;
            LOGGER.error("Program is terminating. Couldn't read the yaml file");
            LOGGER.error(String.format("Program assumes yaml file is located at '%s' int the resources directory",
                    yamlPath));
            LOGGER.error(String.format("Related error: %s", e.getMessage()));
            System.exit(PROGRAM_FAILURE);
        }
    }

    public static String getDepartment(){
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.department;
    }

    public static String getCurTerm(){
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.curTerm;
    }

    public static String getPrevTerm(){
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.prevTerm;
    }

    public static String getSeasonTerm(){
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.seasonTerm;
    }

    public static boolean isAggressiveSolver() {
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.aggressiveSolver;
    }

    public static boolean isTesting() {
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.testing;
    }

    public static boolean isUseApi(){
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.useApi;
    }

    public static BitSet getCompressInMask() {
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.cpmrsInBs;
    }

    public static BitSet getCompressOutMask() {
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.cmprsOutBs;
    }
}
