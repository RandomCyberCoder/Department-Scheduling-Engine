package org.acme.schooltimetabling.domain.teacher;

import java.util.BitSet;

public class Teacher {
    public int id;
    /*canon name*/
    public String name;
    /*acceptable timeslots*/
    public BitSet acceptable;
    /*timeslot preferences*/
    public BitSet preferences;
    /*impossible timeslots*/
    public BitSet conflict;

    public Teacher(int id, String name, BitSet preferences, BitSet acceptable, BitSet conflict) {
        this.id = id;
        this.name = name;
        this.preferences = preferences;
        this.acceptable = acceptable;
        this.conflict = conflict;
    }

    protected Teacher(Teacher copyMe){
        this.id = copyMe.id;
        this.name = copyMe.name;
        this.preferences = (BitSet) copyMe.preferences.clone();
        this.acceptable = (BitSet) copyMe.acceptable.clone();
        this.conflict = (BitSet) copyMe.conflict.clone();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BitSet getAcceptable() {
        return acceptable;
    }

    public BitSet getPreferences() {
        return preferences;
    }

    public BitSet getConflict() {
        return conflict;
    }
}
