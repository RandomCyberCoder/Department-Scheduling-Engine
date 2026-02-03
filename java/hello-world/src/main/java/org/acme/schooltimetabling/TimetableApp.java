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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.acme.schooltimetabling.helperClasses.ParseInput;
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
}
