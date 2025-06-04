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
    public LocalTime startTimeLec;
    public LocalTime endTimeLec;
    public BitSet lectureBitSet;
    public boolean onlyLec;
    public LocalTime startTimeLabAct;
    public LocalTime endTimeLabAct;
    public BitSet labActBitSet;
    public BitSet allTimesBitSet;
    /*I should make these days into a class or something*/
    public boolean lecMonday, lecTuesday, lecWednesday, lecThursday, lecFriday;
    public boolean nonLecMonday, nonLecTuesday, nonLecWednesday, nonLecThursday, nonLecFriday;
    public float lecHours;
    public float totalHours;
    public float totalHours2;
    public boolean secondSlot;

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

    /*TODO add sanity checker here to throw an error if the second time slot overlaps with the first*/
    public Timeslot(int ID, String days, String startTime, String endTime, float lecHours, float totalHours,
                    String days2, String startTime2, String endTime2, float lecture_hours2, float total_hours2)
            throws Exception{

        /*this variable will be used to see if the entry has two timeslots linked,
        * used later for calculating the complete bitset representation of the timeslot*/
        this.secondSlot = false;

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
        /*TODO not sure about this*/
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
                this.secondSlot = true;
                this.onlyLec = false;
                this.totalHours2 = total_hours2;
                this.nonLecMonday = this.nonLecTuesday = this.nonLecWednesday = this.nonLecThursday = this.nonLecFriday = false;
                if(days2.contains("M")){
                    this.nonLecMonday = true;
                }
                if(days2.contains("T")){
                    this.nonLecTuesday = true;
                }
                if(days2.contains("W")){
                    this.nonLecWednesday = true;
                }
                if(days2.contains("R")){
                    this.nonLecThursday = true;
                }
                if(days2.contains("F")){
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
        this.allTimesBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(totalHours * 2),
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


    public int getID() {
        return ID;
    }

    public LocalTime getStartTimeLec() {
        return startTimeLec;
    }

    public LocalTime getEndTimeLec() {
        return endTimeLec;
    }

    public BitSet getLectureBitSet() {
        return lectureBitSet;
    }

    public boolean isOnlyLec() {
        return onlyLec;
    }

    public LocalTime getStartTimeLabAct() {
        return startTimeLabAct;
    }

    public LocalTime getEndTimeLabAct() {
        return endTimeLabAct;
    }

    public BitSet getLabActBitSet() {
        return labActBitSet;
    }

    public BitSet getAllTimesBitSet() {
        return allTimesBitSet;
    }

    public boolean isLecMonday() {
        return lecMonday;
    }

    public boolean isLecTuesday() {
        return lecTuesday;
    }

    public boolean isLecWednesday() {
        return lecWednesday;
    }

    public boolean isLecThursday() {
        return lecThursday;
    }

    public boolean isLecFriday() {
        return lecFriday;
    }

    public boolean isNonLecMonday() {
        return nonLecMonday;
    }

    public boolean isNonLecTuesday() {
        return nonLecTuesday;
    }

    public boolean isNonLecWednesday() {
        return nonLecWednesday;
    }

    public boolean isNonLecThursday() {
        return nonLecThursday;
    }

    public boolean isNonLecFriday() {
        return nonLecFriday;
    }

    public float getLecHours() {
        return lecHours;
    }

    public float getTotalHours() {
        return totalHours;
    }

    public float getTotalHours2() {
        return totalHours2;
    }

    public boolean isSecondSlot() {
        return secondSlot;
    }


}
