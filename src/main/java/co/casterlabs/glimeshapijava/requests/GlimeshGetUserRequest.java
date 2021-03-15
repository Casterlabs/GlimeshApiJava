package co.casterlabs.glimeshapijava.requests;

import java.io.IOException;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.apiutil.web.AuthenticatedWebRequest;
import co.casterlabs.glimeshapijava.GlimeshApiJava;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.HttpUtil;
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import lombok.NonNull;
import okhttp3.Response;

public class GlimeshGetUserRequest extends AuthenticatedWebRequest<GlimeshUser, GlimeshAuth> {
    private static final String QUERY_BASE = "query{user(%s){" + GlimeshUser.GQL_DATA + "}}";

    private String username;
    private int id;

    public GlimeshGetUserRequest(@NonNull GlimeshAuth auth, @NonNull String username) {
        super(auth);

        this.username = username;
    }

    public GlimeshGetUserRequest(@NonNull GlimeshAuth auth, int id) {
        super(auth);

        this.id = id;
    }

    @Override
    protected GlimeshUser execute() throws ApiException, ApiAuthException, IOException {
        String arguments = (this.username == null) ? String.format("id: %d", this.id) : String.format("username: \"%s\"", this.username);
        String query = String.format(QUERY_BASE, arguments);

        Response response = HttpUtil.sendHttp(query, this.auth);
        String body = response.body().string();

        response.close();

        JsonObject json = GlimeshApiJava.GSON.fromJson(body, JsonObject.class);

        if (response.code() == 401) {
            throw new ApiAuthException(json.toString());
        } else if (json.has("errors")) {
            throw new ApiException(json.toString());
        } else {
            return GlimeshApiJava.GSON.fromJson(json.getAsJsonObject("data").get("user"), GlimeshUser.class);
        }
    }

}
