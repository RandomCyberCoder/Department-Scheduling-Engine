package org.acme.schooltimetabling.builders.lessons;

import org.acme.schooltimetabling.constants.Constants;
import org.acme.schooltimetabling.domain.lesson.Lesson;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.fileObjects.LabPatterns;


/**
 * <p>Use this class to build {@code Lesson} objects. Using the constructors is getting
 * kind of messy. The following must be set <i>id, course name, course configuration,
 * teacher object, and section</i>. The following are optional: modifier ("") and
 * labPattern (<i>null</i> if only lecture; {@link LabPatterns} if the configuration includes
 * a lab/act)</p>
 *
 * <p>Note about setting <i>id</i>. that the <i>id</i> is for the lesson object.
 * Don't mistake this with the section number of a courses lecture and/or lab. </p>
 *
 * <p>Note about setting the <i>section</i> number. If the course configuration for the lesson you
 * are building has either a lecture or a lab/act, then set the section number normally. If the
 * course configuration has both a lecture and a lab/act then just pass the lecture section number.
 * The lesson will assume the section number after the lecture is reserved for the lab/act</p>
 */
public class LessonBuilder {
    //required build fields
    private String id = null;
    private String courseName = null;
    private String courseConfig = null;
    private Teacher teacherObj = null;
    private Integer section = null;

    //optional build fields
    private String modifier = "";
    private LabPatterns labPattern = null;


    public LessonBuilder id(int id){
        if(id < 0) throw new IllegalArgumentException("the id must be greater then zero");
        this.id = String.valueOf(id);
        return this;
    }

    public LessonBuilder section(int lecSection) {
        this.section = lecSection;
        return this;
    }

    public LessonBuilder courseName(String courseName) {
        this.courseName = courseName;
        return this;
    }

    public LessonBuilder modifier(String modifier) {
        if(Constants.SPECIAL_CODE_CONVERSION.containsKey(modifier)) modifier = Constants.SPECIAL_CODE_CONVERSION.get(modifier);
        else if(!Constants.SPECIAL_CODE_CONVERSION.containsValue(modifier)) throw new IllegalArgumentException("Modifier is " +
                "not a valid option. Look in Constants.java for valid modifiers.");
        this.modifier = modifier;
        return this;
    }

    public LessonBuilder courseConfig(String courseConfig) {
        if(courseConfig == null || !validateConfig(courseConfig)) throw new IllegalArgumentException("A course config must be given in the format " +
                "d-d-d. Where d is an digit and one of them must be non-zero and digit order is lec-lab-act" );
        this.courseConfig = courseConfig;
        return this;
    }

    private boolean validateConfig(String config){
        return config.matches("^(?=.*[1-9])[0-9]-[0-9]-[0-9]$");
    }

    public LessonBuilder teacherObj(Teacher teacherObj) {
        this.teacherObj = teacherObj;
        return this;
    }

    public LessonBuilder labPattern(LabPatterns labPattern) {
        this.labPattern = labPattern;
        return this;
    }

    public LessonBuilder clear() {
        id = null;
        section = null;
        courseName = null;
        modifier = "";
        courseConfig = null;
        teacherObj = null;
        labPattern = null;
        return this;
    }

    public Lesson build() {
        if (id == null || section == null || courseName == null || courseConfig == null || teacherObj == null) {
            throw new IllegalCallerException(
                    "When calling build() you must have values set for id, section, courseName, courseConfig, and teacherObj"
            );
        }

        //check if we need a default for the lab
        String[] units = courseConfig.split("-");
        int labUnits = Integer.parseInt(units[1]);
        int actUnits = Integer.parseInt(units[2]);
        if(labPattern == null && (labUnits != 0 || actUnits != 0)) labPattern = LabPatterns.MULTIPLE;

        if(modifier == null) modifier = "";

        return new Lesson(id, section, courseName, modifier, courseConfig, teacherObj, labPattern);
    }
}
