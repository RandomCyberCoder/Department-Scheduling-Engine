package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

public class Timeslot {

    @PlanningId
    private String id;
    private int ID;
    public LocalTime startTimeLec;
    public LocalTime endTimeLec;
    public BitSet lectureBitSet;
    public boolean hasLec;
    public boolean hasLabAct;
    public LocalTime startTimeLabAct;
    public LocalTime endTimeLabAct;
    public BitSet labActBitSet;
    public BitSet allTimesBitSet;
    /*I should make these days into a class or something*/
    private EnumSet<Days> lecDays;
    private EnumSet<Days> nonLecDays;
    /**
     * Amount of hours per day in portion one of this timeslot;
     * usually for lecture but possibly for studio space
     */
    public float lecHours;
    /**
     * Amount of hours per day in the second portion of this timeslot if any;
     * currently used only for lab/act
     */
    private float labActHours;
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
    public static Timeslot test_lecLabBitAndDays(int ID, BitSet lecBitSet, BitSet labActBitSet
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
        this.hasLec = !this.lecDays.isEmpty();
        this.hasLabAct = !this.nonLecDays.isEmpty();
    }

    /**
     * test constructor
     * @param ID
     * @param lecBitSet
     * @param labActBitSet
     * @param lecDays
     * @param labActDays
     */
    private Timeslot(int ID, BitSet lecBitSet, BitSet labActBitSet, EnumSet<Days> lecDays , EnumSet<Days> labActDays){
        this.id = Integer.toString(ID);
        this.ID = ID;
        this.lectureBitSet = (BitSet) lecBitSet.clone();
        this.labActBitSet = (BitSet) labActBitSet.clone();
        this.lecDays = lecDays.clone();
        this.nonLecDays = labActDays.clone();
        this.hasLec = !this.lecDays.isEmpty();
        this.hasLabAct = !this.nonLecDays.isEmpty();
        BitSet allBitSet = new BitSet();
        allBitSet.or(this.lectureBitSet);
        allBitSet.or(this.labActBitSet);
        this.allTimesBitSet = allBitSet;
        this.lecHours = lecBitSet.cardinality() /(float)lecDays.size() / 2f;
        this.labActHours = labActBitSet.cardinality() /(float)labActDays.size() /2f;
    }

    /**
     * test constructor
     * @param id
     */
    private Timeslot(String id){
        this.id = id;
    }


