package org.acme.schooltimetabling.solver;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.constants.Days;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.teacher.Teacher;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

public class ConstraintTestHelper {
    public static final EnumSet<Days> NO_DAYS = EnumSet.noneOf(Days.class);
    public static final BitSet EMPTY_BS = new BitSet();
    public final static String DUMMY_STUDIO = "dummyStudioCourse";
    public final static Room DUMMY_ROOM = new Room("1", "dummyRoom", 1);
    public final static Teacher DUMMY_TEACHER = new Teacher(
            1, "dummyInstructor", EMPTY_BS, EMPTY_BS, EMPTY_BS);

    public final static int DUMMY_LINKER = 1;
    /**
     * Test: room name; will be used by a specific course {@link #TEST_L_W_LAB_SPECIFIC}
     */
    public final static String TEST_LAB_ROOM = "LabRoom";
    /**
     * Test: name of lab room not assigned to an course specifically
     */
    public final static String TEST_RANDOM_LAB_ROOM = "Random lab room";
    public final static Set<String> TEST_SET_LAB_ROOMS = Set.of(TEST_LAB_ROOM);
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

    static{
        Constants.POSSIBLE_ROOMS.add(TEST_LAB_ROOM);
        Constants.POSSIBLE_ROOMS.add(TEST_RANDOM_LAB_ROOM);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_RANDOM_LAB_ROOM, 1001);
        Constants.ROOM_TO_ID_BIMAP.put(TEST_LAB_ROOM, 1000);

        Constants.COURSE_ID_BIMAP.put(DUMMY_STUDIO, 2000);
        Constants.STUDIO_STYLE_COURSES.add(DUMMY_STUDIO);
        Constants.STUDIO_STYLE_COURSES.add(TEST_L_W_LAB_SPECIFIC);
        Constants.COURSE_TO_ROOMS.put(TEST_L_W_LAB_SPECIFIC, TEST_SET_LAB_ROOMS);

        TEST_ROOM_RANDO_LAB = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_RANDOM_LAB_ROOM)),
                TEST_RANDOM_LAB_ROOM, Constants.ROOM_TO_ID_BIMAP.get(TEST_RANDOM_LAB_ROOM));
        TEST_ROOM_SPECIFIC = new Room(Integer.toString(Constants.ROOM_TO_ID_BIMAP.get(TEST_LAB_ROOM)),
                TEST_LAB_ROOM, Constants.ROOM_TO_ID_BIMAP.get(TEST_LAB_ROOM));

        Constants.TESTING = true;
    }
    public static void load(){

    }
}
