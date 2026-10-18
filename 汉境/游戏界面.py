# -*- coding: utf-8 -*-
"""游戏界面 —— 原 core.GameScreen + CatMid 的中文移植。

管理启动画面、主菜单、游戏内循环（后台线程驱动游戏逻辑）、
按键分发、读档/存档。
"""

import time
import os
import traceback

import pygame

import 图形引擎
import 图形
import 图片
import 音效
import 输入
import 数据流
import 绘制
import 菜单 as 菜单模块
import 游戏

# 屏幕尺寸（逻辑，与引擎一致）
屏幕宽 = 176
屏幕高 = 208

# 音效开关
音效开关 = True


def 绘制文本(文本, x, y, 图形对象, 颜色, 阴影色):
    """原 GameScreen.drawText。"""
    绘制.绘制文本(文本, x, y, 图形对象, 颜色, 阴影色)


def 绘制矩形(图形对象, x, y, 宽, 高, 填充色, 边框色):
    """原 GameScreen.drawRect。"""
    绘制.绘制矩形(图形对象, x, y, 宽, 高, 填充色, 边框色)


class 主界面:
    """原 GameScreen + MIDlet 生命周期。"""

    def __init__(self):
        self.游戏 = None
        self.运行中 = True
        self.瓦片图像 = None
        self.菜单 = None
        self.启动图像 = None
        self.结束图像 = None
        self.游戏状态 = 0              # 0-4运行 5退出
        self.主菜单项 = ["继续游戏", "新游戏", "帮助", "制作组", "退出"]
        self.存档名 = "china"
        self.字体 = 图形.取默认字体()
        self.菜单状态 = 23             # 21主菜单 22游戏中 23加载中
        self.启动计数 = 60
        self.暂停 = False
        self.音效开关 = True
        self.瓦片尺寸 = 9
        self.界面 = 图形.图形()
        # 与游戏逻辑共享的屏幕尺寸（初始化时由引擎填充）
        self.屏幕宽 = 屏幕宽
        self.屏幕高 = 屏幕高
        # pygame 可能为长按自动产生重复 KEYDOWN；记录物理按下状态，避免菜单重复移动
        self.按下键集合 = set()

    # ---------- 退出 ----------
    def 退出(self):
        self.运行中 = False
        self.游戏状态 = 5

    # ---------- 开始（单线程主循环） ----------
    def 开始(self):
        图形引擎.初始化("汉境")
        # 音效必须在 pygame 初始化后显式启动；Windows 上未初始化 mixer 时不会有声音。
        音效.初始化()
        游戏.当前关卡 = 1
        try:
            self.启动图像 = 图片.创建图片("/mt.png")
            self.结束图像 = 图片.创建图片("/st.png")
        except Exception as e:
            print("载入图片失败:", e)
        try:
            self.加载游戏(False)
        except Exception:
            pass
        try:
            self.事件循环()
        except SystemExit:
            pass
        finally:
            pygame.quit()

    # ---------- 事件循环 + 游戏循环（单线程同帧） ----------
    def 事件循环(self):
        时钟 = pygame.time.Clock()
        上一时刻 = time.time()
        while self.运行中:
            for 事件 in pygame.event.get():
                if 事件.type == pygame.QUIT:
                    self.退出()
                elif 事件.type == pygame.KEYDOWN:
                    if 事件.key in self.按下键集合:
                        continue
                    self.按下键集合.add(事件.key)
                    键 = 输入.映射按键(事件)
                    if 键 != 0:
                        self.按键按下(键)
                elif 事件.type == pygame.KEYUP:
                    self.按下键集合.discard(事件.key)
                    键 = 输入.映射按键(事件)
                    if 键 != 0:
                        self.按键释放(键)
            if not self.暂停:
                self.更新游戏()
                self.重绘屏幕()
            当前 = time.time()
            间隔 = (当前 - 上一时刻) * 1000
            if 间隔 < 50:
                time.sleep((50 - 间隔) / 1000.0)
            上一时刻 = time.time()
            图形引擎.呈现()
            时钟.tick(60)

    # ---------- 更新游戏 ----------
    def 更新游戏(self):
        try:
            if self.菜单状态 == 21:
                if self.菜单 is not None and not self.菜单.是否可见:
                    self.菜单.切换可见()
                self.处理主菜单()
            elif self.菜单状态 == 22:
                if not self.游戏.是否繁忙():
                    self.处理主菜单()
                if self.游戏.更新(self):
                    self.菜单状态 = 21
            elif self.菜单状态 == 23:
                if self.启动计数 <= 0:
                    self.初始化游戏()
                else:
                    self.启动计数 -= 1
                    if self.启动计数 == 0:
                        try:
                            self.启动图像 = self.结束图像
                        except Exception:
                            pass
        except Exception as e:
            print("updateGame:", e)
            traceback.print_exc()

    # ---------- 初始化游戏 ----------
    def 初始化游戏(self):
        try:
            self.瓦片图像 = 图片.创建图片("/mmm.png").表面
            self.菜单 = 菜单模块.菜单(self)
            self.游戏 = 游戏.游戏(self.字体, self)
            self.菜单状态 = 21
            self.菜单.设置菜单(self.主菜单项, True)
        except Exception as e:
            print("initGame:", e)

    # ---------- 玩家操作主菜单 ----------
    def 处理主菜单(self):
        结果 = self.菜单.处理菜单()
        if 结果 == 1:
            if self.菜单状态 != 22:
                self.启动图像 = None
                if not self.加载游戏(True):
                    self.游戏.初始化关卡(True)
                self.菜单状态 = 22
        elif 结果 == 2:
            self.启动图像 = None
            self.游戏.初始化关卡(True)
            self.菜单状态 = 22
        elif 结果 == 3:
            self.显示帮助()
        elif 结果 == 4:
            self.显示制作组()
        elif 结果 == 5:
            if self.菜单状态 == 22:
                self.保存游戏()
            self.退出()

    # ---------- 显示帮助 ----------
    def 显示帮助(self):
        self.暂停 = True
        内容 = ("【描述】\n这款游戏将带你回到两千年前的遥远西域，让你在手机上再现大汉王朝的辉煌!"
                "游戏中有趣的外交，逼真的战斗，多种特产资源，丝丝入扣的任务系统以及八种不同的城市建筑。\n"
                "【规则】策略战棋类游戏，类似经典游戏<文明>的回合制操作方式。游戏共有6关，每一关有特定的任务。\n"
                "【操作】左功能键弹出游戏菜单。方向键移动光标。选择退出时，游戏自动保存当前的状态。")
        self.游戏.显示对话框("帮助", 内容, True, 0)
        self.暂停 = False

    # ---------- 显示制作组 ----------
    def 显示制作组(self):
        self.暂停 = True
        self.游戏.显示对话框("制作组",
            "汉风工作室\n【策划】龙猫,大款\n【设计】大款\n【美工】古月天声\n【测试】小雅,小芯\n【特别感谢】lingyu_ma,kingsgame等", True, 0)
        self.暂停 = False

    # ---------- 按键按下 ----------
    def 按键按下(self, 键):
        if self.菜单状态 == 21:
            if 键 == 输入.键右软键:
                return
        elif self.菜单状态 == 23:
            if self.启动计数 > 1:
                self.启动计数 = 1
                return
            return
        if not (self.菜单状态 == 22 and self.游戏.是否繁忙()) and self.菜单.处理按键(键):
            return
        if self.游戏 is not None:
            self.游戏.按键按下(键)

    # ---------- 按键释放 ----------
    def 按键释放(self, 键):
        if self.菜单状态 != 22 or self.菜单.是否可见:
            return
        if self.游戏 is not None:
            self.游戏.按键释放()

    # ---------- 重绘（绘制到逻辑画布并呈现） ----------
    def 重绘屏幕(self):
        图形对象 = self.界面
        图形对象.设置颜色(0)
        图形对象.填充矩形(0, 0, 屏幕宽, 屏幕高)
        if self.菜单状态 == 21:
            if self.启动图像 is not None:
                图形对象.绘制图片(self.启动图像, 屏幕宽 // 2, 屏幕高 // 2, 1 | 2)
            self.绘制底部栏(图形对象, self.游戏状态)
            if self.菜单 is not None:
                self.菜单.绘制(图形对象)
        elif self.菜单状态 == 22:
            if self.游戏 is not None:
                self.游戏.绘制(图形对象)
            if not self.游戏.是否繁忙():
                self.绘制底部栏(图形对象, self.游戏状态)
                if self.菜单 is not None:
                    self.菜单.绘制(图形对象)
        elif self.菜单状态 == 23:
            self.绘制启动画面(图形对象)
        图形引擎.呈现()

    # ---------- 绘制启动画面 ----------
    def 绘制启动画面(self, 图形对象):
        if self.启动图像 is not None:
            图形对象.设置颜色(0 if self.启动计数 <= 0 else 16777215)
            图形对象.填充矩形(0, 0, 屏幕宽, 屏幕高)
            图形对象.绘制图片(self.启动图像, 屏幕宽 // 2, 屏幕高 // 2, 1 | 2)
            行高 = self.字体.取高度() + 5
            if self.启动计数 <= 0:
                绘制文本("载入中，请等待....", 4, 屏幕高 - 行高, 图形对象, 0, 16777215)
            else:
                绘制文本("按键继续(" + str(self.启动计数 // 6) + ")", 4, 屏幕高 - 行高, 图形对象, 0, 16777215)

    # ---------- 绘制瓦片 ----------
    def 绘制瓦片(self, 图形对象, x, y, 瓦片序号):
        图形对象.设置裁剪(x, y, self.瓦片尺寸, self.瓦片尺寸)
        图形对象.绘制图片(self.瓦片图像, x - (瓦片序号 * self.瓦片尺寸), y, 8)
        图形对象.设置裁剪(0, 0, 屏幕宽, 屏幕高)

    # ---------- 绘制选择器 ----------
    def 绘制选择器(self, 图形对象, x, y, 帧):
        图形对象.设置裁剪(x, y, 18, 11)
        图形对象.绘制图片(self.瓦片图像, (x - (帧 * 18)) - 36, y, 8)
        图形对象.设置裁剪(0, 0, 屏幕宽, 屏幕高)

    # ---------- 绘制底部栏 ----------
    def 绘制底部栏(self, 图形对象, 状态):
        i2 = self.瓦片尺寸
        i3 = 屏幕宽 - i2
        i4 = 屏幕高 - i2
        if self.菜单 is not None and self.菜单.是否可见:
            self.绘制瓦片(图形对象, 1, i4, 0)
            if 状态 != 0:
                self.绘制瓦片(图形对象, i3, i4, 1)
            return
        self.绘制瓦片(图形对象, 1, i4, 2)
        if 状态 == 0:
            self.绘制瓦片(图形对象, i3, i4, 0)
        else:
            self.绘制瓦片(图形对象, i3, i4, 3)

    # ---------- 睡眠 ----------
    def 睡眠(self, 毫秒):
        """阻塞等待时保持窗口响应：处理事件、绘制当前帧。"""
        结束 = time.time() + (毫秒 / 1000.0)
        while time.time() < 结束:
            for 事件 in pygame.event.get():
                if 事件.type == pygame.KEYDOWN:
                    if 事件.key in self.按下键集合:
                        continue
                    self.按下键集合.add(事件.key)
                    键 = 输入.映射按键(事件)
                    if 键 != 0:
                        self.按键按下(键)
                elif 事件.type == pygame.KEYUP:
                    self.按下键集合.discard(事件.key)
                    键 = 输入.映射按键(事件)
                    if 键 != 0:
                        self.按键释放(键)
            self.重绘屏幕()
            图形引擎.呈现()
            time.sleep(0.005)

    # ---------- 加载游戏 ----------
    def 加载游戏(self, 读取数据):
        存档名 = self.存档名 + ".dat"
        if not os.path.exists(存档名):
            return False
        成功 = False
        try:
            with open(存档名, "rb") as f:
                记录 = f.read()
            流 = 数据流.输入流(记录)
            音效开关 = 流.读bool()
            游戏.当前关卡 = 流.读int()
            if 读取数据 and self.游戏 is not None:
                self.游戏.从流加载(流)
                成功 = True
        except Exception:
            return 成功
        return 成功

    # ---------- 保存游戏 ----------
    def 保存游戏(self):
        输出 = 数据流.输出流()
        输出.写bool(音效开关)
        输出.写int(游戏.当前关卡)
        if self.菜单状态 == 22 and self.游戏 is not None:
            self.游戏.存到流(输出)
        存档名 = self.存档名 + ".dat"
        try:
            with open(存档名, "wb") as f:
                f.write(输出.取字节())
        except Exception as e:
            print("saveGame:", e)
