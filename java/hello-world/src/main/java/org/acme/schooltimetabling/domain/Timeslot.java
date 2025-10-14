package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Timeslot {

    @PlanningId
    private String id;
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
    private EnumSet<Days> lecDays;
    private EnumSet<Days> nonLecDays;
    public float lecHours;
    public float totalHours;
    public float totalHours2;
    public boolean secondSlot;
    private static final float FLOAT_TIME_DELTA = 0.01f;
    private static final int MINUTES_PER_HOUR = 60;

    /**
     * Default constructor shouldn't be accessed
     * */
    private Timeslot() {
    }


    /* Test factory method lesson builders */

    /**
     * test factory method
     * @param ID
     * @param lecDays
     * @param labDays
     * @return
     */
    public static Timeslot test_CreateWithDaysOnly(int ID, EnumSet<Days> lecDays, EnumSet<Days> labDays){
        return new Timeslot(ID, lecDays, labDays);
    }

    /**
     * test factory method
     * @param ID
     * @param lecBitSet
     * @param labActBitSet
     * @param lecDays
     * @param labDays
     * @return
     */
    public static Timeslot test_lacLabBitAndDays(int ID, BitSet lecBitSet, BitSet labActBitSet
            , EnumSet<Days> lecDays , EnumSet<Days> labDays){
        return new Timeslot(ID, lecBitSet, labActBitSet, lecDays, labDays);
    }

    /**
     * test factory method
     * @param id
     * @return
     */
    public static Timeslot test_minSetUp(String id){
        return new Timeslot(id);
    }


    /*Private constructors for test factory methods*/

    /**
     * test constructor
     * @param ID
     * @param lecDays
     * @param labDays
     */
    private Timeslot(int ID, EnumSet<Days> lecDays, EnumSet<Days> labDays){
        this.id = Integer.toString(ID);
        this.lecDays = lecDays.clone();
        this.nonLecDays = labDays.clone();
    }

    /**
     * test constructor
     * @param ID
     * @param lecBitSet
     * @param labActBitSet
     * @param lecDays
     * @param labDays
     */
    private Timeslot(int ID, BitSet lecBitSet, BitSet labActBitSet, EnumSet<Days> lecDays , EnumSet<Days> labDays){
        this.id = Integer.toString(ID);
        this.ID = ID;
        this.onlyLec = labActBitSet.cardinality() == 0;
        this.lectureBitSet = (BitSet) lecBitSet.clone();
        this.labActBitSet = (BitSet) labActBitSet.clone();
        this.lecDays = lecDays.clone();
        this.nonLecDays = labDays.clone();
        BitSet allBitSet = new BitSet();
        allBitSet.or(this.lectureBitSet);
        allBitSet.or(this.labActBitSet);
        this.allTimesBitSet = allBitSet;
        this.lecHours = lecBitSet.cardinality() /(float)lecDays.size() / 2f;
        this.totalHours = labActBitSet.cardinality() /(float)labDays.size() /2f + this.lecHours;
    }

    /**
     * test constructor
     * @param id
     */
    private Timeslot(String id){
        this.id = id;
    }



    /*TODO add sanity checker here to throw an error if the second time slot overlaps with the first*/
    public Timeslot(int ID, String days, String startTime, String endTime, float lecHours, float totalHours,
                    String days2, String startTime2, String endTime2, float lecture_hours2, float total_hours2)
            throws Exception{

        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("h:mma");

        /*this variable will be used to see if the entry has two timeslots linked,
        * used later for calculating the complete bitset representation of the timeslot*/
        this.secondSlot = false;

        this.ID = ID;
        this.id = String.valueOf(ID);
        /*Mark what days the timeslot occupies*/
        lecDays = EnumSet.noneOf(Days.class);
        nonLecDays = EnumSet.noneOf(Days.class);

        if(days.contains("M")){
            lecDays.add(Days.MONDAY);
        }
        if(days.contains("T")){
            lecDays.add(Days.TUESDAY);
        }
        if(days.contains("W")){
            lecDays.add(Days.WEDNESDAY);
        }
        if(days.contains("R")){
            lecDays.add(Days.THURSDAY);
        }
        if(days.contains("F")){
            lecDays.add(Days.FRIDAY);
        }
        /*determine if the timeslot will accommodate only lectures*/
        this.lecHours = lecHours;
        this.totalHours = totalHours;
        /* We do the following comparison instead of lec_hours == total_hours because of floating point errors */
        this.onlyLec = Math.abs(lecHours - totalHours) < FLOAT_TIME_DELTA;

        /*check start and end time for the lab and possibly for the lab/activity */
        this.startTimeLec = LocalTime.parse(startTime.trim(), FORMATTER);
        /*LocalTime is immutable so doing this won't modify startTimeLec*/
        this.endTimeLec = startTimeLec.plusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
        /* initialize the lecture BitSet, multiply lecHours by 2 because we need then number of 30 minute blocks */
        this.lectureBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(lecHours * 2),
                this.lecDays);

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
                if(days2.contains("M")){
                    this.nonLecDays.add(Days.MONDAY);
                }
                if(days2.contains("T")){
                    this.nonLecDays.add(Days.TUESDAY);
                }
                if(days2.contains("W")){
                    this.nonLecDays.add(Days.WEDNESDAY);
                }
                if(days2.contains("R")){
                    this.nonLecDays.add(Days.THURSDAY);
                }
                if(days2.contains("F")){
                    this.nonLecDays.add(Days.FRIDAY);
                }

                this.startTimeLabAct = LocalTime.parse(startTime2.trim(), FORMATTER);
                this.endTimeLabAct = LocalTime.parse(endTime2.trim(), FORMATTER);
                this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(total_hours2 * 2),
                        this.nonLecDays);
            }
        }
        else{
            /* end time of the timeslot is when the lab will end */
            this.endTimeLabAct = LocalTime.parse(endTime.trim(), FORMATTER);
            /* When computing the start time of the lab/activity we are assuming that the lab/activity takes equally long.
            * This doesn't necessarily start right after the time the lecture ends. We could have a gap (i.e. like during
            * Tuesday and Thursday)*/
            this.startTimeLabAct = this.endTimeLabAct.minusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
            /* create BitSet for the lab/lec */
            this.nonLecDays = this.lecDays;
            this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(lecHours * 2),
                    this.nonLecDays);
        }

        /*we assume that the whole block will be occupied by whoever is assigned it*/
        this.allTimesBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(totalHours * 2),
                this.lecDays);
        /*if there was a second timeslot we have to join it*/
        if(!onlyLec){
            this.allTimesBitSet.or(this.labActBitSet);
        }
    }


    @Override
    public String toString() {
        String lecStr = "lecture: " + lecDays.toString() + " " + startTimeLec.toString();
        String labActStr = this.onlyLec ? "" :
                (" ---- " + "lab: " + nonLecDays.toString() + startTimeLabAct.toString());
        return lecStr + labActStr;
    }

    public String toStringLec(){
        return getString(lecDays, startTimeLec, endTimeLec);
    }

    public String toStringLabAct(){
        if(onlyLec) return "";

        return getString(nonLecDays, startTimeLabAct, endTimeLabAct);
    }

    private String getString(EnumSet<Days> nonLecDays, LocalTime startTime, LocalTime endTime) {
        StringBuilder buildLecRep = new StringBuilder();

        if(nonLecDays.contains(Days.MONDAY)) buildLecRep.append('M');
        if(nonLecDays.contains(Days.TUESDAY)) buildLecRep.append('T');
        if(nonLecDays.contains(Days.WEDNESDAY)) buildLecRep.append('W');
        if(nonLecDays.contains(Days.THURSDAY)) buildLecRep.append('R');
        if(nonLecDays.contains(Days.FRIDAY)) buildLecRep.append('F');

        buildLecRep.append(" ");
        buildLecRep.append(startTime.toString());
        buildLecRep.append(" - ").append(endTime.toString());

        return buildLecRep.toString();
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getId() {
        return id;
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

    public EnumSet<Days> getLecDays() {
        return lecDays;
    }

    public EnumSet<Days> getNonLecDays() {
        return nonLecDays;
    }
}
