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

public class GlimeshGetMyselfRequest extends AuthenticatedWebRequest<GlimeshUser, GlimeshAuth> {
    private static final String QUERY = "query{myself{" + GlimeshUser.GQL_DATA + "}}";

    public GlimeshGetMyselfRequest(@NonNull GlimeshAuth auth) {
        super(auth);
    }

    @Override
    protected GlimeshUser execute() throws ApiException, ApiAuthException, IOException {
        Response response = HttpUtil.sendHttp(QUERY, this.auth);
        String body = response.body().string();

        response.close();

        JsonObject json = GlimeshApiJava.GSON.fromJson(body, JsonObject.class);

        if (response.code() == 401) {
            throw new ApiAuthException(json.toString());
        } else if (json.has("errors")) {
            throw new ApiAuthException(json.toString());
        } else {
            return GlimeshApiJava.GSON.fromJson(json.getAsJsonObject("data").get("myself"), GlimeshUser.class);
        }
    }

}
