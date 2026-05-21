package org.acme.schooltimetabling.builders.teachers.policies;

import java.util.BitSet;

public interface TeachingPolicy {
    void apply(BitSet preference, BitSet acceptable, BitSet conflict);
    boolean isFaculty();
}
