package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class TimeslotGenerator extends Generator{
    private final static Logger LOGGER = LoggerFactory.getLogger(TimeslotGenerator.class);

    /**
     * <p>Generates all timeslots for the problem setup</p>
     *
     * @param filePath Path to the file. Assumes the file is in the resources directory
     * @return returns a list of TimeSlots create from the file provided
     */
    public static ArrayList<Timeslot> generateTimeslots(String filePath){
        int timeslotID = 0;
        //parse csv
        ArrayList<Timeslot> timeslotList = new ArrayList<>();
        ArrayList<HashMap<String, String>> timeslotCSV= null;

        timeslotCSV = ParseInput.readCSV(filePath, null);
        if(timeslotCSV.isEmpty()){
            LOGGER.error("Terminating program. Timeslot list is empty. Nothing to schedule");
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

        //loop through entries
        for(HashMap<String, String> timeslotMap: timeslotCSV) {
            //instantiate a timeslot instance for every entry
            Timeslot newTimeslot = generateTimeslot(timeslotID++, timeslotMap);
            //add the timeslot instant to our list
            timeslotList.add(newTimeslot);
        }

        LOGGER.info("Finished generating timeslots");

        return timeslotList;
    }


    private static Timeslot generateTimeslot(int ID, HashMap<String, String> timeslotMap){
        Timeslot newTimeslot = null;
        String days = Optional.ofNullable(timeslotMap.get("days"))
                .filter(s -> !s.isBlank())
                .orElse("").strip();
        String startTime = timeslotMap.get("startTime");
        String endTime = timeslotMap.get("endTime");
        float startHours = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("startHours"))
                        .filter(s -> !s.isBlank())
                        .orElse("-1"));
        float totalHours = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("totalHours"))
                        .filter(s -> !s.isBlank())
                        .orElse("-1"));
        String days2 = Optional.ofNullable(timeslotMap.get("days2"))
                .filter(s -> !s.isBlank())
                .orElse("").strip();
        String startTime2 = timeslotMap.get("startTime2");
        String endTime2 = timeslotMap.get("endTime2");
        float totalHours2 = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("totalHours2"))
                        .filter(s -> !s.isBlank())
                        .orElse("-1")
        );

        try {
            newTimeslot = new Timeslot(ID, days, startTime, endTime, startHours, totalHours,
                    days2, startTime2, endTime2, totalHours2);
        }
        catch (Exception e){
            LOGGER.error("Terminating program. Failed to generate a timeslot with the following (check format):\n " +
                    String.format("days: %s; startTime: %s; endTime: %s; startHours: %s; totalHours: %s; days2: %s"
                            ,days, startTime, endTime, startHours, totalHours, days2));
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

        return newTimeslot;
    }
}
