package org.acme.schooltimetabling.helperClasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;

public class From_other {

    private static final int PROGRAM_FAILURE = 1;
    public static final String YAML_FILE_PATH = "config.yaml";
    public static ScheduleConfig scheduleConfig;

    static {
        Yaml yaml = new Yaml();
        InputStream inputStream = null;
        try {
            // Load the YAML file
            inputStream = ParseInput.class.getClassLoader().getResourceAsStream(YAML_FILE_PATH);
            if (inputStream == null) {
                throw new FileNotFoundException("YAML file not found at " + YAML_FILE_PATH);
            }

            // Initialize scheduleConfig with the parsed YAML content
            scheduleConfig = yaml.loadAs(inputStream, ScheduleConfig.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<ScheduleFormat> readScheduleClasses() {
//        Yaml yaml = new Yaml();
//        InputStream inputStream;
//        try{
//            inputStream = ParseInput.class.getClassLoader().getResourceAsStream(YAML_FILE_PATH);
//            //inputStream = new FileInputStream(new File(TOML_FILE_PATH));
//            scheduleConfig = yaml.loadAs(inputStream, ScheduleConfig.class);
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }

        List<ScheduleFormat> parsedSchedules = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File("src/main/java/org/example/input/schedule-2254-CSC.json");
            parsedSchedules = objectMapper.readValue(file, new TypeReference<List<ScheduleFormat>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return parsedSchedules;
    }

    // Java code to illustrate reading a
// CSV file line by line
    public static ArrayList<HashMap<String, String>> readCSV(String file, ArrayList<String> replacementHeaders) throws FileNotFoundException {
        ArrayList<HashMap<String, String>> csvRead = new ArrayList<>();
        String line = null;
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

                System.out.print("----------- Reading a record -----------");
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

    public static ArrayList<ArrayList<String>> readTSV(String file) {
        ArrayList<ArrayList<String>> tsvRead = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(file)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ArrayList<String> parsedLine = (ArrayList<String>) Arrays.asList(line.split("\t"));
                tsvRead.add(parsedLine);
            }
        } catch (Exception e) {
            System.out.printf("Error trying read file \"%s\".... printing stack trace:\n", file);
            e.printStackTrace();
        }

        return tsvRead;
    }

    public static void readXLSX(String filePath) {
        try {

            // Reading file from local directory
            FileInputStream file = new FileInputStream(
                    new File(filePath));

            // Create Workbook instance holding reference to
            // .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);

            // Get first/desired sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(0);

            // Iterate through each rows one by one
            Iterator<Row> rowIterator = sheet.iterator();

            // Till there is an element condition holds true
            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();

                // For each row, iterate through all the
                // columns
                Iterator<Cell> cellIterator
                        = row.cellIterator();

                while (cellIterator.hasNext()) {

                    Cell cell = cellIterator.next();

                    // Checking the cell type and format
                    // accordingly
                    switch (cell.getCellType()) {
                        // Case 1
                        case NUMERIC:
                            System.out.print(
                                    cell.getNumericCellValue()
                                            + "t");
                            break;

                        // Case 2
                        case STRING:
                            System.out.print(
                                    cell.getStringCellValue()
                                            + "t");
                            break;
                    }
                }

                System.out.println("");
            }

            // Closing file output streams
            file.close();
        }

        // Catch block to handle exceptions
        catch (Exception e) {

            // Display the exception along with line number
            // using printStackTrace() method
            e.printStackTrace();
        }

    }
}
