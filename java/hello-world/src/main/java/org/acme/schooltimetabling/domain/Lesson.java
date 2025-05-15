package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import org.acme.schooltimetabling.helperClasses.Teacher;

@PlanningEntity
public class Lesson {
    /*One unit of lecture is equal to one hour in class*/
    final private int LEC_UNITS_TO_HOURS = 1;
    /*One unit of Lab is equal to three hours in lab*/
    final private int LAB_UNITS_TO_HOURS = 3;
    final private int ACTIVITY_UNITS_TO_HOURS = 2;
    @PlanningId
    private String id;

    private String subject;
    private String teacher;
    private String studentGroup;
    public String courseName, teacherName, modifiers;
    public int courseID, teacherID, lecSection;
    public boolean lab_activity;
    public float lec_hours, lab_activity_hours;
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
    public Lesson(String Id, String courseName, String teacherName, String modifiers,
                  String courseConfig, int courseID, Teacher teacherObj){
        /*here we will have to calculate how many lec_hours we need*/
        /*TODO pick up here again should be easy to wrap up.. I think*/

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
