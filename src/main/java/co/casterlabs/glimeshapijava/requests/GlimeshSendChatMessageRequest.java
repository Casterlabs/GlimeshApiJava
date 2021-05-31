package co.casterlabs.glimeshapijava.requests;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.apiutil.web.AuthenticatedWebRequest;
import co.casterlabs.glimeshapijava.GlimeshApiJava;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.HttpUtil;
import lombok.NonNull;
import okhttp3.Response;

public class GlimeshSendChatMessageRequest extends AuthenticatedWebRequest<Void, GlimeshAuth> {
    private static final String MUTATION = "mutation{createChatMessage(channelId:%d,message:{message:%s}){insertedAt}}";

    private String message;
    private int channelId;

    public GlimeshSendChatMessageRequest(@NonNull GlimeshAuth auth, @NonNull String message, int channelId) {
        super(auth);

        this.message = message;
        this.channelId = channelId;
    }

    @Override
    protected Void execute() throws ApiException, ApiAuthException, IOException {
        String query = String.format(MUTATION, this.channelId, new JsonPrimitive(this.message).toString());

        try (Response response = HttpUtil.sendHttp(query, this.auth)) {
            String body = response.body().string();

            JsonObject json = GlimeshApiJava.GSON.fromJson(body, JsonObject.class);

            if (response.code() == 401) {
                throw new ApiAuthException(json.toString());
            } else if (json.has("errors")) {
                throw new ApiException(json.toString());
            } else {
                return null;
            }
        }
    }

}
