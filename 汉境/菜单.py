# -*- coding: utf-8 -*-
"""菜单 —— 原 core.Menu 的中文移植。

弹出式选择菜单：上下键移动选中项，确定/左软键选中，右软键取消或关闭。
"""

import 游戏界面
import 图形 as 图形模块
import 图形引擎
import 输入
import 绘制


class 菜单:
    def __init__(self, 屏幕):
        self.是否可见 = False
        self.游戏屏幕 = 屏幕
        self.菜单项 = []
        self.选中序号 = 0
        self.闪烁计数 = 0
        self.帧计数 = 0
        self.动作码 = 0

    def 切换可见(self):
        self.是否可见 = not self.是否可见

    def 处理按键(self, 键):
        if 键 in (输入.键星, 输入.键右软键, 输入.键确定, 输入.键下, 输入.键上):
            if self.是否可见:
                self.动作码 = 键
            return self.是否可见
        if 键 == 输入.键左软键:
            self.动作码 = 键
            return True
        # 其余键（含左/右方向键 -3/-4 等）不归菜单处理
        return False

    def 设置菜单(self, 项, 可见):
        self.是否可见 = 可见
        self.菜单项 = list(项)
        self.选中序号 = 0
        self.动作码 = 0

    def 处理菜单(self):
        """每帧调用；返回 0 表示无动作，>0 表示选中了第几项(+1)，-1 表示只是重绘。"""
        结果 = 0
        if self.是否可见:
            if self.帧计数 < 2:
                self.帧计数 += 1
            else:
                self.帧计数 = 0
                if self.闪烁计数 < 2:
                    self.闪烁计数 += 1
                else:
                    self.闪烁计数 = 0
                结果 = -1

        if self.动作码 == 输入.键星 or self.动作码 == 输入.键确定:
            if self.是否可见:
                self.切换可见()
                结果 = self.选中序号 + 1
        elif self.动作码 == 输入.键右软键:
            if self.是否可见:
                self.切换可见()
            结果 = -1
        elif self.动作码 == 输入.键左软键:
            self.切换可见()
            if not self.是否可见:
                结果 = self.选中序号 + 1
            else:
                self.选中序号 = 0
        elif self.动作码 == 输入.键下:
            if self.选中序号 + 1 < len(self.菜单项):
                self.选中序号 += 1
            else:
                self.选中序号 = 0
            结果 = -1
        elif self.动作码 == 输入.键上:
            if self.选中序号 > 0:
                self.选中序号 -= 1
            else:
                self.选中序号 = len(self.菜单项) - 1
            结果 = -1

        self.动作码 = 0
        return 结果

    def 绘制(self, 图形):
        if not self.是否可见:
            return
        字体 = 图形模块.取默认字体()
        最大宽度指针 = 0
        最大宽度 = len(self.菜单项[0])
        for i in range(1, len(self.菜单项)):
            长度 = len(self.菜单项[i])
            if 最大宽度 < 长度:
                最大宽度指针 = i
                最大宽度 = 长度
        框宽 = 字体.字符串宽度(self.菜单项[最大宽度指针]) + 24
        项高 = 字体.取高度() + 2
        框高 = (len(self.菜单项) * 项高) + 4
        x = (游戏界面.屏幕宽 - 框宽) >> 1
        y = (游戏界面.屏幕高 - 框高) >> 1
        绘制.绘制矩形(图形, x, y, 框宽, 框高, -1358954496, 16760703)
        当前y = y + 2
        for i2 in range(len(self.菜单项)):
            颜色 = 16777215
            if i2 == self.选中序号:
                self.游戏屏幕.绘制选择器(图形, x + 2, (当前y + (项高 >> 1)) - 5, self.闪烁计数)
                颜色 = 16777088
            绘制.绘制文本(self.菜单项[i2], x + 22, 当前y + 1, 图形, 颜色, 1052688)
            当前y += 项高
