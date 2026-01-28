package org.acme.schooltimetabling.apiCalls.teacherEndpoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class TeacherRecord {
    @SerializedName("id")
    @JsonProperty("id")
    private int pk;
    @SerializedName("non_canon")
    @JsonProperty("non_canon")
    private String nonCanon;
    @SerializedName("canon")
    @JsonProperty("canon")
    private String canon;
    @SerializedName("email")
    @JsonProperty("email")
    private String email;
    @SerializedName("faculty")
    @JsonProperty("faculty")
    private boolean isFaculty;
    @SerializedName("csc")
    @JsonProperty("csc")
    private boolean inCSC;
    @SerializedName("cpe")
    @JsonProperty("cpe")
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
