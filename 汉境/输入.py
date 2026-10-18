# -*- coding: utf-8 -*-
"""输入引擎 —— 把 pygame 键码映射为原版 Nokia FullCanvas 的负码。

原版按键逻辑针对 -1..-11 等负码做 switch，这里把方向键/字母键翻译回去。
"""

import pygame

# 与 com.nokia.mid.ui.FullCanvas 兼容的负码
键上 = -1
键下 = -2
键左 = -3
键右 = -4
键确定 = -5
键左软键 = -6
键右软键 = -7
键挂机 = -8
键结束 = -9
键星 = -10
键井 = -11

# 特殊"功能码"（原代码里用作快捷选择等）
功能选择下一城 = 49   # '1'
功能选择下一部队 = 50 # '2'
功能打开游戏菜单 = 51 # '3'
功能选择上一城 = 52   # '4'


def _映射键(键) -> int:
    """把 pygame 键码映射为原版负码；无法映射返回 0。"""
    if 键 == pygame.K_UP:
        return 键上
    if 键 == pygame.K_DOWN:
        return 键下
    if 键 == pygame.K_LEFT:
        return 键左
    if 键 == pygame.K_RIGHT:
        return 键右
    if 键 in (pygame.K_RETURN, pygame.K_SPACE, pygame.K_KP_ENTER):
        return 键确定
    if 键 in (pygame.K_a, pygame.K_m):
        return 键左软键
    if 键 in (pygame.K_b, pygame.K_ESCAPE):
        return 键右软键
    if 键 in (pygame.K_1, pygame.K_KP1):
        return 功能选择下一城
    if 键 in (pygame.K_2, pygame.K_KP2):
        return 功能选择下一部队
    if 键 in (pygame.K_3, pygame.K_KP3):
        return 功能打开游戏菜单
    if 键 in (pygame.K_4, pygame.K_KP4):
        return 功能选择上一城
    return 0


def 映射按键(事件) -> int:
    """把 pygame.key 事件映射为原版负码；无法映射返回 0。

    KEYDOWN 与 KEYUP 都映射，供 按键按下/按键释放 使用。
    """
    if 事件.type in (pygame.KEYDOWN, pygame.KEYUP):
        return _映射键(事件.key)
    return 0
