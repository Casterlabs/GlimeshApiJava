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
import co.casterlabs.glimeshapijava.types.GlimeshSubscriber;
import lombok.NonNull;
import okhttp3.Response;

public class GlimeshGetUserSubscribersRequest extends AuthenticatedWebRequest<List<GlimeshSubscriber>, GlimeshAuth> {
    private static final String QUERY_BASE = "query{subscriptions(%s){" + GlimeshSubscriber.GQL_DATA + "}}";
    private static final Type TYPE = new TypeToken<List<GlimeshSubscriber>>() {
    }.getType();

    private String username;
    private int id;

    public GlimeshGetUserSubscribersRequest(@NonNull GlimeshAuth auth, @NonNull String username) {
        super(auth);

        this.username = username;
    }

    public GlimeshGetUserSubscribersRequest(@NonNull GlimeshAuth auth, int id) {
        super(auth);

        this.id = id;
    }

    @Override
    protected List<GlimeshSubscriber> execute() throws ApiException, ApiAuthException, IOException {
        String arguments = (this.username == null) ? String.format("streamerId: %d", this.id) : String.format("streamerUsername: \"%s\"", this.username);
        String query = String.format(QUERY_BASE, arguments);

        try (Response response = HttpUtil.sendHttp(query, this.auth)) {
            String body = response.body().string();

            JsonObject json = GlimeshApiJava.GSON.fromJson(body, JsonObject.class);

            if (response.code() == 401) {
                throw new ApiAuthException(json.toString());
            } else if (json.has("errors")) {
                throw new ApiException(json.toString());
            } else {
                return GlimeshApiJava.GSON.fromJson(json.getAsJsonObject("data").get("subscriptions"), TYPE);
            }
        }
    }

}
