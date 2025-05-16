package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import org.acme.schooltimetabling.helperClasses.Teacher;

@PlanningEntity
public class Lesson {
    @PlanningId
    private String id;

    private String subject;
    private String teacher;
    private String studentGroup;
    public String courseName, teacherName, modifiers;
    public int courseID, lecSection, labActSection;
    public boolean hasLabAct;
    public int lec_hours, lab_activity_hours;
    public Teacher teacherObj;


    @PlanningVariable
    private Timeslot timeslot;

    @PlanningVariable
    private Room room;

    // No-arg constructor required for Timefold
    public Lesson() {
    }

    /*stuff from starter code*/
    public Lesson(String id, String subject, String teacher, String studentGroup) {
        this.id = id;
        this.subject = subject;
        this.teacher = teacher;
        this.studentGroup = studentGroup;
    }
    /*stuff from starter code*/
    public Lesson(String id, String subject, String teacher, String studentGroup, Timeslot timeslot, Room room) {
        this(id, subject, teacher, studentGroup);
        this.timeslot = timeslot;
        this.room = room;
    }

    /*TODO change all planning variable IDs to a int/Integer as mentioned in the documentation
    *  https://docs.timefold.ai/timefold-solver/latest/using-timefold-solver/modeling-planning-problems#planningId*/
    public Lesson(String Id, int lecSection, String courseName, String teacherName, String modifiers,
                  String courseConfig, int courseID, Teacher teacherObj){
        /*One unit of lecture is equal to one hour in class*/
        final int LEC_UNITS_TO_HOURS = 1;
        /*One unit of Activity is equal to two hours in the activity*/
        int ACTIVITY_UNITS_TO_HOURS = 2;
        /*One unit of Lab is equal to three hours in lab*/
        final int LAB_UNITS_TO_HOURS = 3;
        final int NO_HOURS = 0;
        final int NO_SECTION = -1;
        /*the courseConfig stream is assumed to come in the format
        * E-L-A where E is the number of lecture units, L is the number of
        * lab units, and A is the number of activity units */
        String[] units = courseConfig.split("-");
        int lecUnits = Integer.parseInt(units[0]);
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        lec_hours = lecUnits * LEC_UNITS_TO_HOURS;
        /*Note that I don't actually consider a scenario where a course has both a
        * lab and an activity, but I do this, so I don't have to check for a lab/activity specifically*/
        lab_activity_hours = labUnits * LAB_UNITS_TO_HOURS + actUnits * ACTIVITY_UNITS_TO_HOURS;
        if(lab_activity_hours == NO_HOURS){
            hasLabAct = false;
        }
        /*mark true if there is a lab/activity; false otherwise*/
        this.hasLabAct = !(lab_activity_hours == NO_HOURS);
        this.id = Id;
        this.lecSection = lecSection;
        this.labActSection = this.hasLabAct ? lecSection + 1 : NO_SECTION;
        this.courseID = courseID;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.teacherObj = teacherObj;
        this.modifiers = modifiers;



    }

    @Override
    public String toString() {
        return subject + "(" + id + ")";
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getStudentGroup() {
        return studentGroup;
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

}
