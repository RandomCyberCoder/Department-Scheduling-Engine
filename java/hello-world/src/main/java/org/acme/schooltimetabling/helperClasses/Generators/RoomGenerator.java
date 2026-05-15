package org.acme.schooltimetabling.helperClasses.Generators;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.helperClasses.BitSetHelper;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.PrescheduleObject;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.apache.commons.math3.random.BitsStreamGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;

public class RoomGenerator extends Generator{
    private static final PrescheduleObject PRESCHED_TIMES = ScheduleConfig.getPrescheduledFileName() != null ?
            ParseInput.readPrescheduledFile() : null;
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomGenerator.class);

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
        Room room = new Room(String.valueOf(roomID), roomName, roomID);

        //check for prescheduling
        if(PRESCHED_TIMES != null && PRESCHED_TIMES.getRooms().containsKey(roomName)){
            LOGGER.info("Found prescheduling times for room '{}'. Using prescheduling times.", roomName);
            for(PrescheduleObject.PrescheduledWindow prescheduledWindow: PRESCHED_TIMES.getRooms().get(roomName)){
                BitSet preBs = BitSetHelper.timeJsonToBs(prescheduledWindow);
                room.prescheduleUpdate(preBs);
            }
        }

        return room;
    }

}
