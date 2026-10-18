# -*- coding: utf-8 -*-
"""绘制辅助 —— 原 GameScreen 静态方法 drawText / drawRect 的中文移植。"""

import 图形
import 图形引擎
import 输入


def 绘制文本(文本: str, x: int, y: int, 图形对象, 颜色: int, 阴影色: int):
    """带可选阴影的文字绘制。阴影色为 0 时不绘制阴影。"""
    if 阴影色 != 0:
        图形对象.设置颜色(阴影色)
        # 右下偏移 1 像素作为阴影
        _画字(图形对象, 文本, x + 1, y + 1, 颜色值=阴影色)
    图形对象.设置颜色(颜色)
    _画字(图形对象, 文本, x, y, 颜色值=颜色)


def _画字(图形对象, 文本, x, y, 颜色值):
    """用锚点 LEFT|TOP(8) 绘制，即左上角定位。"""
    import 图形引擎
    字体 = 图形.取默认字体()
    面 = 字体._字体.render(文本, True, 图形对象.取颜色())
    图形对象.表面.blit(面, (x, y))


def 绘制矩形(图形对象, x: int, y: int, 宽: int, 高: int, 填充色: int, 边框色: int):
    """对应 GameScreen.drawRect：填充多边形 + 可选边框。"""
    填入颜色 = ((填充色 >> 16) & 255, (填充色 >> 8) & 255, 填充色 & 255)
    x点 = [x, x + 宽, x + 宽, x]
    y点 = [y, y, y + 高, y + 高]
    pygame多边形填充(图形对象, x点, y点, 填入颜色)
    if 边框色 != 0:
        图形对象.设置颜色(边框色)
        图形对象.画矩形(x, y, 宽, 高)


def pygame多边形填充(图形对象, x点, y点, 颜色):
    import pygame
    pygame.draw.polygon(图形对象.表面, 颜色, list(zip(x点, y点)))
