package co.casterlabs.glimeshapijava.types;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GlimeshChatMessage {
    public static final String GQL_DATA = "id,message,user{" + GlimeshUser.GQL_DATA + "},channel{" + GlimeshChannel.GQL_DATA + "}";

    private GlimeshChannel channel;
    private String id;
    private String message;
    private GlimeshUser user;

}
