package co.casterlabs.glimeshapijava.types;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GlimeshFollower {
    private boolean hasLiveNotifications;
    private GlimeshUser streamer;
    private GlimeshUser user;
    private String id;

}
