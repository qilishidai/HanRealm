# -*- coding: utf-8 -*-
"""图形引擎 —— 负责 pygame 初始化、窗口、缩放、图像加载、绘制原语。

原版 J2ME 使用 176x208 的画布，本移植用底层离屏画布保持原逻辑的坐标，
再整体放大 2 倍显示到窗口（宽=176*2, 高=208*2）。
"""

import pygame

# 逻辑画布尺寸（与原 J2ME 屏幕一致）
屏幕宽 = 176
屏幕高 = 208

# 显示放大倍数
放大倍数 = 3

# 逻辑画布与显示窗口
逻辑画布 = None
显示窗口 = None

# 中文字体缓存：尺寸 -> 字体对象
_字体缓存 = {}


def 初始化(标题: str = "汉境"):
    """初始化 pygame 与窗口。"""
    global 逻辑画布, 显示窗口
    pygame.init()
    try:
        # 优先尝试显示窗口；允许通过环境变量在无显示环境下用虚拟驱动
        显示窗口 = pygame.display.set_mode((屏幕宽 * 放大倍数, 屏幕高 * 放大倍数))
        pygame.display.set_caption(标题)
    except pygame.error:
        pass
    if 显示窗口 is None:
        显示窗口 = pygame.Surface((屏幕宽 * 放大倍数, 屏幕高 * 放大倍数))
    # 逻辑离屏画布，所有绘制按原版 176x208 坐标进行
    逻辑画布 = pygame.Surface((屏幕宽, 屏幕高))
    return 显示窗口


def 加载图像(路径: str):
    """加载图片并保留调色板透明色（原 J2ME PNG 的透明背景）。"""
    try:
        原图 = pygame.image.load(路径)
        # 这些像素图是 P 模式 PNG，并在 PNG 元数据中指定了透明色索引。
        # convert_alpha 可正确保留透明图集边缘；普通 convert 会把透明区变黑。
        if 原图.get_alpha() is not None or 原图.get_colorkey() is not None:
            return 原图.convert_alpha()
        return 原图.convert()
    except Exception:
        try:
            return pygame.image.load(路径).convert_alpha()
        except Exception:
            return pygame.Surface((1, 1), pygame.SRCALPHA)


def 获取字体(尺寸: int):
    """获取一个可显示中文的字体对象。"""
    if 尺寸 in _字体缓存:
        return _字体缓存[尺寸]
    try:
        if not pygame.font.get_init():
            pygame.font.init()
    except Exception:
        pass
    字体 = None
    # 依次尝试常见中文字体族/文件
    for 名称 in ("simhei", "microsoftyahei", "msyh", "simsun", "kaiti", "stkaiti"):
        try:
            字体 = pygame.font.SysFont(名称, 尺寸)
            # 用一句话测试是否能真正渲染中文（防止 fallback 到不含汉字的字体）
            if 字体.render("汉", True, (0, 0, 0)).get_width() > 0:
                break
            字体 = None
        except Exception:
            字体 = None
    if 字体 is None:
        # 兜底：直接使用 Windows 字体文件路径
        for 路径 in ("C:/Windows/Fonts/simhei.ttf",
                     "C:/Windows/Fonts/msyh.ttc",
                     "C:/Windows/Fonts/simsun.ttc",
                     "C:/Windows/Fonts/Deng.ttf"):
            try:
                字体 = pygame.font.Font(路径, 尺寸)
                break
            except Exception:
                continue
    if 字体 is None:
        字体 = pygame.font.Font(None, 尺寸)
    _字体缓存[尺寸] = 字体
    return 字体


def 呈现():
    """把逻辑画布放大 2 倍并翻转显示。"""
    缩小 = pygame.transform.scale(逻辑画布, (屏幕宽 * 放大倍数, 屏幕高 * 放大倍数))
    显示窗口.blit(缩小, (0, 0))
    pygame.display.flip()
