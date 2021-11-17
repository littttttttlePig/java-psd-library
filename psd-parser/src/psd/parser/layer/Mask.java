package psd.parser.layer;

import lombok.Data;

@Data
public class Mask {
    /**Rectangle enclosing layer mask: Top, left, bottom, right*/
    int top;
    int left;
    int bottom;
    int right;
    /**0 or 255*/
    int defaultColor;
    /**position relative to layer*/
    boolean relative;
    /**layer mask disabled*/
    boolean disabled;
    /**invert layer mask when blending*/
    boolean invert;

}
