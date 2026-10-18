package com.nokia.mid.ui;

import javax.microedition.lcdui.Canvas;

/**
 * Nokia FullCanvas 兼容桩。
 *
 * 该反编译工程使用 Nokia 的全屏画布与自定义负码按键。标准 MIDP 的
 * Canvas 只有正码方向键/数字键，没有软键。这里用 Canvas 代替，并补上
 * 游戏里 switch 所依赖的负码常量，便于编译与对照。
 */
public abstract class FullCanvas extends Canvas {

    /** 上(方向键) */
    public static final int KEY_UP = -1;
    /** 下 */
    public static final int KEY_DOWN = -2;
    /** 左 */
    public static final int KEY_LEFT = -3;
    /** 右 */
    public static final int KEY_RIGHT = -4;
    /** 确定 */
    public static final int KEY_SELECT = -5;
    /** 左软键(菜单) */
    public static final int KEY_SOFTKEY0 = -6;
    /** 右软键 */
    public static final int KEY_SOFTKEY1 = -7;
    /** 挂机/呼叫 键 */
    public static final int KEY_SEND = -8;
    /** 结束键 */
    public static final int KEY_END = -9;
    /** * 键 */
    public static final int KEY_STAR = -10;
    /** # 键 */
    public static final int KEY_POUND = -11;

    protected FullCanvas() {
        super();
    }
}