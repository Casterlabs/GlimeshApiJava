package co.casterlabs.glimeshapijava.requests;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.apiutil.web.AuthenticatedWebRequest;
import co.casterlabs.glimeshapijava.GlimeshApiJava;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.HttpUtil;
import co.casterlabs.glimeshapijava.types.GlimeshFollower;
import lombok.NonNull;
import okhttp3.Response;

public class GlimeshGetUserFollowersRequest extends AuthenticatedWebRequest<List<GlimeshFollower>, GlimeshAuth> {
    private static final String QUERY_BASE = "query{followers(%s){" + GlimeshFollower.GQL_DATA + "}}";
    private static final Type TYPE = new TypeToken<List<GlimeshFollower>>() {
    }.getType();

    private String username;
    private int id;

    public GlimeshGetUserFollowersRequest(@NonNull GlimeshAuth auth, @NonNull String username) {
        super(auth);

        this.username = username;
    }

    public GlimeshGetUserFollowersRequest(@NonNull GlimeshAuth auth, int id) {
        super(auth);

        this.id = id;
    }

    @Override
    protected List<GlimeshFollower> execute() throws ApiException, ApiAuthException, IOException {
        String arguments = (this.username == null) ? String.format("streamerId: %d", this.id) : String.format("streamerUsername: \"%s\"", this.username);
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
            return GlimeshApiJava.GSON.fromJson(json.getAsJsonObject("data").get("followers"), TYPE);
        }
    }

}
