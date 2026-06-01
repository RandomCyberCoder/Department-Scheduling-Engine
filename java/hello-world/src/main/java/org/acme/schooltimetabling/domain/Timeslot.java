package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.EnumSet;

public class Timeslot {
    public class Partition{
        private EnumSet<Days> days = EnumSet.noneOf(Days.class);
        private LocalTime startTime = null;
        private LocalTime endTime = null;
        private BitSet bitSet = new BitSet();
        /**
         * Amount of hours per day for this partition
         */
        private float hours = 0f;

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public EnumSet<Days> getDays() {
            return days;
        }

        public BitSet getBitSet() {
            return bitSet;
        }

        public float getHours() {
            return hours;
        }

        public boolean isContinuous(){
            return days.size() == 1 && bitSet.cardinality() > 2;
        }
    }

    /*TODO checklist
    *  -convert names having lecture or lab/act to first slot and second slot; maybe rething names
    *  -update callers for getting previous lecture or lab/act bitsets
    *   Note that we shouldn't try to do any smart behavior like returning lab/act time
    *   if we have only one slot and also returning lecture time if we have only one slot this will be messy
    *   This should be handle by callers to decide how they want to handle this info
    *  -we might also want to update the parsers to reflect this; this should just boil down to the header names
    *  -merge attributes into the Partition slot
    *  +Update the constraints that are accessing the times. We might want to look into adding another constraint
    *   so we can divided up responsibilities*/
    @PlanningId
    private String id;
    private int ID;

    private Partition partition1;
    private Partition partition2;

    public boolean hasSlot2;

    public BitSet allTimesBitSet;

    //static vars
    private static final float EPSILON = 0.01f;
    private static final int MINUTES_PER_HOUR = 60;
    private static final LocalTime EARLIEST_TIME = LocalTime.parse("7:00AM", Constants.TIME_FORMATTER);
    private static final LocalTime LATEST_TIME = LocalTime.parse("10:00PM", Constants.TIME_FORMATTER);

    /**
     * Default constructor shouldn't be accessed
     * */
    private Timeslot() {
    }


    /*----------------------------- Test Stuff ----------------------------- */

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
     */
    private Timeslot(int ID, EnumSet<Days> daysSlot1, EnumSet<Days> daysSlot2){
        if(daysSlot1.isEmpty()) throw new IllegalArgumentException("Slot1 must always include a time");
        this.partition1 = new Partition();
        this.partition2 = new Partition();
        this.id = Integer.toString(ID);
        this.partition1.days = daysSlot1.clone();
        this.partition2.days = daysSlot2.clone();
        this.hasSlot2 = !this.partition2.days.isEmpty();
    }

    /**
     * test constructor
     */
    private Timeslot(int ID, BitSet bitset1, BitSet bitSetSlot2, EnumSet<Days> daysSlot1, EnumSet<Days> daysSlot2){
        if(daysSlot1.isEmpty()) throw new IllegalArgumentException("Slot1 must always include a time");

        this.partition1 = new Partition();
        this.partition2 = new Partition();
        this.id = Integer.toString(ID);
        this.ID = ID;
        this.partition1.bitSet = (BitSet) bitset1.clone();
        this.partition2.bitSet = (BitSet) bitSetSlot2.clone();
        this.partition1.days = daysSlot1.clone();
        this.partition2.days = daysSlot2.clone();
        this.hasSlot2 = !this.partition2.days.isEmpty();
        BitSet allBitSet = new BitSet();
        allBitSet.or(this.partition1.bitSet);
        allBitSet.or(this.partition2.bitSet);
        this.allTimesBitSet = allBitSet;
        this.partition1.hours = bitset1.cardinality() /(float) daysSlot1.size() / 2f;
        this.partition2.hours = bitSetSlot2.cardinality() /(float)daysSlot2.size() /2f;
    }

    /**
     * test constructor
     */
    private Timeslot(String id){
        this.id = id;
    }

