package org.acme.schooltimetabling.builders.teachers;

import org.acme.schooltimetabling.builders.teachers.policies.TeachingPolicy;
import org.acme.schooltimetabling.constants.Preference;
import org.acme.schooltimetabling.domain.teacher.Faculty;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import org.acme.schooltimetabling.helperClasses.Generators.TeacherGenerator;
import org.jspecify.annotations.NonNull;

import java.util.BitSet;

public class TeacherBuilder {
    //we don't let them set the id; we will set it for them; not information they need to know
    private BitSet preference = null;
    private BitSet acceptable = null;
    private BitSet conflict = null;
    private String canon = null;
    private Preference gapPref = null;
    private TeachingPolicy policy;

    public TeacherBuilder(TeachingPolicy teachingPolicy){
        policy = teachingPolicy;
    }

    public TeacherBuilder setPolicy(@NonNull TeachingPolicy teachingPolicy){
        policy = teachingPolicy;
        return this;
    }

    public TeacherBuilder preference(@NonNull BitSet preference){
        this.preference = (BitSet) preference.clone();
        return this;
    }

    public TeacherBuilder acceptable(@NonNull BitSet acceptable){
        this.acceptable = (BitSet) acceptable.clone();
        return this;
    }

    public TeacherBuilder conflict(@NonNull BitSet conflict){
        this.conflict = (BitSet) conflict.clone();
        return this;
    }

    public TeacherBuilder canon(@NonNull String canon){
        this.canon = canon;
        return this;
    }

    public TeacherBuilder gapPref(@NonNull Preference gapPref){
        this.gapPref = gapPref;
        return this;
    }

    /**
     * Note you must first set the <i>preference</i>, <i>acceptable</i>, and <i>conflict</i> <i>BitSets</i> must
     * be set before calling this method.
     * @param preschedule <i>BitSet</i> mask
     */
    public TeacherBuilder preschedule(@NonNull BitSet preschedule){
        this.preference.andNot(preschedule);
        this.acceptable.andNot(preschedule);
        this.conflict.or(preschedule);
        return this;
    }

    /**
     * clears the private fields containing the data passed to the {@link Faculty} or {@link Teacher} constructors
     * by setting them to <i>null</i>.
     */
    public TeacherBuilder clear(){
        preference = null;
        acceptable = null;
        conflict = null;
        canon = null;
        gapPref = null;

        return this;
    }

    public Teacher build(){
        //validation
        if(preference == null || acceptable == null || conflict == null || canon == null || gapPref == null){
            throw new IllegalCallerException("When calling build() you must have a value set for preference, acceptable, " +
                    "conflict, canon name, and the gap preference");
        }

        BitSet preference = (BitSet) this.preference.clone();
        BitSet acceptable = (BitSet) this.acceptable.clone();
        BitSet conflict = (BitSet) this.conflict.clone();

        //apply the policy for the instructor
        policy.apply(preference, acceptable, conflict);

        if(policy.isFaculty()){
            return new Faculty(TeacherGenerator.getNextTeacherID(),
                    canon,
                    preference,
                    acceptable,
                    conflict,
                    gapPref);
        }
        else{
            return new Teacher(TeacherGenerator.getNextTeacherID(),
                    canon,
                    preference,
                    acceptable,
                    conflict,
                    gapPref);
        }
    }
}
