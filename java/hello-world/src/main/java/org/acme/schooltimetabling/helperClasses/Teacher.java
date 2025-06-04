package org.acme.schooltimetabling.helperClasses;

import java.util.List;
import java.util.BitSet;

public class Teacher {
    public int id;
    /*we should store canon name here*/
    public String name;

    /*timeslot preferences*/
    public BitSet preferences;

    /*acceptable timeslots*/
    public BitSet acceptable;

    /*impossible timeslots*/
    public BitSet conflict;

    public Teacher(int id, String name, BitSet preferences, BitSet acceptable, BitSet conflict) {
        this.id = id;
        this.name = name;
        this.preferences = preferences;
        this.acceptable = acceptable;
        this.conflict = conflict;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BitSet getPreferences() {
        return preferences;
    }

    public BitSet getAcceptable() {
        return acceptable;
    }

    public BitSet getConflict() {
        return conflict;
    }
}
