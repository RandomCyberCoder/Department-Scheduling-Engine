package org.acme.schooltimetabling.helperClasses;

import java.util.List;
import java.util.BitSet;

public class Teacher {
    /*we should store canon name here*/
    public String name;

    /*timeslot preferences*/
    public BitSet preferences;

    /*acceptable timeslots*/
    public BitSet acceptable;

    /*impossible timeslots*/
    public BitSet conflict;


}
