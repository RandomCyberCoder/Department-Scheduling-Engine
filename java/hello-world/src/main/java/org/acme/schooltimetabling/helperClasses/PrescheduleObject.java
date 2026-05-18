package org.acme.schooltimetabling.helperClasses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrescheduleObject {

    private Map<String, List<PrescheduledWindow>> teachers = new LinkedHashMap<>();
    private Map<String, List<PrescheduledWindow>> rooms = new LinkedHashMap<>();

    public PrescheduleObject() {
    }

    @JsonProperty("teachers")
    public Map<String, List<PrescheduledWindow>> getTeachers() {
        return teachers;
    }

    public void setTeachers(Map<String, List<PrescheduledWindow>> teachers) {
        this.teachers = teachers;
    }

    @JsonProperty("rooms")
    public Map<String, List<PrescheduledWindow>> getRooms() {
        return rooms;
    }

    public void setRooms(Map<String, List<PrescheduledWindow>> rooms) {
        this.rooms = rooms;
    }

    public boolean hasTeachers() {
        return teachers != null;
    }

    public boolean hasRooms() {
        return rooms != null;
    }

    /**
     * Checks to make sure that the JSON object has all the fields that are necessary
     */
    public void validate() {
        if (!hasTeachers()) {
            throw new IllegalStateException("PrescheduleObject must contain at least one teacher entry.");
        }
        if (!hasRooms()) {
            throw new IllegalStateException("PrescheduleObject must contain at least one room entry.");
        }

        validateWindows("teacher", teachers);
        validateWindows("room", rooms);
    }

    private void validateWindows(String category, Map<String, List<PrescheduledWindow>> entries) {
        for (Map.Entry<String, List<PrescheduledWindow>> entry : entries.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                throw new IllegalStateException("PrescheduleObject contains no windows for " + category + " '"
                        + entry.getKey() + "'.");
            }
            for (PrescheduledWindow window : entry.getValue()) {
                if (window == null) {
                    throw new IllegalStateException("PrescheduleObject contains a null window for " + category + " '"
                            + entry.getKey() + "'.");
                }
                window.validate(category, entry.getKey());
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrescheduledWindow {
        @JsonProperty("start")
        private String start;
        @JsonProperty("day")
        private String day;
        @JsonProperty("end")
        private String end;

        public PrescheduledWindow() {
        }

        public PrescheduledWindow(String start, String day, String end) {
            this.start = start;
            this.day = day;
            this.end = end;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public String getDay() {
            return day;
        }

        public String getEnd() {
            return end;
        }

        public void validate(String category, String ownerName) {
            if (start == null || start.isBlank()) {
                throw new IllegalStateException("Missing start for " + category + " '" + ownerName + "'.");
            }
            if (day == null || day.isBlank()) {
                throw new IllegalStateException("Missing day for " + category + " '" + ownerName + "'.");
            }
            if (end == null || end.isBlank()) {
                throw new IllegalStateException("Missing end for " + category + " '" + ownerName + "'.");
            }
        }
    }

    public static PrescheduleObject empty() {
        return new PrescheduleObject();
    }

    /**
     * Add a new Prescheduled Window for a teacher
     */
    public void addTeacherWindow(String teacherName, PrescheduledWindow window) {
        teachers.computeIfAbsent(teacherName, ignored -> new ArrayList<>()).add(window);
    }

    /**
     * Add a new Prescheduled Window for a room
     */
    public void addRoomWindow(String roomName, PrescheduledWindow window) {
        rooms.computeIfAbsent(roomName, ignored -> new ArrayList<>()).add(window);
    }
}
