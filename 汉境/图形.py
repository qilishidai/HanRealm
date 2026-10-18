# -*- coding: utf-8 -*-
"""图形上下文 —— 对应原 J2ME 的 Graphics / Font / DirectGraphics。

在 176x208 的逻辑画布上进行绘制；drawImage/drawString 的 anchor 与 Midp 一致。

surface 延迟到首次绘制时从 图形引擎.逻辑画布 获取，避免在引擎初始化前
缓存到 None。
"""

import pygame

import 图形引擎


# Midp 绘制锚点常量
左 = 8
右 = 64
上 = 0
下 = 16
水平居中 = 1
垂直居中 = 2


class 字体:
    """包装 pygame Font，提供 Midp Font 接口。"""

    def __init__(self, 尺寸=12):
        self._字体 = 图形引擎.获取字体(尺寸)

    def 取高度(self):
        return self._字体.get_linesize()  # 行高近似 getHeight

    def 字符串宽度(self, 文本: str) -> int:
        return self._字体.size(文本)[0]

    def 字符宽度(self, 字符) -> int:
        return self._字体.size(字符)[0]

    def 渲染(self, 文本: str, 颜色, 反锯齿=True):
        return self._字体.render(文本, 反锯齿, 颜色)


# 原版用的字体（尺寸8 与系统默认）。延迟到首次获取时真正加载字体，避免导入期调用 pygame。
字体对象 = None


def 取默认字体() -> 字体:
    global 字体对象
    if 字体对象 is None:
        字体对象 = 字体(12)
    return 字体对象


class 图形:
    """包装逻辑画布 surface，提供与原版一致的绘制原语。"""

    def __init__(self, 表面=None):
        self._绑定表面 = 表面
        self._颜色 = (0, 0, 0)

    def _取表面(self):
        if self._绑定表面 is not None:
            return self._绑定表面
        return 图形引擎.逻辑画布

    @property
    def 表面(self):
        return self._取表面()

    # ---- 颜色 ----
    def 设置颜色(self, 颜色值):
        """颜色值为 0xRRGGBB 整数。"""
        self._颜色 = ((颜色值 >> 16) & 255, (颜色值 >> 8) & 255, 颜色值 & 255)
        return self

    def 取颜色(self):
        return self._颜色

    # ---- 区域裁剪 ----
    def 设置裁剪(self, x, y, 宽, 高):
        self.表面.set_clip(pygame.Rect(x, y, 宽, 高))

    def 清裁剪(self):
        self.表面.set_clip(None)

    # ---- 基本绘制 ----
    def 填充矩形(self, x, y, 宽, 高):
        表面 = self.表面
        表面.fill(self._颜色, (x, y, 宽, 高))

    def 画矩形(self, x, y, 宽, 高):
        pygame.draw.rect(self.表面, self._颜色, (x, y, 宽, 高), 1)

    def 画线(self, x1, y1, x2, y2):
        pygame.draw.line(self.表面, self._颜色, (x1, y1), (x2, y2))

    # ---- 绘制图像（带锚点） ----
    def 绘制图片(self, 图片对象, x, y, 锚点):
        """anchor 使用 Midp 位标志。计算左上角后 blit。

        图片对象 可以是 图片.图片 包装，也可直接是 pygame Surface。
        """
        图像表面 = getattr(图片对象, "表面", 图片对象)
        宽 = 图像表面.get_width()
        高 = 图像表面.get_height()
        if (锚点 & 8) != 0:      # LEFT
            px = x
        elif (锚点 & 64) != 0:   # RIGHT
            px = x - 宽
        else:                    # HCENTER
            px = x - (宽 >> 1)
        if (锚点 & 16) != 0:     # BOTTOM
            py = y - 高
        elif (锚点 & 2) != 0:    # VCENTER
            py = y - (高 >> 1)
        else:                    # TOP
            py = y
        self.表面.blit(图像表面, (px, py))

    # ---- 绘制文本（带锚点） ----
    def 绘制字符串(self, 文本: str, x, y, 锚点, 字体=None, 颜色=None, 背景=None):
        字体 = 取默认字体() if 字体 is None else 字体
        if 颜色 is None:
            颜色 = self._颜色
        文本表面 = 字体._字体.render(文本, True, 颜色)
        宽 = 文本表面.get_width()
        高 = 文本表面.get_height()
        if (锚点 & 8) != 0:
            px = x
        elif (锚点 & 64) != 0:
            px = x - 宽
        else:
            px = x - (宽 >> 1)
        if (锚点 & 16) != 0:
            py = y - 高
        elif (锚点 & 2) != 0:
            py = y - (高 >> 1)
        else:
            py = y
        self.表面.blit(文本表面, (px, py))

    # ---- 多边形填充（DirectGraphics.fillPolygon 等价） ----
    def 填充多边形(self, x点, x偏移, y点, y偏移, 点数, 填充颜色):
        颜色 = ((填充颜色 >> 16) & 255, (填充颜色 >> 8) & 255, 填充颜色 & 255)
        xs = [x点[x偏移 + i] for i in range(点数)]
        ys = [y点[y偏移 + i] for i in range(点数)]
        pygame.draw.polygon(self.表面, 颜色, list(zip(xs, ys)))