    /*----------------------------- End of Test Stuff ----------------------------- */


    /**
     * Main constructor for creating timeslots
     */
    public Timeslot(int ID, String days, String startTime, String endTime, float startHours, float totalHours,
                    String days2, String startTime2, String endTime2, float totalHours2){
        if(days.isBlank()) throw new IllegalArgumentException("The first portion of the timeslot config entry must be " +
                "populated");

        final DateTimeFormatter FORMATTER = Constants.TIME_FORMATTER;

        //parse raw inputs
        EnumSet<Days> daysSlot1 = parseDayString(days.strip());
        LocalTime startTimeSlot1 = LocalTime.parse(startTime.strip(), FORMATTER);
        LocalTime endTimeSlot1 = LocalTime.parse(endTime.strip(), FORMATTER);

        EnumSet<Days> daysSlot2 = parseDayString(days2.strip());
        LocalTime startTimeSlot2;
        LocalTime endTimeSlot2;
        if(daysSlot2.isEmpty()) startTimeSlot2 = endTimeSlot2 = null;
        else{
            startTimeSlot2 = LocalTime.parse(startTime2.strip(), FORMATTER);
            endTimeSlot2 = LocalTime.parse(endTime2.strip(), FORMATTER);
        }

        //validate parsed raw inputs
        validate(daysSlot1, startTimeSlot1, endTimeSlot1, startHours, totalHours,
                daysSlot2, startTimeSlot2, endTimeSlot2, totalHours2);

        partition1 = new Partition();
        partition2 = new Partition();

        this.ID = ID;
        this.id = String.valueOf(ID);
        //process parsed inputs after we have validated them;
        if(days2.isEmpty()){
            long startMinutes = Math.round(startHours * 60f);
            long totalMinutes = Math.round(totalHours * 60f);

            //scenario where starthours is the whole timeslot
            if(startMinutes == totalMinutes){
                this.hasSlot2 = false;

                this.partition1.days = daysSlot1;
                this.partition1.hours = totalHours;
                this.partition1.startTime = startTimeSlot1;
                this.partition1.endTime = endTimeSlot1;
                this.partition1.bitSet = BitSetHelper.timeSlotBitSet(
                        this.partition1.startTime,
                        Math.round(this.partition1.hours * 2f),
                        this.partition1.days
                );

                this.partition2.days = EnumSet.noneOf(Days.class);
                this.partition2.startTime = null;
                this.partition2.endTime = null;
                this.partition2.hours = 0f;
                this.partition2.bitSet = new BitSet();

                this.allTimesBitSet = new BitSet();
                this.allTimesBitSet.or(this.partition1.bitSet);
            }
            //other scenario when it doesn't
            //this means we have the slot1 and slot2 populated
            else{
                this.hasSlot2 = true;

                this.partition1.days = daysSlot1;
                this.partition1.hours = startHours;
                this.partition1.startTime = startTimeSlot1;
                this.partition1.endTime = this.partition1.startTime.plusMinutes(
                        Math.round(this.partition1.hours * 60f)
                );
                this.partition1.bitSet = BitSetHelper.timeSlotBitSet(
                        this.partition1.startTime,
                        Math.round(this.partition1.hours * 2f),
                        this.partition1.days
                );

                this.partition2.days = daysSlot1;
                this.partition2.hours = startHours;
                this.partition2.startTime = endTimeSlot1.minusMinutes(
                        Math.round(this.partition2.hours * 60f)
                );
                this.partition2.endTime = endTimeSlot1;
                this.partition2.bitSet = BitSetHelper.timeSlotBitSet(
                        this.partition2.startTime,
                        Math.round(this.partition2.hours * 2f),
                        this.partition2.days
                );

                this.allTimesBitSet = BitSetHelper.timeSlotBitSet(
                        this.partition1.startTime,
                        Math.round(totalHours * 2f),
                        this.partition1.days
                );
            }
        }
        else{
            this.hasSlot2 = true;

            this.partition1.days = daysSlot1;
            this.partition1.hours = totalHours;
            this.partition1.startTime = startTimeSlot1;
            this.partition1.endTime = endTimeSlot1;
            this.partition1.bitSet = BitSetHelper.timeSlotBitSet(
                    this.partition1.startTime,
                    Math.round(this.partition1.hours * 2),
                    this.partition1.days
            );

            this.partition2.days = daysSlot2;
            this.partition2.hours = totalHours2;
            this.partition2.startTime = startTimeSlot2;
            this.partition2.endTime = endTimeSlot2;
            this.partition2.bitSet = BitSetHelper.timeSlotBitSet(
                    this.partition2.startTime,
                    Math.round(this.partition2.hours * 2),
                    this.partition2.days
            );

            this.allTimesBitSet = new BitSet();
            this.allTimesBitSet.or(this.partition1.bitSet);
            this.allTimesBitSet.or(this.partition2.bitSet);
        }
    }