    public Timeslot(int ID, String days, String startTime, String endTime, float lecHours, float totalHours,
                    String days2, String startTime2, String endTime2, float lab_hours)
            throws Exception{

        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("h:mma");
        //by default a timeslot has neither
        this.hasLec = false;
        this.hasLabAct = false;
        this.lectureBitSet = new BitSet();
        this.labActBitSet = new BitSet();
        this.allTimesBitSet = new BitSet();
        this.lecDays = EnumSet.noneOf(Days.class);
        this.nonLecDays = EnumSet.noneOf(Days.class);

        this.ID = ID;
        this.id = String.valueOf(ID);
        //check the first subslot
        if(!days.isBlank()){
            this.hasLec = true;
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

            this.lecHours = lecHours;

            /*check start and end time for the lab and possibly for the lab/activity */
            this.startTimeLec = LocalTime.parse(startTime.trim(), FORMATTER);
            /*LocalTime is immutable so doing this won't modify startTimeLec*/
            this.endTimeLec = startTimeLec.plusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
            /* initialize the lecture BitSet, multiply lecHours by 2 because we need then number of 30 minute blocks */
            this.lectureBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(this.lecHours * 2),
                    this.lecDays);

            //if the total hours is greater than the lecHours then there is extra time for lab in this subslot
            /* We do the following comparison instead of lec_hours == total_hours because of floating point errors */
            if(Math.abs(lecHours - totalHours) > FLOAT_TIME_DELTA){
                this.hasLabAct = true;
                /* end time of the timeslot is when the lab will end */
                this.endTimeLabAct = LocalTime.parse(endTime.trim(), FORMATTER);
                /* When computing the start time of the lab/activity we are assuming that the lab/activity takes equally long.
                 * This doesn't necessarily start right after the time the lecture ends. We could have a gap (i.e. like during
                 * Tuesday and Thursday)*/
                this.startTimeLabAct = this.endTimeLabAct.minusMinutes(Math.round(MINUTES_PER_HOUR * this.lecHours));
                /* create BitSet for the lab/lec */
                this.nonLecDays = this.lecDays;
                this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(this.lecHours * 2),
                        this.nonLecDays);
                this.labActHours = this.lecHours;
            }
        }

        if(!days2.isBlank()){
            //sanity check; avoids accidentally having two lab/act timeslots
            if(this.hasLabAct) throw new RuntimeException("When creating a timeslot an error occurred. Timeslot had a " +
                    "lab activity set in the first and second sub slot.");
            this.hasLabAct = true;
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

            /*Hours per day for second timeslot are assumed to be dedicated towards labs/acts */
            this.labActHours = lab_hours;
            this.startTimeLabAct = LocalTime.parse(startTime2.trim(), FORMATTER);
            this.endTimeLabAct = LocalTime.parse(endTime2.trim(), FORMATTER);
            this.labActBitSet = BitSetHelper.timeSlotBitSet(this.startTimeLabAct, Math.round(lab_hours * 2),
                    this.nonLecDays);
        }

        //sanity check if user overlapped the lec and lab/act sub timeslots
        if(this.labActBitSet.intersects(this.lectureBitSet)) throw new RuntimeException("Error creating timeslot. " +
            "The lecture and lab/act times overlap");

        if(!days.isBlank()){
            /*we assume that the whole block will be occupied by whoever is assigned it*/
            this.allTimesBitSet.or(BitSetHelper.timeSlotBitSet(this.startTimeLec, Math.round(totalHours * 2),
                    this.lecDays));
        }
        //OR with lab/act bitset if the lab/act time was in the second subplot instead of the first
        if(!days2.isBlank()){
            this.allTimesBitSet.or(this.labActBitSet);
        }
    }


    /**
     * <p>Check that is timeslot is continuous; i.e. timeslot is for one day and the time
     * is dedicated to either a lab or lecture.</p>
     * <p>NOTE: if the timeslot was read in having two slots. Then it will auto be marked
     * as not continuous even if the time is back to back. Assumed they are meant for separate
     * portions of a course; i.e. lecture and labs</p>
     * <p>NOTE: if the timeslot is only one hour long on a single days it won't be marked as continuous</p>
     * @return True if continuous; Otherwise false. One hour long (continuous) single day timeslots are marked
     * as not continuous;
     */
    public boolean isContinuous(){
        if(lecDays.size() != 1 || !nonLecDays.isEmpty()) return false;

        BitSet potentialBitSet = lectureBitSet;
        int indexFirstBit = potentialBitSet.nextSetBit(0);
        int cardinality = potentialBitSet.cardinality();
        BitSet mask = new BitSet();
        mask.set(indexFirstBit, indexFirstBit + cardinality);
        mask.and(potentialBitSet);

        if(cardinality <= 2) return false;

        return mask.cardinality() == cardinality;

    }

    @Override
    public String toString() {
        return this.toStringLec() + " ---- " + this.toStringLabAct();
    }

    public String toStringLec(){
        if(!this.hasLec) return "No lec time";
        return getString(lecDays, startTimeLec, endTimeLec);
    }

    public String toStringLabAct(){
        if(!this.hasLabAct) return "No lab/act time";

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

    public boolean isHasLec() {
        return hasLec;
    }

    public boolean isHasLabAct() {
        return hasLabAct;
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


    /**
     * @return number of lec hours the timeslot can accommodate per day
     */
    public float getLecHours() {
        if(!this.hasLec) return 0;
        else return lecHours;
    }

    /**
     * @return number of lab/act hours the timeslot can accommodate per day
     */
    public float getLabActHours(){
        if(!this.hasLabAct) return 0;
        else return labActHours;
    }

    public EnumSet<Days> getLecDays() {
        return lecDays;
    }

    public EnumSet<Days> getNonLecDays() {
        return nonLecDays;
    }
}
