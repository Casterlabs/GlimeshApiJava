package co.casterlabs.glimeshapijava;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.auth.AuthProvider;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import okhttp3.Request.Builder;
import okhttp3.Response;

public class GlimeshAuth implements AuthProvider {
    private static final String AUTH_CODE_URL = "https://glimesh.tv/api/oauth/token?grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s";
    private static final String REFRESH_URL = "https://glimesh.tv/api/oauth/token?grant_type=refresh_token&refresh_token=%s&redirect_uri=%s&client_id=%s&client_secret=%s";

    private @Getter String refreshToken;
    private String redirectUri;
    private String clientId;
    private String secret;

    private @Getter String accessToken;
    private @Getter int expiresIn;

    public GlimeshAuth(String refreshToken, String redirectUri, String clientId, String secret) throws ApiAuthException {
        this.refreshToken = refreshToken;
        this.redirectUri = redirectUri;
        this.clientId = clientId;
        this.secret = secret;

        this.refresh();
    }

    public GlimeshAuth(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void authenticateRequest(@NonNull Builder request) {
        if (this.accessToken == null) {
            request.addHeader("Authorization", "Client-ID " + this.clientId);
        } else {
            request.addHeader("Authorization", "Bearer " + this.accessToken);
        }
    }

    @SneakyThrows
    public URI getRealtimeUrl() {
        if (this.accessToken == null) {
            return new URI("wss://glimesh.tv/api/socket/websocket?vsn=2.0.0&client_id=" + this.clientId);
        } else {
            return new URI("wss://glimesh.tv/api/socket/websocket?vsn=2.0.0&token=" + this.accessToken);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void refresh() throws ApiAuthException {
        try {
            String url = String.format(REFRESH_URL, this.refreshToken, URLEncoder.encode(this.redirectUri), this.clientId, this.secret);

            Response response = HttpUtil.sendHttp("{}", url);
            String body = response.body().string();

            response.close();

            JsonObject json = GlimeshApiJava.GSON.fromJson(body, JsonObject.class);

            if (json.has("error")) {
                throw new ApiAuthException(json.get("error").getAsString() + ": " + json.get("error_description").getAsString());
            } else if (json.has("errors")) {
                throw new ApiAuthException(json.toString());
            } else {
                AuthResponse data = GlimeshApiJava.GSON.fromJson(json, AuthResponse.class);

                this.refreshToken = data.refreshToken;
                this.accessToken = data.accessToken;
                this.expiresIn = data.expiresIn;
            }
        } catch (IOException e) {
            throw new ApiAuthException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public static AuthResponse authorize(String code, String redirectUri, String clientId, String secret) throws IOException, ApiAuthException {
        String url = String.format(AUTH_CODE_URL, code, URLEncoder.encode(redirectUri), clientId, secret);

        Response response = HttpUtil.sendHttp("{}", url);
        String body = response.body().string();

        response.close();

        JsonObject json = GlimeshApiJava.GSON.fromJson(body, JsonObject.class);

        if (json.has("error")) {
            throw new ApiAuthException(json.get("error").getAsString() + ": " + json.get("error_description").getAsString());
        } else if (json.has("errors")) {
            throw new ApiAuthException(json.toString());
        } else {
            return GlimeshApiJava.GSON.fromJson(json, AuthResponse.class);
        }
    }

    @Getter
    @ToString
    public static class AuthResponse {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("expires_in")
        private int expiresIn;

        @SerializedName("refresh_token")
        private String refreshToken;

        private String scope;

        @SerializedName("token_type")
        private String tokenType;

    }

}
