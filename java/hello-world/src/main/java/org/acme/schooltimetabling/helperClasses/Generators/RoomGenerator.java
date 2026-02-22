package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Room;

import java.util.ArrayList;

public class RoomGenerator extends Generator{
    /**
     * Generates a list of rooms that are specified in the {@link Constants#POSSIBLE_ROOMS Constants.POSSIBLE_ROOMS}
     * constant. This includes all lab rooms and a special room used for lecture only courses
     * @return A list of rooms courses can be placed into
     */
    public static ArrayList<Room> generateRooms(){
        ArrayList<Room> roomsList = new ArrayList<>();
        Room newRoom;

        for(String room: Constants.POSSIBLE_ROOMS){
            newRoom = generateRoom(room);
            roomsList.add(newRoom);
        }

        return roomsList;
    }

    /**
     * Creates a room for the room with name <i>roomName</i>. The Room object is given the id
     * {@link Constants#ROOM_TO_ID_BIMAP Constants.ROOM_TO_ID_BIMAP} has reserved for it.
     *
     * @param roomName Name of room to be created
     * @return a Room object
     */
    private static Room generateRoom(String roomName){
        int roomID = Constants.ROOM_TO_ID_BIMAP.get(roomName);
        return new Room(String.valueOf(roomID), roomName, roomID);
    }

}
