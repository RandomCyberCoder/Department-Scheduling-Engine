package org.acme.schooltimetabling.helperClasses;
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

    public List<String> getFall() {
        return fall;
    }

    public String getName() {
        return name;
    }

    public List<String> getSpring() {
        return spring;
    }

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
