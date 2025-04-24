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

    /*new*/
    private int ID;
    private LocalTime startTimeLec;
    private LocalTime endTimeLec;
    private BitSet lectureBitSet;
    private boolean onlyLec;
    private LocalTime startTimeLabAct;
    private LocalTime endTimeLabAct;
    private BitSet labActBitSet;
    private BitSet allTimesBitSet;
    /*I should make these days into a class or something*/
    private boolean lecMonday, lecTuesday, lecWednesday, lecThursday, lecFriday;
    private boolean nonLecMonday, nonLecTuesday, nonLecWednesday, nonLecThursday, nonLecFriday;
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

    public Timeslot(int ID, String days, String startTime, String endTime, float lecHours, float totalHours,
                    String days2, String startTime2, String endTime2, float lecture_hours2, float total_hours2)
            throws Exception{

        /*this variable will be used to see if the entry has two timeslots linked,
        * used later for calculating the complete bitset representation of the timeslot*/
        boolean secondSlot = false;

        this.ID = ID;
        this.id = String.valueOf(ID);
        /*Mark what days the timeslot occupies*/
        lecMonday = lecTuesday = lecWednesday = lecThursday = lecFriday = false;
        if(days.contains("M")){
            lecMonday = true;
        }
        if(days.contains("T")){
            lecTuesday = true;
        }
        if(days.contains("W")){
            lecWednesday = true;
        }
        if(days.contains("R")){
            lecThursday = true;
        }
        if(days.contains("F")){
            lecFriday = true;
        }
        /*determine if the timeslot will accommodate only lectures*/
        this.lecHours = lecHours;
        this.totalHours = totalHours;
        /* We do the following comparison instead of lec_hours == total_hours because of floating point errors */
        this.onlyLec = Math.abs(lecHours - totalHours) < FLOAT_TIME_DELTA;

        /*check start and end time for the lab and possibly for the lab/activity */
        this.startTimeLec = LocalTime.parse(startTime.trim(), DateTimeFormatter.ofPattern("h:mma"));
        /*LocalTime is immutable so doing this won't modify startTimeLec*/
        this.endTimeLec = startTimeLec.plusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
        /* initialize the lecture BitSet, multiply lecHours by 2 because we need then number of 30 minute blocks */
        this.lectureBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(lecHours * 2),
                this.lecMonday, this.lecTuesday, this.lecWednesday, this.lecThursday, this.lecFriday);

        /* initialize the lab/activity members based off if the timeslot is for
         * lectures only */
        if(this.onlyLec){
            /*if the first time slot was only a lecture time slot we now want to check if the adjacent time slot has
            * a lab time.*/
            if(days2.isBlank()){
                this.labActBitSet = new BitSet();
                this.startTimeLabAct = this.endTimeLabAct = this.endTimeLec;
            }
            else{
                secondSlot = true;
                this.onlyLec = false;
                this.nonLecMonday = this.nonLecTuesday = this.nonLecWednesday = this.nonLecThursday = this.nonLecFriday = false;
                if(days.contains("M")){
                    this.nonLecMonday = true;
                }
                if(days.contains("T")){
                    this.nonLecTuesday = true;
                }
                if(days.contains("W")){
                    this.nonLecWednesday = true;
                }
                if(days.contains("R")){
                    this.nonLecThursday = true;
                }
                if(days.contains("F")){
                    this.nonLecFriday = true;
                }
                this.endTimeLabAct = LocalTime.parse(endTime2.trim(), DateTimeFormatter.ofPattern("h:mma"));
                this.startTimeLabAct = LocalTime.parse(startTime2.trim(), DateTimeFormatter.ofPattern("h:mma"));
                this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(total_hours2 * 2),
                        this.nonLecMonday, this.nonLecTuesday, this.nonLecWednesday, this.nonLecThursday, this.nonLecFriday);
            }
        }
        else{
            /* end time of the timeslot is when the lab will end */
            this.endTimeLabAct = LocalTime.parse(endTime.trim(), DateTimeFormatter.ofPattern("h:mma"));
            /* When computing the start time of the lab/activity we are assuming that the lab/activity takes equally long.*/
            this.startTimeLabAct = this.endTimeLabAct.minusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
            /* create BitSet for the lab/lec */
            this.nonLecMonday = this.lecMonday;
            this.nonLecTuesday = this.lecTuesday;
            this.nonLecWednesday = this.lecWednesday;
            this.nonLecThursday = this.lecThursday;
            this.nonLecFriday = this.lecFriday;
            this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(lecHours * 2),
                    this.nonLecMonday, this.nonLecTuesday, this.nonLecWednesday, this.nonLecThursday, this.nonLecFriday);
        }

        /*we assume that the whole block will be occupied by whoever is assigned it*/
        this.allTimesBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(lecHours * 2),
                this.lecMonday, this.lecTuesday, this.lecWednesday, this.lecThursday, this.lecFriday);
        /*if there was a second timeslot we have to join it*/
        if(secondSlot){
            this.allTimesBitSet.or(this.labActBitSet);
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
