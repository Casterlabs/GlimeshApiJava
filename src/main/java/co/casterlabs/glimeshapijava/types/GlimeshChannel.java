package co.casterlabs.glimeshapijava.types;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GlimeshChannel {
    public static final String GQL_DATA = "chatRulesHtml,chatRulesMd,id,inaccessible,language,status,stream{" + GlimeshStream.GQL_DATA + "},streamer{" + GlimeshUser.GQL_DATA + "},thumbnail,title";

    private GlimeshCategory category;
    // TODO private ChatMessage[] chatMessages;
    private String chatRulesHtml;
    private String chatRulesMd;
    private int id;
    private boolean inaccessible;
    private String language;
    private ChannelStatus status;
    private GlimeshStream stream;
    private GlimeshUser streamer;
    private String thumbnail;
    private String title;

    public static enum ChannelStatus {
        LIVE,
        OFFLINE
    }

}
