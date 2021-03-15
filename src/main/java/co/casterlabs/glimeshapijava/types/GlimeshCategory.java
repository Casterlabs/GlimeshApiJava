package co.casterlabs.glimeshapijava.types;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GlimeshCategory {
    private String name;
    private GlimeshCategory parent;
    private String slug;
    private String tagName;

}
