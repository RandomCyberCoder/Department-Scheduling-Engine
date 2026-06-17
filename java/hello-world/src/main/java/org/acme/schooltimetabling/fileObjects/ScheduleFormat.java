package org.acme.schooltimetabling.fileObjects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleFormat {
    private List<String> fall;
    private String name;
    private List<String> spring;
    private List<String> winter;

    public ScheduleFormat() {

    }

    /**
     * Get list of courses the teacher is scheduled for the fall
     * @return course list for teacher in this term
     */
    public List<String> getFall() {
        return fall;
    }

    /**
     * Name of the teacher. Assume this is the canon name
     * @return teacher name
     */
    public String getName() {
        return name;
    }

    /**
     * Get list of courses the teacher is scheduled for the spring
     * @return course list for teacher in this term
     */
    public List<String> getSpring() {
        return spring;
    }

    /**
     * Get list of courses the teacher is scheduled for the winter
     * @return course list for teacher in this term
     */
    public List<String> getWinter() {
        return winter;
    }

    @Override
    public String toString() {
        return "scheduleFormat{" +
                "fall=" + fall +
                ", name='" + name + '\'' +
                ", spring=" + spring +
                ", winter=" + winter +
                '}';
    }
}
