package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
//TODO MAKE SURE THE THAT TIMESLOTS BEING GENERATED DON'T TAKE INTO ACCOUNT THE ENDING LIKE IF IT ENDS AT 11:00AM THAT THE 11AM ISN'T MARKED FOR THE LEC OR LAB/ACT BIT
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
        String timeStart = timeslotMap.get("time_start");
        String timeEnd = timeslotMap.get("time_end");
        float lectureHours = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("lecture_hours"))
                        .filter(s -> !s.isBlank())
                        .orElse("-1"));
        float totalHours = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("total_hours"))
                        .filter(s -> !s.isBlank())
                        .orElse("-1"));
        String labDays = Optional.ofNullable(timeslotMap.get("lab_days"))
                .filter(s -> !s.isBlank())
                .orElse("").strip();
        String labStart = timeslotMap.get("lab_start");
        String labEnd = timeslotMap.get("lab_end");
        float labHours = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("lab_hours"))
                        .filter(s -> !s.isBlank())
                        .orElse("-1")
        );

        try {
            newTimeslot = new Timeslot(ID, days, timeStart, timeEnd, lectureHours, totalHours,
                    labDays, labStart, labEnd, labHours);
        }
        catch (Exception e){
            LOGGER.error("Terminating program. Failed to generate a timeslot with the following (check format):\n " +
                    String.format("days: %s; timeStart: %s; timeEnd: %s; lecHours: %s; totalHours: %s; labDays: %s"
                            ,days, timeStart, timeEnd, lectureHours, totalHours, labDays));
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

        return newTimeslot;
    }
}
