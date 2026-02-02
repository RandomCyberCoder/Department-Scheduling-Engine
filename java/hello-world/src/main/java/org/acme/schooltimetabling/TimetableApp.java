package org.acme.schooltimetabling;

import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Timetable;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.*;
import org.acme.schooltimetabling.helperClasses.Generators.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acme.schooltimetabling.helperClasses.ParseInput;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TimetableApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimetableApp.class);
    private static final String YAML_FILE_PATH = "constants/config.yaml";
    private static final boolean PRINT_DETAILED_SUMMARY = false;
    

    public static void main(String[] args) throws Exception{
        ArrayList<Room> roomList;
        ArrayList<Lesson> lessonList;
        ArrayList<Timeslot> timeslotList;
        Timetable timetable;

        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        LOGGER.info(String.format("%s, %s, %s, %s\n", ScheduleConfig.getDepartment(),
                ScheduleConfig.getCurTerm(), ScheduleConfig.getPrevTerm(),
                ScheduleConfig.getSeasonTerm()));

        Map<String, Teacher> teacherMap = TeacherGenerator.teacherGenDriver();
        /*generate timeslots*/
        LOGGER.info("Creating timeslot objects");
        timeslotList = TimeslotGenerator.generateTimeslots(
                String.format("constants/%s_possibleTimes.csv", ScheduleConfig.getDepartment()));

        /*parse schedules*/
        /*read from the file who will be teaching what for this quarter*/
        List<ScheduleFormat> parsedSchedules = ParseInput.readScheduleClasses(String.format("input/schedule-%s-%s.json"
                , ScheduleConfig.getCurTerm(), ScheduleConfig.getDepartment()));

        LOGGER.info("Creating lesson objects");
        /*Creating Lessons*/
        lessonList = LessonGenerator.generateLessons(parsedSchedules, teacherMap);

        roomList = RoomGenerator.generateRooms();
        timetable = new Timetable("setup", timeslotList, roomList, lessonList);

        SolverConfig solverConfig = SolverConfig.createFromXmlResource("solverConfig.xml");

        SolverFactory<Timetable> solverFactory = SolverFactory.create(solverConfig);
        Solver<Timetable> solver = solverFactory.buildSolver();
        Timetable solution = solver.solve(timetable);

        //analyzing the solution
        SolutionManager<Timetable, HardSoftLongScore> solutionManager = SolutionManager.create(solverFactory);
        ScoreAnalysis<HardSoftLongScore> scoreAnalysis = solutionManager.analyze(solution);

        //short summary of violated constraints in the solution
        LOGGER.info(scoreAnalysis.summarize());

        //print a detailed summary for every constraint a list of all the instances of it being violated
        if(PRINT_DETAILED_SUMMARY){
            scoreAnalysis.constraintMap().forEach((constraintRef, constraintAnalysis) -> {
                LOGGER.info("Constraint: " + constraintRef.constraintId());
                LOGGER.info(" Score: " + constraintAnalysis.score());
                for (MatchAnalysis<HardSoftLongScore> match : constraintAnalysis.matches()) {
                    LOGGER.info("  Match score: " + match.score());
                    LOGGER.info("  Justification: " + match.justification());
                }
            });
        }

//        storeResults(solution);
        ResultSaver resultSaver = new ResultSaver(solution);
        resultSaver.saveSolution();

        return;
    }

    private static void storeResults(Timetable solution) throws Exception{
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet solutionSpreadsheet = workbook.createSheet("Solution");
        XSSFSheet leftOutSpreadsheet = workbook.createSheet("LeftOut");
        List<Lesson> solutionLessons = solution.getLessons();
        int solutionRow = 1;
        int leftOutRow = 1;
        for(XSSFSheet sheet: List.of(solutionSpreadsheet, leftOutSpreadsheet)){
            Row row = sheet.createRow(0);
            Cell cell;

            cell = row.createCell(0);
            cell.setCellValue("Instructor Name");
            sheet.setColumnWidth(0,9000);

            cell = row.createCell(1);
            cell.setCellValue("Course Name");
            sheet.setColumnWidth(1,4000);

            cell = row.createCell(2);
            cell.setCellValue("Lesson planning ID");
            sheet.setColumnWidth(2,6000);


            cell = row.createCell(3);
            cell.setCellValue("Linker");
            sheet.setColumnWidth(3, 2000);

            cell = row.createCell(4);
            cell.setCellValue("Has a lab/act");
            sheet.setColumnWidth(4,6000);

            cell = row.createCell(5);
            cell.setCellValue("Room");
            sheet.setColumnWidth(5,6000);

            cell = row.createCell(6);
            cell.setCellValue("Lecture Time");
            sheet.setColumnWidth(6,10000);

            cell = row.createCell(7);
            cell.setCellValue("Lab Time");
            sheet.setColumnWidth(7,10000);
        }

        for(Lesson lesson: solutionLessons){
            //teacher, course name, lab, timeslot lec range, timeslot lab/act range
            Row row = solutionSpreadsheet.createRow(solutionRow++);
            Cell cell;
            cell = row.createCell(0);
            cell.setCellValue(lesson.getTeacherName());

            cell = row.createCell(1);
            cell.setCellValue(lesson.getCourseName());

            cell = row.createCell(2);
            cell.setCellValue(lesson.getId());

            if(lesson.getLinker() != null){
                cell = row.createCell(3);
                cell.setCellValue(lesson.getLinker());
            }

            cell = row.createCell(4);
            cell.setCellValue(lesson.hasLabAct);

            if(lesson.getTimeslot() != null && lesson.getRoom() != null){

                cell = row.createCell(5);
                cell.setCellValue(lesson.getRoom().getName());

                cell = row.createCell(6);
                cell.setCellValue(lesson.getTimeslot().toStringLec());

                cell = row.createCell(7);
                cell.setCellValue(lesson.getTimeslot().toStringLabAct());
            }
        }

        Path path = Paths.get(
                Timetable.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParent().getParent();
        System.out.println(path);
        String fileLocation = path + "/src/main/java/org/acme/schooltimetabling/generated/temp.xlsx";

        FileOutputStream outputStream = new FileOutputStream(fileLocation);
        workbook.write(outputStream);
        workbook.close();
    }

}