    private EnumSet<Days> parseDayString(String parse){
        EnumSet<Days> res = EnumSet.noneOf(Days.class);

        for(char day : parse.toUpperCase().toCharArray()){
            if(day == 'M'){
                res.add(Days.MONDAY);
            }
            else if(day == 'T'){
                res.add(Days.TUESDAY);
            }
            else if(day == 'W'){
                res.add(Days.WEDNESDAY);
            }
            else if(day == 'R'){
                res.add(Days.THURSDAY);
            }
            else if(day == 'F'){
                res.add(Days.FRIDAY);
            }
            else{
                throw new RuntimeException("Days can only include (case-insensitive) 'm' [Monday], " +
                        "'t' [Tuesdays], 'W' [Wednesday], 'r' [Thursday], 'f' [Friday].");
            }
        }

        return res;
    }

    /**
     * used to validate the parsed values given in the constructor, it will throw errors if an invalid setup
     * is found
     */
    private static void validate(
            EnumSet<Days> days, LocalTime startTime, LocalTime endTime, float startHours, float totalHours,
            EnumSet<Days> days2, LocalTime startTime2, LocalTime endTime2, float totalHours2) {

        //validate individual slots
        validatePartition(days, startTime, endTime, startHours, totalHours, "first");
        if(!days2.isEmpty()){
            validatePartition(days2, startTime2, endTime2, totalHours2, totalHours2, "second");

            //make sure that the bitsets don't overlap if the second portion was given
            BitSet part1 = BitSetHelper.timeSlotBitSet(startTime, Math.round(startHours * 2f), days);
            BitSet part2 = BitSetHelper.timeSlotBitSet(startTime2, Math.round(totalHours * 2f), days2);
            if(part1.intersects(part2)) throw new RuntimeException("Both portions given in the timeslot config overlap");
            //if we use the second partition then the first time slots total hours should all be used up by startHours
            try{
                validatePartition(days, startTime, endTime, startHours, startHours, "first");
            } catch (Exception e) {
                throw new RuntimeException("If using both partitions for a timeslot setup, the first partition should " +
                        "have startHours equal totalHours.");
            }
        }
    }

