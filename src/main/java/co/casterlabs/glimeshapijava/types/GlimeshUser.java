package co.casterlabs.glimeshapijava.types;

import java.util.List;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GlimeshUser {
    public static final String GQL_DATA = "avatar,avatarUrl,confirmedAt,displayname,id,username,profileContentHtml,profileContentMd,socials{id,identifier,insertedAt,platform,updatedAt,username}";

    private String avatar;
    private String avatarUrl;
    // private Instant confirmedAt;
    private String displayname;
    private String username;
    private int id;
    private String profileContentHtml;
    private String profileContentMd;
    private List<GlimeshSocial> socials;

    /**
     * @deprecated Use {@link #getAvatarUrl()}
     */
    @Deprecated
    public String getAvatar() {
        return this.avatar;
    }

}
