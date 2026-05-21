package org.acme.schooltimetabling.defaultTimes;

import java.util.BitSet;

/**
 * All classes that inherit this class will be a part of a prototype pattern;
 * Each class should be implemented as a singleton
 * When implementing this class, the classes should make sure that the preference, acceptable, and conflict
 * {@link BitSet}s cover the whole time span of
 * 7AM-10PM for MTWRF
 */
public interface DefaultTime {
    //these should all return copies of the bitsets for safety
    BitSet getPreference();
    BitSet getAcceptable();
    BitSet getConflict();
}
