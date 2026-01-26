package org.acme.schooltimetabling.apiCalls;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiConstants {
    public final static String baseUrl = "http://localhost:8000/api/";
    private final static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    public final static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(ApiConstants.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build();
}
