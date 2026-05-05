package org.acme.schooltimetabling;

import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.acme.schooltimetabling.apiCalls.auth.AuthTokens;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.lesson.Lesson;
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
//TODO andrea schuman name udpate in excel file, update in the db as well or at least check over this
    private static final Logger LOGGER = LoggerFactory.getLogger(TimetableApp.class);
    private static final String YAML_FILE_PATH = "constants/config.yaml";
    private static final boolean PRINT_DETAILED_SUMMARY = true;
    

    public static void main(String[] args) throws Exception{
        ArrayList<Room> roomList;
        ArrayList<Lesson> lessonList;
        ArrayList<Timeslot> timeslotList;
        Timetable timetable;
        AuthTokens.getAccessToken();
        ScheduleConfig.loadConfig(YAML_FILE_PATH);
        LOGGER.info(String.format("%s, %s, %s, %s\n", ScheduleConfig.getDepartment(),
                ScheduleConfig.getCurTerm(), ScheduleConfig.getPrevTerm(),
                ScheduleConfig.getSeasonTerm()));
        LOGGER.info("Loading critical constants");
        Constants.load();

        Map<String, Teacher> teacherMap = TeacherGenerator.teacherGenDriver();
        /*generate timeslots*/
        LOGGER.info("Creating timeslot objects");
        timeslotList = TimeslotGenerator.generateTimeslots("constants/possibleTimes.csv");

        /*parse schedules*/
        /*read from the file who will be teaching what for this quarter*/
        List<ScheduleFormat> parsedSchedules = ParseInput.readScheduleClasses(String.format("input/schedule-%s-%s.json"
                , ScheduleConfig.getCurTerm(), ScheduleConfig.getDepartment().toLowerCase()));

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
        SolutionManager<Timetable, HardMediumSoftScore> solutionManager = SolutionManager.create(solverFactory);
        ScoreAnalysis<HardMediumSoftScore> scoreAnalysis = solutionManager.analyze(solution);

        //short summary of violated constraints in the solution
        LOGGER.info(scoreAnalysis.summarize());

        /*tally up how many 30minute lecture blocks are in/out of primetime and print out. Mostly to verify
        * the aggressive primetime scheduling hard constraint*/
        int blcksOutPT = 0;
        int blcksInPT = 0;
        for(Lesson lesson : solution.getLessons()){
            blcksInPT += lesson.maskInPT().cardinality();
            blcksOutPT += lesson.maskOutPT().cardinality();
        }
        LOGGER.info(String.format("Number of 30-minute blocks in prime time: %d;  Number of 30-minute blocks " +
                "out of prime time: %d", blcksInPT, blcksOutPT));
        //notify user that primetime hard constraint failed. It shouldn't fail based on my test but just in case.
        if(blcksInPT > blcksOutPT){
            LOGGER.error("FAILED TO MEET SCHEDULE 50%+ OF LECTURE TIME OUTSIDE OF PRIMETIME. SWITCH TO " +
                    "NON-AGGRESSIVE SOLVER IF POSSIBLE. WHILE CONSTRAINT IS INVESTIGATED");
        }


        //print a detailed summary for every constraint a list of all the instances of it being violated
        if(PRINT_DETAILED_SUMMARY){
            scoreAnalysis.constraintMap().forEach((constraintRef, constraintAnalysis) -> {
                //skip printing detailed summary for anything that isn't given a hard penalty
                if(constraintAnalysis.score().hardScore() == 0) return;
                LOGGER.info("Constraint: " + constraintRef.constraintId());
                LOGGER.info(" Score: " + constraintAnalysis.score());
                for (MatchAnalysis<HardMediumSoftScore> match : constraintAnalysis.matches()) {
                    LOGGER.info("  Match score: " + match.score());
                    LOGGER.info("  Justification: " + match.justification());
                }
            });
        }

        ResultSaver resultSaver = new ResultSaver(solution);
        resultSaver.saveSolution();
        resultSaver.teacherTimesToJson();

        return;
    }
}
