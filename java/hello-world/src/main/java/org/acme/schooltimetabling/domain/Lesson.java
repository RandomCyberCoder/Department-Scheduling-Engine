package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.teacher.Teacher;

@PlanningEntity
public class Lesson {
    /**One unit of lecture is equal to one hour in class*/
    private final int LEC_UNITS_TO_HOURS = 1;
    /**One unit of Activity is equal to two hours in the activity*/
    private final int ACTIVITY_UNITS_TO_HOURS = 2;
    /**One unit of Lab is equal to three hours in lab*/
    private final int LAB_UNITS_TO_HOURS = 3;
    private final int NO_HOURS = 0;
    private final int NO_SECTION = -1;

    @PlanningId
    private String id;
    public String courseName, teacherName, modifiers;
    public int courseID, lecSection, labActSection;
    public boolean hasLecture, hasLabAct;
    public int lec_hours, lab_activity_hours;
    public Teacher teacherObj;
    private Integer linker;


    @PlanningVariable
    private Timeslot timeslot;

    @PlanningVariable
    private Room room;

    /**Don't for default constructor use*/
    private Lesson() {
    }


    /*TODO change all planning variable IDs to a int/Integer as mentioned in the documentation
    *  https://docs.timefold.ai/timefold-solver/latest/using-timefold-solver/modeling-planning-problems#planningId*/

    /* Test factory methods */
    public static Lesson test_buildLesson(String Id, int lecSection, String courseName, String teacherName, String modifiers,
                            String courseConfig, int courseID, Teacher teacherObj, Timeslot timeslot, Room room){
        return new Lesson(Id, lecSection, courseName, teacherName, modifiers, courseConfig, courseID, teacherObj
                , timeslot, room);
    }

    /* Test constructor(s)*/
    public Lesson(String Id, int lecSection, String courseName, String teacherName, String modifiers,
                  String courseConfig, int courseID, Teacher teacherObj, Timeslot timeslot, Room room){
//        /*calling normal constructor used during setup*/
//        this(Id, lecSection, courseName, teacherName, modifiers, courseConfig, courseID, teacherObj, null);
        /*the courseConfig stream is assumed to come in the format
         * E-L-A where E is the number of lecture units, L is the number of
         * lab units, and A is the number of activity units */
        String[] units = courseConfig.split("-");
        int lecUnits = Integer.parseInt(units[0]);
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        lec_hours = lecUnits * LEC_UNITS_TO_HOURS;
        /*Note that I don't actually consider a scenario where a course has both a
         * lab and an activity, not sure if that's possible. I do this, so I don't have to check for a
         * lab/activity specifically*/
        lab_activity_hours = labUnits * LAB_UNITS_TO_HOURS + actUnits * ACTIVITY_UNITS_TO_HOURS;
        /*mark true if lecture or lab/act exists; false otherwise*/
        this.hasLecture = lecUnits != NO_HOURS;
        this.hasLabAct = lab_activity_hours != NO_HOURS;
        this.id = Id;
        this.lecSection = lecSection;
        this.labActSection = this.hasLabAct ? lecSection + 1 : NO_SECTION;
        this.courseID = courseID;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.teacherObj = teacherObj;
        this.modifiers = modifiers;
        this.linker = null;
        /*Populate planning variables*/
        this.timeslot = timeslot;
        this.room = room;
    }

    /**
     * Class constructor
     * <p>
     * the courseConfig stream is assumed to come in the format
     * E-L-A where E is the number of lecture units, L is the number of
     * lab units, and A is the number of activity units
     * </p>
     *
     * @param Id unique ID for the lesson
     * @param lecSection unique section number for the lecture
     * @param courseName name of the course that will be taught for this lesson
     * @param modifiers any course modifiers. If no modifiers pass a "" string
     * @param courseConfig configuration of the <i>courseName</i> for this lesson
     * @param teacherObj teacher object associated with the <i>teacherName</i>
     */
    public Lesson(String Id, int lecSection, String courseName, String modifiers,
                  String courseConfig, Teacher teacherObj, Integer linker){
        /*the courseConfig stream is assumed to come in the format
        * E-L-A where E is the number of lecture units, L is the number of
        * lab units, and A is the number of activity units */
        String[] units = courseConfig.split("-");
        int lecUnits = Integer.parseInt(units[0]);
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        lec_hours = lecUnits * LEC_UNITS_TO_HOURS;
        /*Note that I don't actually consider a scenario where a course has both a
        * lab and an activity, not sure if that's possible. I do this, so I don't have to check for a
        * lab/activity specifically*/
        lab_activity_hours = labUnits * LAB_UNITS_TO_HOURS + actUnits * ACTIVITY_UNITS_TO_HOURS;
        /*mark true if lecture or lab/act exists; false otherwise*/
        this.hasLecture = lecUnits != NO_HOURS;
        this.hasLabAct = lab_activity_hours != NO_HOURS;
        this.id = Id;
        this.lecSection = lecSection;
        this.labActSection = this.hasLabAct ? lecSection + 1 : NO_SECTION;
        this.courseID = Constants.COURSE_ID_BIMAP.get(courseName);
        this.courseName = courseName;
        this.teacherObj = teacherObj;
        /*TODO check if we can delete this field*/
        this.teacherName = teacherObj.getName();
        this.modifiers = modifiers;
        this.linker = linker;
    }

    @Override
    public String toString() {
        return courseName + "(" + id + ")" + " instructor: " + teacherObj.getName();
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getId() {
        return id;
    }
    public Timeslot getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(Timeslot timeslot) {
        this.timeslot = timeslot;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getModifiers() {
        return modifiers;
    }

    public int getCourseID() {
        return courseID;
    }

    public int getLecSection() {
        return lecSection;
    }

    public int getLabActSection() {
        return labActSection;
    }

    public boolean isHasLabAct() {
        return hasLabAct;
    }

    public int getLec_hours() {
        return lec_hours;
    }

    public int getLab_activity_hours() {
        return lab_activity_hours;
    }

    public Teacher getTeacherObj() {
        return teacherObj;
    }

    public Integer getLinker(){
        return linker;
    }
}
