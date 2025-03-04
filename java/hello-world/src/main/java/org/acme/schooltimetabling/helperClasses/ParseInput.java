package org.acme.schooltimetabling.helperClasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class ParseInput {
    /*value for failing */
    private static final int PROGRAM_FAILURE = 1;

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

    /**
     * <p>Reads a csv and maps the old headers to the new headers. Each entry in the csv will be an
     *    item in the ArrayList returned where each item in the ArrayList is a HashMap that will map
     *    the column name, they key, to the value for entry in the csv
     * </p>
     *
     * @param file file path of the current quarter's instructor survey
     * @param replacementHeaders an ArrayList<String> of headers to replace the current csv headers
     *                           if you don't want replacement headers pass <code>null</code>
     * */
    public static ArrayList<HashMap<String, String>> readCSV(String file, ArrayList<String> replacementHeaders) throws FileNotFoundException {
        ArrayList<HashMap<String, String>> csvRead = new ArrayList<>();
        boolean headerRead = false;

        try (FileReader fileReader = new FileReader(file)) {
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;
            ArrayList<String> headers = null;

            while ((nextRecord = csvReader.readNext()) != null) {
                HashMap<String, String> mapRow = new HashMap<>();
                /*read the header*/
                if (!headerRead) {
                    /*check if user wants the default header of they gave us a new header to use*/
                    if (replacementHeaders == null) {
                        headers = (ArrayList<String>) Arrays.asList(nextRecord);
                    } else {
                        headers = replacementHeaders;
                    }
                    headerRead = true;
                }

                //System.out.println("----------- Reading a record -----------");
                Stack<String> headerStack = new Stack<>();
                headerStack.addAll(headers);
                int overFlow = 1;
                for (String cell : nextRecord) {
                    /*if we run out of headers we create sum for excess data to prevent
                     * data loss.
                     * */
                    if (headerStack.empty()) {
                        String overFlowHeader = String.format("overFlow_%d", overFlow++);
                        mapRow.put(overFlowHeader, cell);
                    } else {
                        String nextHeaderKey = headerStack.pop();
                        mapRow.put(nextHeaderKey, cell);
                    }
                    //System.out.println(cell + "\t");
                }
                /*if the queue of headers is not empty we assign those headers an empty string
                value for the key-value mapping*/
                if (!headerStack.empty()) {
                    for (String header : headerStack) {
                        mapRow.put(header, "");
                    }
                }
                //System.out.println();
                csvRead.add(mapRow);
            }
        } catch (Exception e) {
            System.out.println("Had trouble reading csv file: " + file);
            e.printStackTrace();
            System.exit(PROGRAM_FAILURE);
        }

        return csvRead;
    }
}
