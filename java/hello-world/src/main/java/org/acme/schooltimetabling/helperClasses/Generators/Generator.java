package org.acme.schooltimetabling.helperClasses.Generators;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.helperClasses.ParseInput;
import org.acme.schooltimetabling.helperClasses.ScheduleFormat;
import org.acme.schooltimetabling.helperClasses.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Generator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    /**
     * <p>The function creates a mapping from the names in the teacher HashMap to the respective
     * name in the schedules List. We need this so we can link the names in the teachers survey file
     * to the names in the schedule-DEPARTMENT-TERM file because they names are not the same. The name
     * in the latter file will be the cannon name which all names for the teacher must map to.</p>
     *
     * @param teacherHashMap Hashmap of the teacher's name to their Teacher object
     * @param schedules List of ScheduleFormat objects that contain what courses each teacher will teach
     */
    public static void createTeacherNameMapping(
            HashMap<String, Teacher> teacherHashMap, List<ScheduleFormat> schedules){
        String teacherName_schedule;
        String[] nameSplit;
        String firstName;
        String lastName;
        HashMap<String, String> teacherNameToCanon = Constants.TEACHER_NAME_TO_CANON;
        boolean mappingFound;

        for(String teacherName: teacherHashMap.keySet()){
            mappingFound = false;
            /*I'm assuming here that the names in the survey file are in the format
            * "<First> <Last>" where First is a char sequences with no spaces and
            * Last a char sequence the can have spaces but only the first portion of
            * it is considered. If it violates this I assume it's some generic schedule and skip it*/
            nameSplit = teacherName.split(" ");
            if(Constants.DEBUG && nameSplit.length < 2){
                LOGGER.warn((String.format("In createTeacherNameMapping skipping teacher key in HashMap " +
                        "that has value %s", teacherName)));
                continue;
            }
            firstName = nameSplit[0];
            lastName = nameSplit[1];
            /*If the name is already in the name remapping then it means that they
            * are a special case, and already exists in the mapping, so we can skip*/
            if(teacherNameToCanon.get(teacherName) != null){
                continue;
            }
            for(ScheduleFormat schedule: schedules){
                teacherName_schedule = schedule.getName();
                if(teacherName_schedule.contains(firstName) && teacherName_schedule.contains(lastName)){
                    mappingFound = true;
                    teacherNameToCanon.put(teacherName, teacherName_schedule);
                    break;
                }
            }
            if(Constants.DEBUG && !mappingFound){
                LOGGER.warn("Couldn't find a mapping for %s\n", teacherName);
            }
        }
    }

    /**
     * <p>This method will take an iterator of all possible courses and will
     * return a bidirectional map of courses in the current department we want
     * to schedule mapped to an ID created for them (an integer)</p>
     *
     * @param courses an iterator for all courses
     * @return A bidirectional map of a course to its ID
     */
    public static BiMap<String, Integer> genCourseToIdMapping(Iterator<String> courses){
        final String DEPARTMENT = ParseInput.scheduleConfig.department.toLowerCase();
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
