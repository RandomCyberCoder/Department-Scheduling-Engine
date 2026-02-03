package org.acme.schooltimetabling.helperClasses;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
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
    private CellStyle headerCellStyle = null;
    private CellStyle lessonCellStyle = null;

    static {
        ROOM_COL_MAP = new HashMap<>();
        int colIdx = 0;
        for(String roomName: Constants.POSSIBLE_ROOMS){
            if(Objects.equals(roomName, Constants.LEC_ONLY)) continue;
            ROOM_COL_MAP.put(roomName, START_COLUMN + colIdx++ * COLUMN_SPACING);
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
                TEACHER_COL_MAP.put(lesson.getTeacherObj().getName(),
                        START_COLUMN + rowNum++ * COLUMN_SPACING);
            }
        }
    }

    public void saveSolution() throws Exception{
        final XSSFWorkbook workbook = new XSSFWorkbook();
        headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
        lessonCellStyle = workbook.createCellStyle();
        lessonCellStyle.setWrapText(true);
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
        Map<Integer, Row> rowsBuilt = new HashMap<>();
        setupShtHdrs(teacherSheet, rowsBuilt, TEACHER_COL_MAP.keySet().iterator(), TEACHER_COL_MAP);
        List<Lesson> lessonList = solToPrint.getLessons();
        for(Lesson lesson: lessonList){
            Timeslot ts = lesson.getTimeslot();
            Teacher teacher = lesson.getTeacherObj();
            //lecture
            if(lesson.isHasLecture()){
                String lecStr = lecToStr(lesson);
                fillTimeCell(teacherSheet, rowsBuilt, lecStr, TEACHER_COL_MAP.get(teacher.getName()), ts.getLecDays(),
                        ts.getStartTimeLec(), ts.getEndTimeLec());
            }
            //lab
            if(lesson.isHasLabAct()){
                String labStr = labToStr(lesson);
                fillTimeCell(teacherSheet, rowsBuilt, labStr, TEACHER_COL_MAP.get(teacher.getName()), ts.getNonLecDays(),
                        ts.getStartTimeLabAct(), ts.getEndTimeLabAct());
            }

        }
    }

    private void roomView(XSSFSheet roomSheet){
        Map<Integer, Row> rowsBuilt = new HashMap<>();
        setupShtHdrs(roomSheet, rowsBuilt, ROOM_COL_MAP.keySet().iterator(), ROOM_COL_MAP);
        List<Lesson> lessonList = solToPrint.getLessons();
        String labStr = "";
        for(Lesson lesson: lessonList){
            Timeslot ts = lesson.getTimeslot();
            labStr =  labToStr(lesson);
            fillTimeCell(roomSheet, rowsBuilt, labStr, ROOM_COL_MAP.get(lesson.getRoom().getName()), ts.getNonLecDays(),
                            ts.getStartTimeLabAct(), ts.getEndTimeLabAct());

        }
    }

    /**
     *
     * @param sheet
     * @param rowMap map of row num to Row if created. This is important because if you remake the {@link Row} for row number
     *               that has had one built for already it will delete the old contents
     * @param cellVal
     * @param entityStrtCol column idx from map
     * @param start start time (inclusive)
     * @param end end time (exclusive)
     */
    private void fillTimeCell(XSSFSheet sheet, Map<Integer, Row> rowMap, String cellVal, int entityStrtCol,
                              EnumSet<Days> days, LocalTime start, LocalTime end){
        final int T_OFFSET = 1;
        final int W_OFFSET = 2;
        final int R_OFFSET = 3;
        final int F_OFFSET = 4;
        final int frstRw = TIME_ROW_MAP.get(start);
        //minus 30 minutes because end time is exclusive
        final int lstRw = TIME_ROW_MAP.get(end.minusMinutes(30));
        final int strtCol = entityStrtCol;
        for(Days day: days){
            int useCol;

            if(day == Days.MONDAY) useCol = strtCol;
            else if(day == Days.TUESDAY) useCol = strtCol + T_OFFSET;
            else if(day == Days.WEDNESDAY) useCol = strtCol + W_OFFSET;
            else if(day == Days.THURSDAY) useCol = strtCol + R_OFFSET;
            else useCol = strtCol + F_OFFSET;

            Cell cell = rowMap.get(frstRw).createCell(useCol);
            cell.setCellValue(cellVal);
            cell.setCellStyle(lessonCellStyle);
            CellRangeAddress region = new CellRangeAddress(frstRw, lstRw, useCol, useCol);
            sheet.addMergedRegion(region);
        }
    }

    private void setupShtHdrs(XSSFSheet sheet, Map<Integer, Row> rowMap, Iterator<String> entities, Map<String, Integer> entityColMap){
        //I don't add these to the
        Row entityRow = sheet.createRow(0);
        Row daysRow = sheet.createRow(1);

        //gen time cols
        entityRow.createCell(0).setCellValue("Time");
        CellRangeAddress region = new CellRangeAddress(0, 1, 0, 0);
        sheet.addMergedRegion(region);
        for(LocalTime localTime: TIME_ROW_MAP.keySet()){
            final int TIME_COL = TIME_ROW_MAP.get(localTime);
            Row row = sheet.createRow(TIME_COL);
            row.setHeight((short) 2000);
            rowMap.put(TIME_COL, row);
            Cell cell = row.createCell(0);
            cell.setCellValue(localTime.format(LOCALTIME_FORMATTER));
        }

        //generate the header
        //row 1 names
        //row 2 days
        while(entities.hasNext()) {
            //create entity header
            String entityName = entities.next();
            int colStartIdx = entityColMap.get(entityName);
            Cell cell = entityRow.createCell(colStartIdx);
            cell.setCellValue(entityName);
            if(headerCellStyle != null) cell.setCellStyle(headerCellStyle);
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
                sheet.setColumnWidth(colStartIdx + i, 4000);
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
        for(int attempt = 1; attempt <= maxAttempts; attempt++) {
            try{
                Path projectRoot = Paths.get(System.getProperty("user.dir"));
                Path outputDir = projectRoot.resolve("generated");
                Files.createDirectories(outputDir);
                Path filePath = outputDir.resolve(
                        String.format("%s_%s_solution.xlsx", ScheduleConfig.getCurTerm(),
                                ScheduleConfig.getDepartment())
                );
                FileOutputStream outputStream = new FileOutputStream(filePath.toFile());
                workbook.write(outputStream);
                workbook.close();
                break;
            }
            catch (Exception e){
                LOGGER.error(String.format("UNABLE TO SAVE SOLUTION. Error: %s", e.getMessage()));
                LOGGER.info("Error likely due to file being open. Retrying writing to file when user is ready.");
                System.out.println("Press enter when you are ready to retry saving file:");
                Scanner scanner = new Scanner(System.in);
                e.printStackTrace();
                scanner.nextLine();
            }
        }
    }

    private String labToStr(Lesson lesson){
        Timeslot ts = lesson.getTimeslot();
        return String.format("%s\n", lesson.getTeacherObj().getName()) +
                String.format("%s\n", lesson.getCourseName()) +
                String.format("%s\n", lesson.getRoom().getName()) +
                String.format("Lab Sec Num: %s\n", lesson.getLabActSection()) +
                String.format("%s  %s-%s", ts.getLecDays().toString(), ts.getStartTimeLabAct().format(LOCALTIME_FORMATTER),
                        ts.getEndTimeLabAct().format(LOCALTIME_FORMATTER));
    }

    private String lecToStr(Lesson lesson){
        Timeslot ts = lesson.getTimeslot();
        return String.format("%s\n", lesson.getTeacherObj().getName()) +
                String.format("%s\n", lesson.getCourseName()) +
                String.format("%s\n", "University Room") +
                String.format("Lab Sec Num: %s\n", lesson.getLecSection()) +
                String.format("%s  %s-%s", ts.getLecDays().toString(), ts.getStartTimeLec().format(LOCALTIME_FORMATTER),
                        ts.getEndTimeLec().format(LOCALTIME_FORMATTER));


    }

}
