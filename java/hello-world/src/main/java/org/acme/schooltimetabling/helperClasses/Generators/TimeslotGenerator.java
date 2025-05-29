package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.helperClasses.ParseInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class TimeslotGenerator extends Generator{
    private static Timeslot generateTimeslot(int ID, HashMap<String, String> timeslotMap){
        Timeslot newTimeslot = null;
        String days = timeslotMap.get("days");
        String timeStart = timeslotMap.get("time_start");
        String timeEnd = timeslotMap.get("time_end");
        float lectureHours = Float.parseFloat(timeslotMap.get("lecture_hours"));
        float totalHours = Float.parseFloat(timeslotMap.get("total_hours"));
        String days2 = timeslotMap.get("days2");
        String timeStart2 = timeslotMap.get("time_start2");
        String timeEnd2 = timeslotMap.get("time_end2");
        /*if the there is a valid entry for the lecture_hours2 or total_hours2 we use the value
        * if there isn't a valid value we just generate a random one because this second timeslot
        * isn't meant to be used*/
        float lecture_hours2 = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("lecture_hours2"))
                        .filter(s -> !s.isBlank())
                        .orElse(String.valueOf(Math.random() * 10))
        );
        float total_hours2 = Float.parseFloat(
                Optional.ofNullable(timeslotMap.get("total_hours2"))
                        .filter(s -> !s.isBlank())
                        .orElse(String.valueOf(Math.random() * 10))
        );

        try {
            newTimeslot = new Timeslot(ID, days, timeStart, timeEnd, lectureHours, totalHours,
                    days2, timeStart2, timeEnd2, lecture_hours2, total_hours2);
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }
        return newTimeslot;
    }

    /**
     * <p>Generates all timeslots for the problem setup</p>
     *
     * @param filePath Path to the file. Path assumes you are in the org.acme.schooltimetabling package
     * @return returns a list of TimeSlots create from the file provided
     */
    public static ArrayList<Timeslot> generateTimeslots(String filePath){
        int timeslotID = 0;
        //parse csv
        String timeslotsFile = filePath;
        ArrayList<Timeslot> timeslotList = new ArrayList<>();
        ArrayList<HashMap<String, String>> timeslotCSV= null;
        try{
            timeslotCSV = ParseInput.readCSV(timeslotsFile, null);
            if(timeslotCSV.isEmpty()){
                throw new Exception("timeslot list is empty");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(ParseInput.PROGRAM_FAILURE);
        }
        //loop through entries
        for(HashMap<String, String > timeslotMap: timeslotCSV) {
            //instantiate a timeslot instance for every entry
            Timeslot newTimeslot = generateTimeslot(timeslotID++, timeslotMap);
            //add the timeslot instant to our list
            timeslotList.add(newTimeslot);
        }
        return timeslotList;
    }
}
