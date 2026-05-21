package org.acme.schooltimetabling.apiCalls.surveyEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.acme.schooltimetabling.apiCalls.ApiConstants;
import org.acme.schooltimetabling.fileObjects.ScheduleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SurveyCalls {
    final static Logger LOGGER = LoggerFactory.getLogger(SurveyCalls.class);
//    public static void main(String[] args) throws Exception{
//        termSurveysToRecord();
//    }

    public static List<SurveyRecord> termSurveysToRecord() throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        if(ApiConstants.getRetrofitWithAuth() == null){
            LOGGER.error("Couldn't get authentication tokens");
            return Collections.emptyList();
        }
        SurveyService service = ApiConstants.getRetrofitWithAuth().create(SurveyService.class);
        Call<List<Map<String, Object>>> callSync = service.getSurveys(ImmutableMap.of(
                "term", ScheduleConfig.getCurTerm()
        ));
        Response<List<Map<String, Object>>> response = callSync.execute();
        if(!response.isSuccessful() && response.errorBody() != null){
            LOGGER.error(String.format("Got an error when trying to read from the db; Error: %s; Additional info: %s",
                    response.errorBody().string(), response.raw().message()));
        }
        List<Map<String, Object>> surveyMaps = response.body() != null ? response.body() : Collections.emptyList();

        List<SurveyRecord> surveyRecords = new ArrayList<>();
        for(Map<String, Object> record: surveyMaps){
            //convert to teacher pref
            SurveyRecord surveyRecord = mapper.convertValue(record, SurveyRecord.class);
            surveyRecords.add(surveyRecord);
        }

        return surveyRecords;
    }


}

interface SurveyService{
    @GET("surveys/")
    public Call<List<Map<String, Object>>> getSurveys(
            @QueryMap Map<String, String> filters
    );
}