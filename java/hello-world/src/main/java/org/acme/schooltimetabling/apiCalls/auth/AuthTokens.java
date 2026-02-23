package org.acme.schooltimetabling.apiCalls.auth;

import io.github.cdimascio.dotenv.Dotenv;
import org.acme.schooltimetabling.apiCalls.ApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.Map;

/**
 * Singleton Auth class
 */
public class AuthTokens {
    private static Logger LOGGER = LoggerFactory.getLogger(AuthTokens.class);
    private static AuthTokens INSTANCE = null;
    private String accessToken;
    private String refreshToken;

    private AuthTokens() {
    }

    private static AuthTokens load() {
        AuthTokens ret = new AuthTokens();
        LOGGER.info("Getting Auth credentials....");
        AuthService service = ApiConstants.retrofit.create(AuthService.class);
        try {
            Dotenv dotenv = Dotenv.configure().load();
            String userPassword = dotenv.get("API_PASSWORD");
            String userName = dotenv.get("API_USER");
            if (userName == null || userPassword == null) {
                LOGGER.error("Failed to find a user name and/or password for DB authentication");
                return ret;
            }

            Map<String, String> reqBody = Map.of("username", userName, "password", userPassword);
            Call<Map<String, Object>> callSync = service.getTokenPair(reqBody);
            Response<Map<String, Object>> response = callSync.execute();
            if (!response.isSuccessful() && response.errorBody() != null) {
                LOGGER.error(String.format("Got an error when trying to read from the db; Error: %s; Additional info: %s",
                        response.errorBody().string(), response.raw().message()));
                return ret;
            } else {
                ret.accessToken = (String) response.body().get("access");
                ret.refreshToken = (String) response.body().get("refresh");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("failed");
        }

        return ret;
    }

    /**
     * @return the current access token
     */
    public static String getAccessToken() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE.accessToken;
    }


    interface AuthService {
        @POST("token/")
        public Call<Map<String, Object>> getTokenPair(@Body Map<String, String> body);

//        @POST("token/refresh/")
//        public Call<Map<String, Object>> getNewToken(@Body Map<String, String> body);
    }
}
