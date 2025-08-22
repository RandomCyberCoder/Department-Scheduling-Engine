package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

public class Room {

    @PlanningId
    public String id;

    private String name;
    /*I think this might be the only thing we really need in terms
    * of what is needed for scheduling. Anything else is for debugging
    * but even that could be found in the hashmaps that have been created*/
    private int ID;
    public Room() {
    }

    public Room(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Room(String id, String name, int ID){
        this.id = id;
        this.name = name;
        this.ID = ID;
    }

    @Override
    public String toString() {
        return name;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getID() {
        return ID;
    }
}
