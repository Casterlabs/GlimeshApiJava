package co.casterlabs.glimeshapijava;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class GlimeshApiJava {
    public static final String GLIMESH_API = "https://glimesh.tv/api";
    // @formatter:off
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    // @formatter:on

    private static final JsonObject BLANK = new JsonObject();

    public static String formatQuery(String query) {
        JsonObject payload = new JsonObject();

        payload.addProperty("query", query);
        payload.add("variables", BLANK);

        return payload.toString();
    }

}
