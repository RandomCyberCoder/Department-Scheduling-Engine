package org.acme.schooltimetabling.apiCalls.teacherEndpoint;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import okhttp3.OkHttpClient;
import org.acme.schooltimetabling.apiCalls.ApiConstants;
import org.acme.schooltimetabling.domain.teacher.Teacher;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TeacherCalls {
    //but thsi into the
    public static void main(String[] args) throws Exception{
        getAllTeachers();
    }

    public static List<Teacher> getAllTeachers() throws Exception{
        TeacherService service = ApiConstants.retrofit.create(TeacherService.class);
        Call<List<TeacherRecord>> callSync = service.getTeachers(ImmutableMap.of(
//                example of query param
//                "department", "csc"
        ));

        Response<List<TeacherRecord>> response = callSync.execute();
        List<TeacherRecord> records = response.body() != null ? response.body() : Collections.emptyList();

        List<Teacher> teachers = new ArrayList<>();
        for(TeacherRecord record: records){
            //teacher hash map -> pk

        }
//        ImmutableMap.of
        return teachers;

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