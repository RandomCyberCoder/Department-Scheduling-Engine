package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Room;

import java.util.ArrayList;

public class RoomGenerator extends Generator{
    private static Room generateRoom(String roomName){
        int roomID = Constants.ROOM_TO_ID_BIMAP.get(roomName);
        return new Room(String.valueOf(roomID), roomName, roomID);
    }

    public static ArrayList<Room> generateRooms(){
        ArrayList<Room> roomsList = new ArrayList<>();
        Room newRoom;

        for(String room: Constants.POSSIBLE_ROOMS){
            newRoom = generateRoom(room);
            roomsList.add(newRoom);
        }

        return roomsList;
    }
}
