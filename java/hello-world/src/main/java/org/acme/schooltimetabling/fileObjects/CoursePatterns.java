package org.acme.schooltimetabling.fileObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CoursePatterns {
    private Map<String, LabPatterns> defaultPattern = new LinkedHashMap<>();
    private Map<String, Map<String, LabPatterns>> teacherPreference = new LinkedHashMap<>();


    public CoursePatterns() {
    }

    /**
     * retrieves the pattern for a lab of the given course
     * @param course name of the course whose lab pattern we want
     * @return {@code null} if no course for the pattern was found or
     */
    public LabPatterns getCourseDefault(String course) {
        if(course == null) return null;
        return this.defaultPattern.get(course);
    }

    @JsonProperty("defaultPattern")
    public void setDefaultPattern(Map<String, LabPatterns> defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    /**
     * Returns the preference of a course for an instructor if one can be found.
     *
     * @param course course whose preference we want
     * @param name name of teacher whose preference we want
     * @return preference of the <i>course</i> for the given <i>instructor</i>. If no preference was found,
     * {@code null} will be returned.
     */
    public LabPatterns getTeacherPref(String course, String name) {
        Map<String, LabPatterns> teacherMap;

        if(course == null || name == null) return null;

        //return null if no instructor preferences found or if no preference for the course
        //is given; Otherwise, return the preference for the course
        teacherMap = this.teacherPreference.get(name);
        if(teacherMap == null) return null;
        return teacherMap.get(course);
    }

    @JsonProperty("teacherPreference")
    public void setTeacherPreference(Map<String, Map<String, LabPatterns>> teacherPreference) {
        this.teacherPreference = teacherPreference;
    }

    static public CoursePatterns empty(){
        return new CoursePatterns();
    }
}
