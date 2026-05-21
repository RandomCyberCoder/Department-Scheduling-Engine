package org.acme.schooltimetabling.builders.teachers.policies;

import java.util.BitSet;

public class DefaultTeachingPolicy implements TeachingPolicy {
    public DefaultTeachingPolicy(){}

    @Override
    public boolean isFaculty() {
        return false;
    }

    @Override
    public void apply(BitSet preference, BitSet acceptable, BitSet conflict) {
        //no updates when using the default policy
    }
}
