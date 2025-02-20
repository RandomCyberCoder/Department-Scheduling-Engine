package org.acme.schooltimetabling.helperClasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;

public class ParseInput {

    public static List<ScheduleFormat> readScheduleClasses(){
        List<ScheduleFormat> parsedSchedules = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File("java/hello-world/src/main/java/org/acme/schooltimetabling/input/schedule-2254-CSC.json");
            parsedSchedules = objectMapper.readValue(file, new TypeReference<List<ScheduleFormat>>() {});
        } catch (Exception e) {
            e.printStackTrace();
        }

        return parsedSchedules;
    }
}
