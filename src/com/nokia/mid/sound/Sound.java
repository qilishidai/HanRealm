package com.nokia.mid.sound;

/**
 * Nokia Sound 兼容桩。
 *
 * 标准 MIDP 不包含 com.nokia.mid.sound.Sound 简易音效接口。这里构造与
 * 字段保持一致以通过编译，方法为空实现（无声版）。
 */
public class Sound {

    /** 播放中 */
    public static final int SOUND_PLAYING = 0;
    /** 已停止 */
    public static final int SOUND_STOPPED = 1;
    /** 未初始化 */
    public static final int SOUND_UNINITIALIZED = 2;

    private final byte[] data;
    private final int type;

    public Sound(byte[] data, int type) {
        this.data = data;
        this.type = type;
    }

    /** 桩实现恒为已停止，便于 getState()!=0 分支（对应原本"没在播则播放"的逻辑）。 */
    public int getState() {
        return SOUND_STOPPED;
    }

    /** 设置音量（0-255），桩实现不做处理。 */
    public void setGain(int gain) {
    }

    /** 播放（loop 次循环），桩实现不做处理。 */
    public void play(int loop) {
    }

    public void stop() {
    }

    public void release() {
    }
}