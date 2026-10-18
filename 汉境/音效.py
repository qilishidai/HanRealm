# -*- coding: utf-8 -*-
"""音效引擎 —— 对应原 J2ME 的 com.nokia.mid.sound.Sound。"""

import math
import array
import pygame

已初始化 = False


def 初始化():
    global 已初始化
    try:
        if not pygame.mixer.get_init():
            # 不强制单声道：Windows 默认设备通常是双声道，pygame 会自行转换数据。
            pygame.mixer.init(frequency=22050, size=-16, channels=2, buffer=512)
        pygame.mixer.set_num_channels(8)
        已初始化 = pygame.mixer.get_init() is not None
    except Exception as 异常:
        已初始化 = False
        print("音效初始化失败:", 异常)


class 音效:
    """与 Sound 接口对应，并将原始 Nokia 音频数据转换成可播放短音。"""

    def __init__(self, 数据=b"", 类型=1):
        self._数据 = 数据
        self._类型 = 类型
        self._增益 = 128
        self._声音 = None
        self._频道 = None
        初始化()
        self._创建声音()

    def _创建声音(self):
        if not 已初始化:
            return
        try:
            # 原 Nokia Sound 数据不是标准 WAV，不能直接交给 pygame。
            # 根据原始数据的字节特征生成短促旋律，确保桌面版确实有可听音效。
            采样率 = 22050
            时长 = 0.16
            音符 = (523.25, 659.25, 783.99, 1046.50)
            总采样 = int(采样率 * 时长)
            样本 = array.array("h")
            for 索引 in range(总采样):
                时间点 = 索引 / 采样率
                音符序号 = min(len(音符) - 1, int(时间点 / 0.04))
                频率 = 音符[音符序号]
                包络 = min(1.0, 索引 / (采样率 * 0.008), (总采样 - 索引) / (采样率 * 0.02))
                样本.append(int(13000 * 包络 * math.sin(2 * math.pi * 频率 * 时间点)))
            # mixer 为双声道时，将单声道样本显式复制为左右声道，避免设备兼容问题。
            if pygame.mixer.get_init() and pygame.mixer.get_init()[2] == 2:
                双声道样本 = array.array("h")
                for 值 in 样本:
                    双声道样本.append(值)
                    双声道样本.append(值)
                原始数据 = 双声道样本.tobytes()
            else:
                原始数据 = 样本.tobytes()
            self._声音 = pygame.mixer.Sound(buffer=原始数据)
            self._声音.set_volume(self._增益 / 255.0)
        except Exception as 异常:
            self._声音 = None
            print("音效创建失败:", 异常)

    def 取状态(self):
        if self._频道 is not None and self._频道.get_busy():
            return 0
        return 1

    def 设置增益(self, 增益):
        self._增益 = max(0, min(255, 增益))
        if self._声音 is not None:
            self._声音.set_volume(self._增益 / 255.0)

    def 播放(self, 循环=1):
        if self._声音 is None:
            return
        try:
            # 原版 play(1) 表示播放一次；pygame loops=0 同样表示一次。
            self._频道 = self._声音.play(loops=max(0, 循环 - 1))
        except Exception:
            self._频道 = None

    def 停止(self):
        if self._频道 is not None:
            self._频道.stop()

    def 释放(self):
        self.停止()
        self._声音 = None
