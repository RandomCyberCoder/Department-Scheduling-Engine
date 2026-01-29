package org.acme.schooltimetabling.helperClasses.Generators;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Generator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    /**
     * <p>This method will take an iterator of all possible courses and will
     * return a bidirectional map of courses in the current department we want
     * to schedule mapped to an ID created for them (an integer)</p>
     *
     * @param courses an iterator for all courses
     * @return A bidirectional map of a course to its ID
     */
    public static BiMap<String, Integer> genCourseToIdMapping(Iterator<String> courses){
        final String DEPARTMENT = ScheduleConfig.getDepartment().toLowerCase();
        BiMap<String, Integer> courseIdMapping = HashBiMap.create();
        int count = 1;
        String course;

        while(courses.hasNext()){
            course = courses.next();
            if(!course.contains(DEPARTMENT)){
                continue;
            }
            courseIdMapping.put(course, count++);
        }

        return courseIdMapping;
    }

}
