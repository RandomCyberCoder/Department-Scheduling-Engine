package org.acme.schooltimetabling.apiCalls.teacherEndpoint;

import com.google.gson.annotations.SerializedName;

public class TeacherRecord {
    @SerializedName("id")
    private int pk;
    @SerializedName("non_canon")
    private String nonCanon;
    @SerializedName("canon")
    private String canon;
    @SerializedName("email")
    private String email;
    @SerializedName("faculty")
    private boolean isFaculty;
    @SerializedName("csc")
    private boolean inCSC;
    @SerializedName("cpe")
    private boolean inCPE;

    public int getPk() {
        return pk;
    }

    public String getNonCanon() {
        return nonCanon;
    }

    public String getCanon() {
        return canon;
    }

    public String getEmail() {
        return email;
    }

    public boolean isFaculty() {
        return isFaculty;
    }

    public boolean isInCSC() {
        return inCSC;
    }

    public boolean isInCPE() {
        return inCPE;
    }

}
