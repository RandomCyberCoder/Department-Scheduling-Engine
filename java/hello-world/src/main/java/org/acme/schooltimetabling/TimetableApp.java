package org.acme.schooltimetabling;

import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import com.google.common.collect.BiMap;
import org.acme.schooltimetabling.constants.Constants;
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

    private static final boolean PRINT_DETAILED_SUMMARY = false;

    public static void main(String[] args) throws Exception{
        ArrayList<Room> roomList;
        ArrayList<Lesson> lessonList;
        ArrayList<Timeslot> timeslotList;
        Timetable timetable;

        LOGGER.info(String.format("%s, %s, %s, %s\n", ParseInput.scheduleConfig.department,
                ParseInput.scheduleConfig.curTerm, ParseInput.scheduleConfig.prevTerm,
                ParseInput.scheduleConfig.seasonTerm));

        /*New headers for the survey*/
        ArrayList<String> newSurveyHeaders = new ArrayList<>(
                Arrays.asList("id", "start", "complete", "email", "name", "use_old",
                        "7 AM","8 AM","9 AM","10 AM","11 AM","12 PM","1 PM","2 PM",
                        "3 PM","4 PM","5 PM","6 PM","7 PM","8 PM","9 PM","7 AM2",
                        "8 AM2","9 AM2","10 AM2","11 AM2","12 PM2","1 PM2","2 PM2",
                        "3 PM2","4 PM2","5 PM2","6 PM2","7 PM2","8 PM2","9 PM2",
                        "mwf_1", "tr_1", "mwf_2", "mwf_tr",
                        "tr_2", "mwf_3","mwf_2_tr_1", "mwf_1_tr_2",
                        "tr_3", "mwrf", "mtwr", "mw", "tr",
                        "back_to_back", "gap", "constraint", "require",
                        "pref", "comment", "stars")
        );


        /*TODO make it so teachers with no survey get assigned a generic timeslot
        *  ....maybe add a list of the generics to constants???*/
        /*read the cur & prev quarter survey and then create Teacher objects*/
        String curQuarterSurveyPath = String.format("input/%s-survey.csv", ParseInput.scheduleConfig.curTerm);
        String prevQuarterSurveyPath = String.format("input/%s-survey.csv", ParseInput.scheduleConfig.prevTerm);
        LOGGER.info("Reading the current quarter teacher survey");
        ArrayList<HashMap<String, String>> curQuarterSurveys = ParseInput.readCSV(curQuarterSurveyPath, newSurveyHeaders);
        LOGGER.info("Reading the previous quarter teacher survey");
        /*read the prev quarter survey*/
        ArrayList<HashMap<String, String>> prevQuarterSurveys  = ParseInput.readCSV(prevQuarterSurveyPath,newSurveyHeaders);
        LOGGER.info("Creating teacher objects");
        /*teacher name -> teacher object*/
        HashMap<String, Teacher>teacherHashMap = TeacherGenerator.generateTeachers(curQuarterSurveys, prevQuarterSurveys);

        /*generate timeslots*/
        LOGGER.info("Creating timeslot objects");
        timeslotList = TimeslotGenerator.generateTimeslots("constants/possibleTimes.csv");

        /*parse schedules*/
        /*read from the file who will be teaching what for this quarter*/
        List<ScheduleFormat> parsedSchedules = ParseInput.readScheduleClasses(String.format("input/schedule-%s-%s.json"
                , ParseInput.scheduleConfig.curTerm, ParseInput.scheduleConfig.department));

        LOGGER.info("Creating lesson objects");
        /*Creating Lessons*/
        /*TODO: migrate this next line into the Constants class and this can change the lesson class creation for
        *  courseID member*/
        HashMap<String, String> courseConfigs = ParseInput.readCourseConfigs("constants/configurations.tsv");
        BiMap<String, Integer> courseIdMapping = Generator.genCourseToIdMapping(courseConfigs.keySet().iterator());
        lessonList = LessonGenerator.generateLessons(courseConfigs, courseIdMapping, parsedSchedules, teacherHashMap);

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
        System.out.println(scoreAnalysis.summarize());

        //print a detailed summary for every constraint a list of all the instances of it being violated
        if(PRINT_DETAILED_SUMMARY){
            scoreAnalysis.constraintMap().forEach((constraintRef, constraintAnalysis) -> {
                System.out.println("Constraint: " + constraintRef.constraintId());
                System.out.println(" Score: " + constraintAnalysis.score());
                for (MatchAnalysis<HardSoftLongScore> match : constraintAnalysis.matches()) {
                    System.out.println("  Match score: " + match.score());
                    System.out.println("  Justification: " + match.justification());
                }
            });
        }

        storeResults(solution);
    }

    public static void storeResults(Timetable solution) throws Exception{
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
            sheet.setColumnWidth(1,6000);

            cell = row.createCell(2);
            cell.setCellValue("Has a lab/act");
            sheet.setColumnWidth(3,6000);

            cell = row.createCell(3);
            cell.setCellValue("Room");
            sheet.setColumnWidth(2,6000);

            cell = row.createCell(4);
            cell.setCellValue("Lecture Time");
            sheet.setColumnWidth(4,10000);

            cell = row.createCell(5);
            cell.setCellValue("Lab Time");
            sheet.setColumnWidth(5,10000);
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
            cell.setCellValue(lesson.hasLabAct);

            if(lesson.getTimeslot() != null && lesson.getRoom() != null){

                cell = row.createCell(3);
                cell.setCellValue(lesson.getRoom().getName());

                cell = row.createCell(4);
                cell.setCellValue(lesson.getTimeslot().toStringLec());

                cell = row.createCell(5);
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