    private static void validatePartition(
            EnumSet<Days> days, LocalTime startTime, LocalTime endTime, float startHours, float totalHours, String label) {
        long actualMinutes = Duration.between(startTime, endTime).toMinutes();
        long expectedMinutes = Math.round(totalHours * 60f);

        if(days == null || days.isEmpty()) {
            throw new IllegalArgumentException(label + " days must not be empty");
        }

        if(!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("The end time of a timeslot must come after the start time");
        }

        if(startTime.isBefore(EARLIEST_TIME) || endTime.isAfter(LATEST_TIME)){
            throw new IllegalArgumentException("The time specified must be between 7AM and 10PM inclusive");
        }

        if(startHours <= EPSILON || totalHours <= EPSILON || startHours > totalHours + EPSILON){
            throw new IllegalArgumentException("Start hours and total hours must be positive. " +
                    "Total hours must be at least start hours.");
        }

        if(!isMultipleOfHalfHour(startHours) || !isMultipleOfHalfHour(totalHours)) {
            throw new IllegalArgumentException("Start hours must be in 0.5 hour increments");
        }

        if(!approximatelyEqual(startHours, totalHours) && startHours * 2f > totalHours + EPSILON) {
            throw new IllegalArgumentException(
                    "Start hours must either equal total hours or be at most half of total hours. Note everything must" +
                            "be a multiple of .5");
        }

        if (actualMinutes != expectedMinutes) {
            throw new IllegalArgumentException(
                    label + " partition duration must match total hours. Expected " + totalHours +
                            " hours but found " + (actualMinutes / 60f) + " hours.");
        }
    }

    private static boolean isMultipleOfHalfHour(float hours) {
        float doubled = hours * 2f;
        return Math.abs(doubled - Math.round(doubled)) < EPSILON;
    }

    private static boolean approximatelyEqual(float a, float b) {
        return Math.abs(a - b) < EPSILON;
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
    public boolean isContinuousSlot1(){
        return partition1.isContinuous();
    }

    /**
     * Checks that second partition has all its time in one block; all time in one day and all back to back
     * @return true if continuous; otherwise false. It will throw an error if there is no second slot.
     */
    public boolean isContinuousSlot2(){
        if(!hasSlot2) throw new RuntimeException(String.format(
                "Timeslot with id '%d' has no second slot",
                ID
        ));
        return partition2.isContinuous();
    }

    @Override
    public String toString() {
        return this.toStringSlot1() + " ---- " + this.toStringSlot2();
    }

    private String toStringSlot1(){
        return getString(
                this.partition1.days,
                this.partition1.startTime,
                this.partition1.endTime
        );
    }

    private String toStringSlot2(){
        if(!this.hasSlot2) return "No second slot";

        return getString(
                this.partition2.days,
                this.partition2.startTime,
                this.partition2.endTime
        );
    }

    private String getString(EnumSet<Days> days, LocalTime startTime, LocalTime endTime) {
        StringBuilder buildLecRep = new StringBuilder();

        if(days.contains(Days.MONDAY)) buildLecRep.append('M');
        if(days.contains(Days.TUESDAY)) buildLecRep.append('T');
        if(days.contains(Days.WEDNESDAY)) buildLecRep.append('W');
        if(days.contains(Days.THURSDAY)) buildLecRep.append('R');
        if(days.contains(Days.FRIDAY)) buildLecRep.append('F');

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

    public LocalTime getStartTimeSlot1() {
        return partition1.startTime;
    }

    public LocalTime getEndTimeSlot1() {
        return partition1.endTime;
    }

    public BitSet getBitSetSlot1() {
        return partition1.bitSet;
    }

    public boolean isHasSlot2() {
        return hasSlot2;
    }

    public LocalTime getStartTimeSlot2() {
        return partition2.startTime;
    }

    public LocalTime getEndTimeSlot2() {
        return partition2.endTime;
    }

    public BitSet getBitSetSlot2() {
        return partition2.bitSet;
    }

    public BitSet getAllTimesBitSet() {
        return allTimesBitSet;
    }


    /**
     * @return number of lec hours the timeslot can accommodate per day
     */
    public float getHoursSlot1() {
        return partition1.hours;
    }

    /**
     * @return number of lab/act hours the timeslot can accommodate per day
     */
    public float getHoursSlot2(){
        if(!this.hasSlot2) return 0;
        else return partition2.hours;
    }

    public EnumSet<Days> getDaysSlot1() {
        return partition1.days;
    }

    public EnumSet<Days> getDaysSlot2() {
        return partition2.days;
    }

    public Partition getPartition1(){
        return partition1;
    }

    public Partition getPartition2(){
        return partition2;
    }
}
