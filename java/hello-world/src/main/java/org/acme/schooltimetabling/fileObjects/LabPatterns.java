package org.acme.schooltimetabling.fileObjects;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum LabPatterns {
    ONE, MULTIPLE;

    @JsonCreator
    public static LabPatterns parseString(String value){
        return LabPatterns.valueOf(value.toUpperCase());
    }
}
