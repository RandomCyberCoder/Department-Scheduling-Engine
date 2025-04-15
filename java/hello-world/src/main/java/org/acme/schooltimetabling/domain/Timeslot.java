package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;

public class Timeslot {

    @PlanningId
    private String id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int ID;

    private LocalTime startTimeLec;
    private LocalTime endTimeLec;
    private BitSet lectureBitSet;
    private boolean onlyLec;
    private LocalTime startTimeLabAct;
    private LocalTime endTimeLabAct;
    private BitSet labActBitSet;
    private BitSet allTimesBitSet;
    private boolean monday, tuesday, wednesday, thursday, friday;
    float lecHours;
    float totalHours;

    private static final float FLOAT_TIME_DELTA = 0.01f;
    private static final int MINUTES_PER_HOUR = 60;

    public Timeslot() {
    }

    public Timeslot(String id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Timeslot(int ID, String days, String startTime, LocalTime endTime, float lecHours, float totalHours)
            throws Exception{

        this.ID = ID;
        /*Mark what days the timeslot occupies*/
        monday = tuesday = wednesday = thursday = friday = false;
        if(days.contains("M")){
            monday = true;
        }
        if(days.contains("T")){
            tuesday = true;
        }
        if(days.contains("W")){
            wednesday = true;
        }
        if(days.contains("R")){
            thursday = true;
        }
        if(days.contains("F")){
            friday = true;
        }
        /*determine if the timeslot will accommodate only lectures*/
        this.lecHours = lecHours;
        this.totalHours = totalHours;
        /* We do the following comparison instead of lec_hours == total_hours because of floating point errors */
        this.onlyLec = Math.abs(lecHours - totalHours) < FLOAT_TIME_DELTA;

        /*check start and end time for the lab and possibly for the lab/activity */
        this.startTimeLec = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("h:mma"));
        /*LocalTime is immutable so doing this won't modify startTimeLec*/
        this.endTimeLec = startTimeLec.plusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
        /* initialize the lecture BitSet, multiply lecHours by 2 because we need then number of 30 minute blocks */
        this.lectureBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(lecHours * 2),
                this.monday, this.tuesday, this.wednesday, this.thursday, this.friday);

        /* initialize the lab/activity members based off if the timeslot is for
         * lectures only */
        if(this.onlyLec){
            this.startTimeLabAct = this.endTimeLabAct = this.endTimeLec;
            /*if the first time slot was only a lecture time slot we now want to check if the adjacent time slot has
            * a lab time.*/
            this.labActBitSet = new BitSet();
        }
        else{
            //ED this should really be endTime.minusMinutes(Math.round(lecHours * 60)))
            this.startTimeLabAct = this.endTimeLec;
            /* compute how much time is left after the lecture for the timeslot*/
            //ED shouldn't this just be the endTime passed?
            this.endTimeLabAct = startTimeLabAct.plusMinutes(Math.round(MINUTES_PER_HOUR * (this.totalHours - this.lecHours)));
            /* create BitSet for the lab/lec */
            this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(lecHours * 2),
                    this.monday, this.tuesday, this.wednesday, this.thursday, this.friday);
        }

    }

    public Timeslot(String id, DayOfWeek dayOfWeek, LocalTime startTime) {
        this(id, dayOfWeek, startTime, startTime.plusMinutes(50));
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + startTime;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getId() {
        return id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

}
