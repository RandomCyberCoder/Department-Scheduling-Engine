package org.acme.schooltimetabling.helperClasses;

import org.slf4j.LoggerFactory;
import java.io.InputStream;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * <p>This is a singleton class that will hold the configuration once it is loaded in
 * using {@link #loadConfig(String)}</p>
 */
public class ScheduleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleConfig.class);
    public String department;
    public String curTerm;
    public String prevTerm;
    public String seasonTerm;
    public boolean useApi;

    private static class HOLDER{
        private static ScheduleConfig scheduleConfig = null;
    }
    private ScheduleConfig(){}

    /**
     * Loads the configuration for the solver
     * @param yamlPath path to the yaml file in the resources directory
     */
    public static void loadConfig(String yamlPath){
        try(InputStream inputStream = ParseInput.getResourceAsStream(yamlPath)){
            Yaml yaml = new Yaml();
            HOLDER.scheduleConfig = yaml.loadAs(inputStream, ScheduleConfig.class);
        } catch (Exception e){
            final int PROGRAM_FAILURE = 1;
            LOGGER.error("Program is terminating. Couldn't read the yaml file");
            LOGGER.error(String.format("Program assumes yaml file is located at '%s' int the resources directory",
                    yamlPath));
            LOGGER.error(String.format("Related error: %s", e.getMessage()));
            System.exit(PROGRAM_FAILURE);
        }
        return;
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

    public static boolean isUseApi(){
        if(HOLDER.scheduleConfig == null) throw new IllegalStateException("Configuration must be loaded");
        return HOLDER.scheduleConfig.useApi;
    }
}
