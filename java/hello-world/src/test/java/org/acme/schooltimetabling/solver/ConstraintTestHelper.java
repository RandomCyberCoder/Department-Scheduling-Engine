package org.acme.schooltimetabling.solver;

import org.acme.schooltimetabling.builders.teachers.TeacherBuilder;
import org.acme.schooltimetabling.builders.teachers.policies.DefaultTeachingPolicy;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.glassfish.jaxb.runtime.v2.runtime.reflect.opt.Const;

import java.time.LocalTime;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

public class ConstraintTestHelper {
    public static final EnumSet<Days> NO_DAYS = EnumSet.noneOf(Days.class);
    public static final BitSet EMPTY_BS = new BitSet();
    public final static String DUMMY_STUDIO = "dummyStudioCourse";
    public final static Room DUMMY_ROOM = new Room("1", "dummyRoom", 1);
    public final static Teacher DUMMY_TEACHER = new TeacherBuilder(new DefaultTeachingPolicy())
            .preference(new BitSet())
            .acceptable(new BitSet())
            .conflict(new BitSet())
            .gapPref(Preference.NEUTRAL)
            .canon("dummyInstructor")
            .build();
    public final static Timeslot DUMMY_TS = Timeslot.test_lecLabBitAndDays(999, EMPTY_BS, EMPTY_BS, NO_DAYS, NO_DAYS);
    /**
     * Test: room name; will be used by a specific course {@link #TEST_L_W_LAB_SPECIFIC}
     */
    public final static String TEST_NAME_LAB_ROOM_SPECIFIC = "LabRoom";
    /**
     * Test: name of lab room not assigned to an course specifically
     */
    public final static String TEST_NAME_RANDOM_LAB_ROOM = "Random lab room";
    /**
     * Test: name of prescheduled ROOM
     */
    public final static String TEST_NAME_PRESCHEDULED_ROOM = "Room Prescheduled";
    public final static Set<String> TEST_SET_LAB_ROOMS = Set.of(TEST_NAME_LAB_ROOM_SPECIFIC);
    /**
     * Test: name of a course that has a specific lab*/
    public final static String TEST_L_W_LAB_SPECIFIC = "Lab/Act course";
    /**
     * Room that can be used by any lab
     */
    public static Room TEST_ROOM_RANDO_LAB;
    /**
     * Room that is used for a course specifically and can also be used by an lab
     */
    public static Room TEST_ROOM_SPECIFIC;
    /**
     * Room prescheduled
     */
    public static Room TEST_ROOM_PRESCHEDULED;
    public static String NON_STUDIO_SPECIFIC = "non studio course with a specific room";

    static{
        //add rooms to those that are possible
        Constants.POSSIBLE_ROOMS.add(TEST_NAME_LAB_ROOM_SPECIFIC);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_NAME_LAB_ROOM_SPECIFIC, 1000);

        Constants.POSSIBLE_ROOMS.add(TEST_NAME_RANDOM_LAB_ROOM);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_NAME_RANDOM_LAB_ROOM, 1001);

        Constants.POSSIBLE_ROOMS.add(TEST_NAME_PRESCHEDULED_ROOM);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_NAME_PRESCHEDULED_ROOM, 1002);

        //add course names to their ID
        Constants.COURSE_ID_BIMAP.put(DUMMY_STUDIO, 2000);
        Constants.COURSE_ID_BIMAP.put(NON_STUDIO_SPECIFIC, 2001);

        //add which lessons are studios
        Constants.STUDIO_STYLE_COURSES.add(DUMMY_STUDIO);
        Constants.STUDIO_STYLE_COURSES.add(TEST_L_W_LAB_SPECIFIC);

        //add mapping for which courses can be in certain rooms
        Constants.COURSE_TO_ROOMS.put(TEST_L_W_LAB_SPECIFIC, TEST_SET_LAB_ROOMS);
        Constants.COURSE_TO_ROOMS.put(NON_STUDIO_SPECIFIC, TEST_SET_LAB_ROOMS);

        //create the room objects
        TEST_ROOM_RANDO_LAB = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_NAME_RANDOM_LAB_ROOM)),
                TEST_NAME_RANDOM_LAB_ROOM, Constants.ROOM_TO_ID_BIMAP.get(TEST_NAME_RANDOM_LAB_ROOM));
        TEST_ROOM_SPECIFIC = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_NAME_LAB_ROOM_SPECIFIC)),
                TEST_NAME_LAB_ROOM_SPECIFIC, Constants.ROOM_TO_ID_BIMAP.get(TEST_NAME_LAB_ROOM_SPECIFIC));
        TEST_ROOM_PRESCHEDULED = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_NAME_LAB_ROOM_SPECIFIC)),
                TEST_NAME_LAB_ROOM_SPECIFIC, Constants.ROOM_TO_ID_BIMAP.get(TEST_NAME_LAB_ROOM_SPECIFIC));

        //let's preschedule a room
        EnumSet<Days> MWF = EnumSet.of(Days.MONDAY, Days.WEDNESDAY, Days.FRIDAY);
        BitSet roomBlocked = BitSetHelper.timeSlotBitSet(LocalTime.parse("9:00AM", Constants.TIME_FORMATTER), 2, MWF);
        TEST_ROOM_PRESCHEDULED.prescheduleUpdate(roomBlocked);


        Constants.TESTING = true;
    }
    public static void load(){

    }
}
