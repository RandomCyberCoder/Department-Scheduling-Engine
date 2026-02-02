package org.acme.schooltimetabling.helperClasses;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Timetable;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ResultSaver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultSaver.class);
    private static final DateTimeFormatter LOCALTIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma");
    private static final int COLUMN_SPACING = 5;
    private static final int START_COLUMN = 1;
    private static final Map<String, Integer> ROOM_COL_MAP;
    private static final Map<LocalTime, Integer> TIME_ROW_MAP;
    private Map<String, Integer> TEACHER_COL_MAP;
    private Timetable solToPrint = null;
    private CellStyle cellStyle = null;

    static {
        ROOM_COL_MAP = new HashMap<>();
        int colIdx = 0;
        for(String roomName: Constants.POSSIBLE_ROOMS){
            if(Objects.equals(roomName, Constants.LEC_ONLY)) continue;
            ROOM_COL_MAP.put(roomName, colIdx++);
        }

        //first two rows (0 and 1) are for the headers
        TIME_ROW_MAP = new HashMap<>();
        int timeRowIdx = 2;
        final LocalTime endTime = LocalTime.parse("10:00PM", LOCALTIME_FORMATTER);
        LocalTime localTime = LocalTime.parse("7:00AM", LOCALTIME_FORMATTER);
        while(localTime.isBefore(endTime)){
            TIME_ROW_MAP.put(localTime, timeRowIdx++);
            localTime = localTime.plusMinutes(30);
        }
    }
    public ResultSaver(Timetable solution){
        solToPrint = solution;
        buildTeacherMap();
    }

    public void useNewSolution(Timetable solution){
        solToPrint = solution;
        buildTeacherMap();
    }

    private void buildTeacherMap(){
        List<Lesson> lessonList = solToPrint.getLessons();
        int rowNum = 0;
        TEACHER_COL_MAP = new HashMap<>();
        for(Lesson lesson: lessonList){
            if(!TEACHER_COL_MAP.containsKey(lesson.getTeacherObj().getName())){
                TEACHER_COL_MAP.put(lesson.getTeacherObj().getName(), rowNum++);
            }
        }
    }

    public void saveSolution(){
        final XSSFWorkbook workbook = new XSSFWorkbook();
        cellStyle = workbook.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        final XSSFSheet roomSheet = workbook.createSheet("Room Usage");
        final XSSFSheet teacherSheet = workbook.createSheet("Teacher schedule");
        final XSSFSheet lessonListSheet = workbook.createSheet("List View");

        roomView(roomSheet);
        teacherView(teacherSheet);
        listView(lessonListSheet);

        try{
            saveSolution(workbook);
        }
        catch (Exception e){
            LOGGER.error(String.format("Unable to save output after retries :( . Related error: %s", e.getMessage()));

        }
    }

    private void listView(XSSFSheet listSheet){
        List<Lesson> lessonList = solToPrint.getLessons();
        final int COURSE_NAME_COL = 0;
        final int SECTION_NUM_COL = 1;
        final int INSTRUCTOR_COL = 2;
        final int ROOM_COL = 3;
        final int DAYS_COL = 4;
        final int START_TIME_COL = 5;
        final int END_TIME_COL = 6;
        Row headerRow = listSheet.createRow(0);
        Cell cell;

        cell = headerRow.createCell(COURSE_NAME_COL);
        cell.setCellValue("Course");
        listSheet.setColumnWidth(COURSE_NAME_COL,4000);

        cell = headerRow.createCell(SECTION_NUM_COL);
        cell.setCellValue("Section Number");
        listSheet.setColumnWidth(SECTION_NUM_COL, 4000);

        cell = headerRow.createCell(INSTRUCTOR_COL);
        cell.setCellValue("Instructor");
        listSheet.setColumnWidth(INSTRUCTOR_COL, 4000);

        cell = headerRow.createCell(ROOM_COL);
        cell.setCellValue("Room");
        listSheet.setColumnWidth(ROOM_COL, 4000);

        cell = headerRow.createCell(DAYS_COL);
        cell.setCellValue("Days");
        listSheet.setColumnWidth(DAYS_COL, 4000);

        cell = headerRow.createCell(START_TIME_COL);
        cell.setCellValue("Start time");
        listSheet.setColumnWidth(START_TIME_COL, 4000);


        cell = headerRow.createCell(END_TIME_COL);
        cell.setCellValue("End time");
        listSheet.setColumnWidth(END_TIME_COL, 4000);

        int rowIdx = 1;
        for(Lesson lesson: lessonList){
            final Timeslot lsTs = lesson.getTimeslot();
            if(lesson.isHasLecture()){
                Row row = listSheet.createRow(rowIdx++);
                Object[] vals = new Object[]{lesson.getCourseName(), lesson.getLecSection(), lesson.getTeacherObj().getName(),
                        lesson.getRoom().getName(), lsTs.getLecDays().toString(), lsTs.getStartTimeLec().toString(),
                        lsTs.getEndTimeLec().toString()};
                lstViewRowHelper(row, vals);
            }
            if(lesson.isHasLabAct()){
                //lab print out
                Row row = listSheet.createRow(rowIdx++);
                Object[] vals = new Object[]{lesson.getCourseName(), lesson.getLabActSection(), lesson.getTeacherObj().getName(),
                        lesson.getRoom().getName(), lsTs.getNonLecDays().toString(), lsTs.getStartTimeLabAct().toString(),
                        lsTs.getEndTimeLabAct().toString()};
                lstViewRowHelper(row, vals);
            }
        }

    }

    private void lstViewRowHelper(Row row, Object[] vals) {
        int cellNum = 0;
        for(Object val: vals){
            Cell rowCell = row.createCell(cellNum++);
            if (val instanceof String) {
                rowCell.setCellValue((String) val);
            }
            else if (val instanceof Integer) {
                rowCell.setCellValue((Integer) val);
            }
        }
    }

    private void teacherView(XSSFSheet teacherSheet){
        setupShtHdrs(teacherSheet, TEACHER_COL_MAP.keySet().iterator(), TEACHER_COL_MAP);
    }

    private void roomView(XSSFSheet roomSheet){
        setupShtHdrs(roomSheet, ROOM_COL_MAP.keySet().iterator(), ROOM_COL_MAP);

    }

    private void setupShtHdrs(XSSFSheet sheet, Iterator<String> entities, Map<String, Integer> entityColMap){
        Row entityRow = sheet.createRow(0);
        Row daysRow = sheet.createRow(1);

        //gen time cols
        entityRow.createCell(0).setCellValue("Time");
        CellRangeAddress region = new CellRangeAddress(0, 1, 0, 0);
        sheet.addMergedRegion(region);
        for(LocalTime localTime: TIME_ROW_MAP.keySet()){
            Row row = sheet.createRow(TIME_ROW_MAP.get(localTime));
            Cell cell = row.createCell(0);
            cell.setCellValue(localTime.format(LOCALTIME_FORMATTER));
        }

        //generate the header
        //row 1 names
        //row 2 days
        while(entities.hasNext()) {
            //create entity header
            String entityName = entities.next();
            int colStartIdx = 1 + entityColMap.get(entityName) * COLUMN_SPACING;
            Cell cell = entityRow.createCell(colStartIdx);
            cell.setCellValue(entityName);
            if(cellStyle != null) cell.setCellStyle(cellStyle);
            CellRangeAddress regionEntity = new CellRangeAddress(
                    0,  // first row
                    0,  // last row (same row)
                    colStartIdx,  // first column
                    colStartIdx + COLUMN_SPACING - 1   // last column
            );
            sheet.addMergedRegion(regionEntity);

            //create days sub header for entity
            for(int i = 0; i < 5; i ++){
                Cell dayCell = daysRow.createCell(colStartIdx + i);
                switch (i) {
                    case 0 -> dayCell.setCellValue("M");
                    case 1 -> dayCell.setCellValue("T");
                    case 2 -> dayCell.setCellValue("W");
                    case 3 -> dayCell.setCellValue("R");
                    case 4 -> dayCell.setCellValue("F");
                }
            }
        }


    }


    private void saveSolution(XSSFWorkbook workbook) throws Exception{
        int maxAttempts = 2; // first try + one retry
        //TODO look into: i think this errors if the file doesn't exists already
//        for(int attempt = 1; attempt <= maxAttempts; attempt++) {
//            try{
                Path path = Paths.get(
                        Timetable.class.getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI()
                ).getParent().getParent();
                System.out.println(path);
                String fileLocation = path + "/src/main/java/org/acme/schooltimetabling/generated/" +
                        String.format("%s_solution.xlsx", ScheduleConfig.getCurTerm());

                Path projectRoot = Paths.get(System.getProperty("user.dir"));
                Path outputDir = projectRoot.resolve("generated");
                Files.createDirectories(outputDir);
                Path filePath = outputDir.resolve(
                        ScheduleConfig.getCurTerm() + "_solution.xlsx"
                );
                FileOutputStream outputStream = new FileOutputStream(filePath.toFile());
                workbook.write(outputStream);
                workbook.close();
//            }
//            catch (Exception e){
//                LOGGER.error(String.format("UNABLE TO SAVE SOLUTION. Error: %s", e.getMessage()));
//                LOGGER.info("Error likely due to file being open. Retrying writing to file when user is ready.");
//                System.out.println("Press enter when you are ready to retry saving file:");
//                Scanner scanner = new Scanner(System.in);
//                scanner.nextLine();
//            }
//        }
    }

}
