package org.acme.schooltimetabling.apiCalls;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.acme.schooltimetabling.apiCalls.auth.AuthTokens;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiConstants {
    public final static String baseUrl = "http://localhost:8002/api/";
    public final static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(ApiConstants.baseUrl)
            .client(new OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static Retrofit retrofitWAuth = null;

    /**
     * @return Retrofit with an authorized client; null if unable to produce an authorized client
     */
    public static Retrofit getRetrofitWithAuth(){
        if(retrofitWAuth != null) return retrofitWAuth;

        String accessToken = AuthTokens.getAccessToken();
        if(accessToken == null) return null;
        OkHttpClient authorizedClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();

        retrofitWAuth =  new Retrofit.Builder()
                .baseUrl(ApiConstants.baseUrl)
                .client(authorizedClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofitWAuth;
    }
}
