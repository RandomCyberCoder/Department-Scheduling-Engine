package org.acme.schooltimetabling.TestClasses;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.helperClasses.Generators.RoomGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestRooms {
    static ArrayList<Room> roomLst;
    @BeforeAll
    static void setup(){
        roomLst = RoomGenerator.generateRooms();
    }


    @Test
    @DisplayName("Making sure we have the right amount of rooms")
    void roomCount(){
        assertEquals(Constants.POSSIBLE_ROOMS.size(), roomLst.size());
    }

    @Test
    @DisplayName("Checking correct ID")
    void roomName(){
        assertAll("Checking correct id",
                roomLst.stream().map((room -> (Executable) () -> {
                    assertEquals(Constants.ROOM_TO_ID_BIMAP.get(room.name), room.ID);
                })));
    }
}
