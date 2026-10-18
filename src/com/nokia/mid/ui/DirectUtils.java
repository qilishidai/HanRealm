package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;

/**
 * Nokia DirectUtils 兼容桩。
 *
 * 返回一个包装标准 Graphics 的 DirectGraphics 实例。
 */
public class DirectUtils {

    private DirectUtils() {
    }

    public static DirectGraphics getDirectGraphics(Graphics graphics) {
        return new DirectGraphics(graphics);
    }
}