package org.acme.schooltimetabling.helperClasses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/***/
public final class ParseInput {
    /*value for failing */
    public static final int PROGRAM_FAILURE = 1;
    public static final String YAML_FILE_PATH = "constants/config.yaml";
    public static ScheduleConfig scheduleConfig;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseInput.class);

    static{
        try(InputStream inputStream = getResourceAsStream(YAML_FILE_PATH)) {

            // Initialize scheduleConfig with the parsed YAML content
            Yaml yaml = new Yaml();
            scheduleConfig = yaml.loadAs(inputStream, ScheduleConfig.class);

        } catch (Exception e) {
            LOGGER.error("Program is terminating. Couldn't read the yaml file");
            e.printStackTrace();
            System.exit(PROGRAM_FAILURE);
        }

    }

    private ParseInput(){
        throw new UnsupportedOperationException("This is a utility class an cannot be instantiated");
    }


    private static InputStream getResourceAsStream(String filePath){
        return ParseInput.class.getClassLoader().getResourceAsStream(filePath);
    }

    private static URL getResourceURL(String filePath){
        return ParseInput.class.getClassLoader().getResource(filePath);
    }

    /**
     * <p>Reads the JSON file containing information of what classes an instructor will teach </p>
     * <p>File format: array of objects</p>
     * <p> object format:</p>
     * <pre><code>
     *{"fall": ["csc103", "csc101"},
     *"name": "Teacher Name",
     *"spring": [&lt;class&gt;...],
     *"winter": [&lt;class&gt;...],}
     *</code></pre>
     * @param filePath path to CSV file
     * @return Schedule of classes
     */
    public static List<ScheduleFormat> readScheduleClasses(String filePath){
        List<ScheduleFormat> parsedSchedules = null;

        try (InputStream inputStream = ParseInput.getResourceAsStream(filePath)){

            ObjectMapper objectMapper = new ObjectMapper();
            parsedSchedules = objectMapper.readValue(inputStream, new TypeReference<List<ScheduleFormat>>() {});
        } catch (Exception e) {
            LOGGER.error("Terminating Program. Couldn't read the file containing classes that will be scheduled. ");
            e.printStackTrace();
            System.exit(PROGRAM_FAILURE);
        }

        return parsedSchedules;
    }

    /**
     * <p>Reads a csv and maps the old headers to the new headers. Each entry in the csv will be an
     *    item in the ArrayList returned where each item in the ArrayList is a HashMap that will map
     *    the column name, they key, to the value for entry in the csv. Warning will be given if error
     *    occurs opening or reading the file.
     * </p>
     *
     * @param filePath file path of the current quarter's instructor survey
     * @param replacementHeaders an ArrayList<String> of headers to replace the current csv headers
     *                           if you don't want replacement headers pass <code>null</code>
     * */
    public static ArrayList<HashMap<String, String>> readCSV(String filePath, ArrayList<String> replacementHeaders){
        ArrayList<HashMap<String, String>> csvRead = new ArrayList<>();
        boolean headerRead = false;

        try (Reader reader = new InputStreamReader(ParseInput.getResourceAsStream(filePath), StandardCharsets.UTF_8)) {
            CSVReader csvReader = new CSVReader(reader);
            String[] nextRecord;
            ArrayList<String> headers = null;

            while ((nextRecord = csvReader.readNext()) != null) {
                HashMap<String, String> mapRow = new HashMap<>();
                /*read the header*/
                if (!headerRead) {
                    /*check if user wants the default header of they gave us a new header to use*/
                    if (replacementHeaders == null) {
                        headers = Arrays.stream(nextRecord)
                                .map(String::trim).collect(Collectors.toCollection(ArrayList::new));
                    } else {
                        headers = replacementHeaders;
                    }
                    headerRead = true;
                    continue;
                }

                //System.out.println("----------- Reading a record -----------");
                Queue<String> headerStack = new LinkedList<>(headers);
                //headerStack.addAll(headers);
                int overFlow = 1;
                for (String cell : nextRecord) {
                    /*if we run out of headers we create sum for excess data to prevent
                     * data loss.
                     * */
                    if (headerStack.isEmpty()) {
                        String overFlowHeader = String.format("overFlow_%d", overFlow++);
                        mapRow.put(overFlowHeader, cell);
                    } else {
                        String nextHeaderKey = headerStack.remove();
                        mapRow.put(nextHeaderKey, cell);
                    }
                    //System.out.println(cell + "\t");
                }
                /*if the queue of headers is not empty we assign those headers an empty string
                value for the key-value mapping*/
                if (!headerStack.isEmpty()) {
                    for (String header : headerStack) {
                        mapRow.put(header, "");
                    }
                }
                //System.out.println();
                csvRead.add(mapRow);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read the csv file: " + filePath);
        }



        return csvRead;
    }

    /**
     * Reads the course configuration file given and returns the courses mapped
     * to their configurations. Both key and values will be strings
     *
     * @param filePath file path assuming its read as you're in the project directory
     * @return a hashmap with the course name as the key and the configuration as the value
     */
    public static HashMap<String, String> readCourseConfigs(String filePath){
        HashMap<String, String> courseConfigs = new HashMap<>();

        try(BufferedReader buf = new BufferedReader(new InputStreamReader(
                getResourceAsStream(filePath), StandardCharsets.UTF_8))){
            String lineRead = null;
            String[] lineProcessed;
            String course;
            String configuration;

            while(true){
                lineRead = buf.readLine();
                if(lineRead == null){
                    break;
                }
                lineProcessed = lineRead.split("\t");
                course = lineProcessed[0];
                configuration = lineProcessed[1];
                courseConfigs.put(course, configuration);
            }


        }
        catch (Exception e){
            LOGGER.error(String.format("Exiting program. Critical error. " +
                    "Couldn't read course config file '%s'", filePath));
            e.printStackTrace();
            System.exit(PROGRAM_FAILURE);
        }
        return courseConfigs;
    }

    /***
     * Assumes the file has a header. Assumes the file is a TSV. Reruns a set of faculty names.
     * @param file Path to file in the {@code resources} directory.
     * @return A set of faculty (tenure track) last names
     */
    public static Set<String> getFaculty(String file) {
        Set<String> faculty = new HashSet<>();
        final int NAME_POSITION = 0;
        final int TITLE_POSITION = 1;
        final int EMAIL_POSITION = 2;

        try (InputStream inputStream = getResourceAsStream(file);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));) {
            String line;
            boolean headerRead = false;
            while ((line = bufferedReader.readLine()) != null) {
                List<String> parsedLine = Arrays.asList(line.split("\t"));
                if(headerRead
                    && parsedLine.get(TITLE_POSITION).toLowerCase().contains("professor")){
                    String[] nameSplit = parsedLine.get(NAME_POSITION).split(" ");
                    faculty.add(nameSplit[nameSplit.length - 1]);
                }
                else{
                    headerRead = true;
                }
            }
        } catch (Exception e) {
            ParseInput.LOGGER.error("Error reading file with potential faculty names");
            ParseInput.LOGGER.error(String.format("Error trying read file \"%s\".... Terminating program until" +
                    " error is fixed", file));
            System.exit(ParseInput.PROGRAM_FAILURE);
        }

        /*Adding in names that aren't included in the file*/
        faculty.add("da Silva");
        faculty.add("DeBruhl II");
        faculty.add("De Moura Canaan");

        return faculty;
    }


}
