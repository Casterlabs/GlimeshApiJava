package co.casterlabs.glimeshapijava.types;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GlimeshSubscriber {
    public static final String GQL_DATA = "isActive,price,productName,streamer{" + GlimeshUser.GQL_DATA + "},user{" + GlimeshUser.GQL_DATA + "},id";
    private boolean isActive;
    private int price;
    private String productName;
    private GlimeshUser streamer;
    private GlimeshUser user;
    private String id;

}
