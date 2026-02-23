package org.acme.schooltimetabling.apiCalls.teacherEndpoint;

import com.google.common.collect.ImmutableMap;
import org.acme.schooltimetabling.apiCalls.ApiConstants;
import org.acme.schooltimetabling.apiCalls.surveyEndpoint.SurveyCalls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TeacherCalls {
    final static Logger LOGGER = LoggerFactory.getLogger(SurveyCalls.class);
    public static void main(String[] args) throws Exception{
        getAllTeachers();
    }

    public static List<TeacherRecord> getAllTeachers() throws Exception{
        TeacherService service = ApiConstants.retrofit.create(TeacherService.class);
        Call<List<TeacherRecord>> callSync = service.getTeachers(ImmutableMap.of(
//                example of query param
//                "department", "csc"
        ));

        Response<List<TeacherRecord>> response = callSync.execute();
        if(!response.isSuccessful() && response.errorBody() != null){
            LOGGER.error(String.format("Got an error when trying to read from the db; Error: %s; Additional info: %s",
                    response.errorBody().string(), response.raw().message()));
        }
        return response.body() != null ? response.body() : Collections.emptyList();
    }

}

/**
 * Only useful for when you call for a teacher by ID. Specifically
 * {@link #}
 */
class TeacherResponse {
    private String success;
    private TeacherRecord data;

    public TeacherRecord getData() {
        return data;
    }
}


interface TeacherService {
    @GET("teachers/")
    public Call<List<TeacherRecord>> getTeachers(
            @QueryMap Map<String, String> filters
    );

    @GET("teachers/{pk}/")
    public Call<TeacherResponse> getTeacher(@Path("pk") int pk);


}