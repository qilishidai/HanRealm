# -*- coding: utf-8 -*-
"""游戏 —— 原 core.Game 的中文移植。

地图解析/渲染、光标移动、菜单状态机、城市菜单、生产/贸易/外交、
AI 回合、胜负判定与任务完成检查、流式存档。
"""

import random

import pygame

import 游戏界面
import 输入
import 数据表
import 城市
import 部队
import 绘制
import 图片
import 图形
import 图形引擎
import 关卡数据
import 音效
import 数据流
import 菜单 as 菜单模块
from 游戏数据 import (
    战役名, 精灵帧, 瓦片偏移, 状态栏颜色, 任务条件, 外交矩阵,
    外交动作, 外交消息, 使者结果, 确认文本, 取消文本, 菜单标签,
    关系名, 城市信息标签, 国家信息标签, 音效数据,
)

# 模块级共享（避免循环导入）
当前关卡 = 1
战斗模式 = True
势力名 = ["汉朝", "匈奴", "大月氏", "楼兰", "大宛"]
势力颜色 = [2147483392, 2130771712, 2130706687, 134152447, 2147418112]


def 格式化消息(格式: str, 参数: list) -> str:
    """原 Game.formatMessage：把 %0、%1… 替换为参数。"""
    结果 = ""
    i = 0
    while i < len(格式):
        字符 = 格式[i]
        if 字符 != "%":
            结果 += 字符
        else:
            i += 1
            if i < len(格式):
                字符2 = 格式[i]
                if '0' <= 字符2 <= '9':
                    索引 = ord(字符2) - ord('0')
                    if 索引 < len(参数) and 参数[索引] is not None:
                        结果 += str(参数[索引])
                else:
                    结果 += 字符2
        i += 1
    return 结果


class 游戏:
    def __init__(self, 字体, 屏幕):
        self.字体 = 字体
        self.游戏屏幕 = 屏幕
        self.菜单 = 菜单模块.菜单(屏幕)
        self.信息栏高度 = 游戏界面.屏幕高 - (字体.取高度() + 6)
        self.屏幕偏移y = 0
        if self.信息栏高度 > 80:
            self.屏幕偏移y = (self.信息栏高度 - 80) >> 1
            self.信息栏高度 = 80 + self.屏幕偏移y
        if self.屏幕偏移y < 0:
            self.屏幕偏移y = 0
        self.城市们 = [城市.城市(self, i + 1) for i in range(8)]
        self.部队们 = [部队.部队(self, i + 1) for i in range(96)]
        # 图像
        self.地图图像 = 图片.创建图片("/map.png").表面
        self.对象图像 = 图片.创建图片("/objs.png").表面
        self.对话框宽度 = 0   # 按键状态：0=无按键，负码=待处理键，功能码=快捷操作
        self.对话框高度 = 0   # 按键释放标记：0=按下未释放，1=已释放
        # 对话框框体尺寸（原版 Game.screenWidth/Height 的对话框用途）
        self.对话框屏幕宽 = 0
        self.对话框屏幕高 = 0
        self.对话框行 = ["", ""]
        self.对话框行数 = 0
        self.对话框文本 = [""] * 24
        self.对话框颜色 = [0] * 24
        self.任务标题 = ""
        self.地图数据 = []
        self.城市名 = []
        self.外交数据 = []
        self.选中城 = None
        self.临时部队 = None
        self.选中部队 = None
        self.城市数 = 0
        self.部队数 = 0
        self.光标x = 0
        self.光标y = 0
        self.视野倍率 = 25
        self.光标位置 = [0, 0]
        self.键码 = 0
        self.键状态 = 0
        self.游戏状态 = 0
        self.对话框结果 = 0
        self.帧计数 = 0
        self.动画计数 = 0
        self.回合计数 = 0
        self.菜单状态 = 1
        self.子菜单索引 = 0
        self.城市菜单索引 = 0
        self.城市信息索引 = 0
        self.菜单选项索引 = [0] * 12
        self.菜单选项数 = 0
        self.外交索引 = 0
        self.外交动作 = 0
        self.外交目标 = 0
        self.城市信息标签 = ["城市", "归属", "人口"]
        self.国家信息标签 = ["国家", "关系", "友好"]
        self.游戏结束 = False
        self.需要重绘 = True
        self.随机源 = random.Random()
        self.音效对象 = 音效.音效(音效数据, 1)
        # 供 部队.绘制部队 访问的精灵帧表
        self.精灵帧 = 精灵帧

    # ---------- 声音 ----------
    def 播放声音(self):
        if 游戏界面.音效开关 and self.音效对象.取状态() != 0:
            self.音效对象.设置增益(240)
            self.音效对象.播放(1)

    # ---------- 初始化关卡 ----------
    def 初始化关卡(self, 选择关卡):
        global 当前关卡
        self.子菜单索引 = 0
        self.游戏结束 = False
        self.视野倍率 = 25
        if 选择关卡:
            if self.对象图像 is None:
                try:
                    self.对象图像 = 图片.创建图片("/objs.png").表面
                except Exception:
                    pass
            if 当前关卡 > 1:
                if 当前关卡 > len(战役名):
                    当前关卡 = len(战役名)
                self.菜单.设置菜单(战役名[:当前关卡], True)
                self.菜单状态 = 11
                return
            self.对话框结果 = 0
        else:
            self.对话框结果 += 1
            if 当前关卡 < self.对话框结果 + 1:
                当前关卡 = self.对话框结果 + 1
            if 当前关卡 > len(战役名):
                当前关卡 = len(战役名)
            if self.对话框结果 >= len(战役名):
                self.显示对话框("胜利", "经过近百年的浴血奋战，汉朝终于切地击败匈奴。你完成了全部任务。", True, 0)
                self.游戏结束 = True
                return
        self.重置游戏状态()

    # ---------- 重置游戏状态 ----------
    def 重置游戏状态(self):
        self.游戏状态 = 0
        self.需要重绘 = True
        self.游戏结束 = False
        self.对话框宽度 = 0
        self.对话框高度 = 0
        self.加载地图数据(True)
        self.临时部队 = None
        行数 = len(self.地图数据)
        列数 = len(self.地图数据[0]) if 行数 else 0
        self.地图滚动x = 20 * (行数 + 列数)
        self.地图滚动y = 10 * (行数 + 列数)
        self.移动光标(self.键码, self.键状态)
        self.菜单状态 = 0

    # ---------- 从流加载 ----------
    def 从流加载(self, 流):
        global 战斗模式
        战斗模式 = 流.读bool()
        self.子菜单索引 = 流.读int()
        self.视野倍率 = 流.读int()
        if self.对象图像 is None:
            self.对象图像 = 图片.创建图片("/objs.png").表面
        self.对话框结果 = 流.读int()
        self.键码 = 流.读int()
        self.键状态 = 流.读int()
        self.需要重绘 = True
        self.游戏结束 = False
        self.对话框宽度 = 0
        self.对话框高度 = 0
        宽 = 流.读int()
        高 = 流.读int()
        self.地图数据 = [[0 for _ in range(宽)] for _ in range(高)]
        for yy in range(高):
            for xx in range(宽):
                self.地图数据[yy][xx] = 流.读int() & 0x7FFFFFFF
        self.城市信息索引 = 流.读int()
        self.城市菜单索引 = 流.读int()
        for 单位 in self.部队们:
            单位.重置部队()
        for 城 in self.城市们:
            城.科技等级 = 0
        for i in range(self.城市信息索引):
            self.部队们[i].从流加载(流)
        for i2 in range(self.城市菜单索引):
            self.城市们[i2].从流加载(流)
        行数 = 流.读int()
        self.外交数据 = []
        for _ in range(行数):
            列数 = 流.读int()
            self.外交数据.append([流.读int() for _ in range(列数)])
        self.游戏状态 = self.选择格子(self.键码, self.键状态)
        self.地图滚动x = 20 * (高 + 宽)
        self.地图滚动y = 10 * (高 + 宽)
        self.移动光标(self.键码, self.键状态)
        self.菜单状态 = 1
        self.加载地图数据(False)

    # ---------- 保存到流 ----------
    def 存到流(self, 流):
        global 战斗模式
        流.写bool(战斗模式)
        流.写int(self.子菜单索引)
        流.写int(self.视野倍率)
        流.写int(self.对话框结果)
        流.写int(self.键码)
        流.写int(self.键状态)
        宽 = len(self.地图数据[0])
        高 = len(self.地图数据)
        流.写int(宽)
        流.写int(高)
        for yy in range(高):
            for xx in range(宽):
                流.写int(self.地图数据[yy][xx])
        流.写int(self.城市信息索引)
        流.写int(self.城市菜单索引)
        for i in range(self.城市信息索引):
            self.部队们[i].存到流(流)
        for i2 in range(self.城市菜单索引):
            self.城市们[i2].存到流(流)
        行数 = len(self.外交数据)
        流.写int(行数)
        for 行 in self.外交数据:
            列数 = len(行)
            流.写int(列数)
            for 值 in 行:
                流.写int(值)

    # ---------- 加载地图数据 ----------
    def 加载地图数据(self, 初始化):
        包 = 关卡数据.获取关卡包()
        关 = 包.关卡[self.对话框结果]
        self.城市名 = 关.城名
        self.对话框行 = 关.任务行
        if not 初始化:
            return
        self.外交数据 = [list(行) for 行 in 外交矩阵[self.对话框结果]]
        self.显示对话框(战役名[self.对话框结果],
                        self.对话框行[0] if len(self.对话框行) > 0 else "", False, 0)
        self.添加对话框行(self.对话框行[1] if len(self.对话框行) > 1 else "", 11468800)
        宽 = 关.宽
        高 = 关.高
        self.地图数据 = [[0 for _ in range(宽)] for _ in range(高)]
        for 城 in self.城市们:
            城.科技等级 = 0
        for 单位 in self.部队们:
            单位.重置部队()
        self.城市菜单索引 = 0
        self.城市信息索引 = 0
        for yy in range(高):
            for xx in range(宽):
                值 = 关.地图数据[yy][xx]
                物体 = (值 >> 8) & 255
                if 物体 == 12 or 物体 == 13:
                    if self.城市菜单索引 < len(self.城市们):
                        城对象 = self.城市们[self.城市菜单索引]
                        归属id = 城对象.归属id
                        等级 = 物体 - 12
                        规模 = (值 >> 16) & 15
                        势力序号 = (值 >> 20) & 15
                        城对象.初始化城市(self.城市名[归属id - 1], xx, yy, 等级, 规模, 势力序号)
                        self.城市菜单索引 += 1
                        self.地图数据[yy][xx] = (值 & 255) | 3072 | (归属id << 16)
                        if 势力序号 == 0:
                            self.键码 = xx
                            self.键状态 = yy
                            self.游戏状态 = 归属id
                    else:
                        self.地图数据[yy][xx] = 值 & 255
                elif 28 <= 物体 <= 32:
                    部队id = self.由地图创建部队(值 >> 8, xx, yy)
                    self.地图数据[yy][xx] = (值 & 255) | (部队id << 24)
                else:
                    self.地图数据[yy][xx] = 值

    # ---------- 由地图数据创建部队 ----------
    def 由地图创建部队(self, 数据, x, y):
        if self.城市信息索引 >= len(self.部队们):
            return 0
        兵种 = (数据 & 255) - 28
        单位 = self.部队们[self.城市信息索引]
        部队id = 单位.部队id
        造价 = 部队.兵种属性[兵种][0]
        单位.初始化部队(造价, (造价 * ((数据 >> 12) & 15)) // 10, 兵种, (数据 >> 16) & 15, (数据 >> 8) & 15)
        单位.地图x = x
        单位.地图y = y
        self.城市信息索引 += 1
        return 部队id

    # ---------- 更新动画计数器 ----------
    def 更新动画计数(self):
        if self.帧计数 < 3:
            self.帧计数 += 1
            return
        self.帧计数 = 0
        self.需要重绘 = True
        if self.动画计数 < 6:
            self.动画计数 += 1
        else:
            self.动画计数 = 0

    # ---------- 显示游戏菜单 ----------
    def 显示游戏菜单(self):
        项 = ["回合结束", "指挥战斗" if 战斗模式 else "自动战斗",
              "关闭音效" if 游戏界面.音效开关 else "开启音效", "任务描述"]
        self.菜单.设置菜单(项, True)
        self.菜单状态 = 9
        self.需要重绘 = True
        self.对话框宽度 = 0

    # ---------- 主更新 ----------
    def 更新(self, 屏幕):
        if self.对话框行数 > 0:
            self.处理对话框输入()
        else:
            if self.游戏结束:
                self.对象图像 = None
                return True
            if self.菜单状态 == 0:
                for i in range(self.城市菜单索引):
                    self.城市们[i].扩张领域()
                self.菜单状态 = 1
            elif self.菜单状态 == 1:
                self.更新部队动画()
                self.处理光标输入()
            elif self.菜单状态 == 3:
                self.处理部队移动()
            elif self.菜单状态 == 5 or self.菜单状态 == 6:
                self.处理城市菜单()
            elif self.菜单状态 == 8:
                # 与原版一致：确定后进入执行状态，外交方法负责显示成功/失败提示。
                self.执行外交(0, self.外交索引 + 1, self.外交目标)
                self.子菜单索引 = 0
                self.菜单状态 = 1
                self.游戏状态 = self.选择格子(self.键码, self.键状态)
            elif self.菜单状态 == 9:
                self.更新部队动画()
                self.处理菜单输入()
            elif self.菜单状态 == 10:
                屏幕.保存游戏()
                self.结束回合()
            elif self.菜单状态 == 11:
                self.处理关卡选择()
        if self.需要重绘:
            屏幕.重绘屏幕()
        if self.对话框高度 == 0 or self.回合计数 < 0:
            return False
        self.对话框宽度 = 0
        self.对话框高度 = 0
        return False

    # ---------- 处理对话框输入 ----------
    def 处理对话框输入(self):
        高 = self.字体.取高度() + 2
        可见行 = (self.游戏屏幕.屏幕高 - ((高 + 8) if self.外交动作 == 0 else 高 * 2)) // 高
        if self.回合计数 > 0:
            self.回合计数 -= 1
            return 0
        if self.对话框宽度 == 0:
            return 0
        结果 = 0
        if self.对话框宽度 == -7 or self.对话框宽度 == -6 or self.对话框宽度 == -5:
            self.对话框行数 = 0
            self.需要重绘 = True
            i2 = 2 if self.对话框宽度 == -7 else 1
            self.对话框宽度 = 0
            if self.外交动作 == 0:
                return 1
            return i2
        elif self.对话框宽度 == -2:
            if self.外交索引 + 可见行 < self.对话框行数:
                self.外交索引 += 1
                self.需要重绘 = True
        elif self.对话框宽度 == -1:
            if self.外交索引 > 0:
                self.外交索引 -= 1
                self.需要重绘 = True
        if self.回合计数 >= 0:
            return 0
        self.回合计数 = 4
        return 0

    # ---------- 更新部队动画 ----------
    def 更新部队动画(self):
        for i in range(self.城市信息索引):
            if self.部队们[i].更新移动动画():
                self.需要重绘 = True

    # ---------- 处理光标输入 ----------
    def 处理光标输入(self):
        self.更新动画计数()
        if self.回合计数 > 0:
            self.回合计数 -= 1
            return
        if self.对话框宽度 == 0:
            return
        # 原版：有按键待处理时，先滚动光标（方向键 -1/-2/-3/-4）
        self.键状态 = self.向下滚动(self.键状态, len(self.地图数据))
        self.键码 = self.向上滚动(self.键码, len(self.地图数据[0]))
        if self.对话框宽度 == -10 or self.对话框宽度 == -7 or self.对话框宽度 == -5:
            self.需要重绘 = True
            self.菜单状态 = 2
            self.对话框宽度 = 0
            if self.临时部队 is None:
                城 = self.取城市(self.取城市所属(self.键码, self.键状态), False)
                if 城 is not None:
                    self.选中城 = 城
                    self.城市数 = 0
                    if self.选中城.势力序号 == 0:
                        self.菜单状态 = 5
                    else:
                        self.菜单状态 = 6
                    self.菜单选项索引[0] = 8
                    self.菜单选项数 = 1
                    self.部队数 = 0
                    self.外交索引 = 0
                    self.外交目标 = 0
                    if self.取部队(-1, False) is not None:
                        for i in range(len(部队.兵种资源需求)):
                            if self.选中城.具备资源(i):
                                self.菜单选项索引[self.菜单选项数] = i
                                if self.选中城.建造成目标 == i:
                                    self.部队数 = self.菜单选项数
                                self.菜单选项数 += 1
                elif self.尝试建造(self.键码, self.键状态):
                    self.菜单状态 = 1
                else:
                    self.显示游戏菜单()
            elif self.临时部队.势力序号 != 0 or self.临时部队.攻击冷却 == 0:
                self.临时部队.显示部队信息()
                self.菜单状态 = 1
            elif not self.临时部队.是否死亡:
                if self.临时部队.更新移动(False):
                    self.菜单状态 = 3
                else:
                    self.临时部队.显示部队信息()
                    self.菜单状态 = 1
                self.选中部队 = self.临时部队
            elif self.显示对话框("指挥确认", "该民工队伍正在建造" + 数据表.建筑名[self.临时部队.移动目标x] + ",是否终止工作?", True, 1) != 1:
                self.菜单状态 = 1
            else:
                self.临时部队.清除死亡标记()
                if self.临时部队.更新移动(False):
                    self.菜单状态 = 3
                self.选中部队 = self.临时部队
        elif self.对话框宽度 == 49:
            self.选择下一城(True)
            self.对话框宽度 = 0
            self.需要重绘 = True
        elif self.对话框宽度 == 50:
            self.选择下一部队()
            self.对话框宽度 = 0
            self.需要重绘 = True
        elif self.对话框宽度 == 51:
            self.显示游戏菜单()
            self.对话框宽度 = 0
        elif self.对话框宽度 == 52:
            self.选择下一城(False)
            self.对话框宽度 = 0
            self.需要重绘 = True
        if self.需要重绘:
            self.游戏状态 = self.选择格子(self.键码, self.键状态)
            self.移动光标(self.键码, self.键状态)
        if self.回合计数 < 0:
            self.回合计数 = 4

    # ---------- 尝试建造 ----------
    def 尝试建造(self, x, y):
        self.菜单选项数 = 0
        if self.选中城 is None or self.选中城.势力序号 != 0:
            return False
        物体 = self.取物体类型(x, y)
        # 城市中心不是普通建筑格；施工覆盖它会让城市从地图上消失。
        if 物体 in (12, 50) or 数据表.资源索引(物体) >= 0:
            return False
        self.城市数 = 数据表.建筑索引(物体)
        self.选中部队 = self.查找空闲工人(self.选中城.归属id)
        if self.选中部队 is None:
            return False
        for i in range(len(数据表.建筑名)):
            if i != self.城市数 and self.选中城.属性[6] >= 数据表.建筑造价[i] // 5:
                self.菜单选项索引[self.菜单选项数] = i
                self.菜单选项数 += 1
        if self.菜单选项数 == 0:
            self.显示对话框("建造信息", "缺少足够的资金!", True, 0)
            return True
        self.菜单状态 = 7
        结果 = self.选择建筑()
        self.菜单状态 = 2
        if 结果 < 0:
            return True
        self.选中部队.移动到(x, y, self.游戏屏幕)
        self.选中部队.开始建造(self.菜单选项索引[结果], self.选中城)
        self.游戏状态 = self.选择格子(self.键码, self.键状态)
        return True

    # ---------- 选择建筑 ----------
    def 选择建筑(self):
        self.部队数 = 0
        self.需要重绘 = True
        while True:
            if self.回合计数 > 0:
                self.回合计数 -= 1
            elif self.对话框宽度 != 0:
                # 只有方向键和确认/取消键才会改变建筑选项。
                # 回车按键可能同时产生重复事件，处理后立即清空按键状态。
                self.部队数 = self.向下滚动(self.部队数, self.菜单选项数)
                if self.是确认键():
                    self.对话框宽度 = 0
                    return self.部队数
                if self.对话框宽度 == -7:
                    self.对话框宽度 = 0
                    return -1
                if self.回合计数 < 0:
                    self.回合计数 = 4
            if self.需要重绘:
                self.游戏屏幕.重绘屏幕()
            if self.对话框高度 != 0:
                self.对话框宽度 = 0
                self.对话框高度 = 0
            self.游戏屏幕.睡眠(50)

    # ---------- 处理部队移动 ----------
    def 处理部队移动(self):
        self.更新部队动画()
        if self.回合计数 > 0:
            self.回合计数 -= 1
            return
        if self.对话框宽度 == 0:
            return
        完成 = True
        if self.对话框宽度 == -4:
            完成 = not self.选中部队.移到方向(2, self.游戏屏幕)
        elif self.对话框宽度 == -3:
            完成 = not self.选中部队.移到方向(0, self.游戏屏幕)
        elif self.对话框宽度 == -2:
            完成 = not self.选中部队.移到方向(1, self.游戏屏幕)
        elif self.对话框宽度 == -1:
            完成 = not self.选中部队.移到方向(3, self.游戏屏幕)
        elif self.对话框宽度 == 53:
            self.临时部队.显示部队信息()
            self.需要重绘 = True
            return
        else:
            self.对话框宽度 = 0
        self.对话框宽度 = 0
        self.需要重绘 = True
        self.选中部队.更新移动(False)
        if self.处理部队回合(self.选中部队, 完成):
            self.菜单状态 = 1
            self.键码 = self.选中部队.地图x
            self.键状态 = self.选中部队.地图y
            self.游戏状态 = self.选择格子(self.键码, self.键状态)
        if self.回合计数 < 0:
            self.回合计数 = 4

    # ---------- 处理部队回合 ----------
    def 处理部队回合(self, 单位, z):
        if 单位.归属id <= 0 or 单位.攻击冷却 < 0:
            单位.攻击冷却 = 0
            return True
        if not z and 单位.攻击冷却 > 0:
            return False
        self.菜单状态 = 1
        单位.更新移动(True)
        if self.尝试建城(单位):
            return True
        if 单位.目标部队 is not None and ((self.显示对话框("指挥", "指挥弓兵射击敌人吗?", True, 1) == 1 or 战斗模式 or 单位.势力序号 != 0) and 单位.攻击目标(self.游戏屏幕)):
            return True
        x = 单位.地图x
        y = 单位.地图y
        城 = self.取城市(self.取归属id(x, y), False)
        建筑序号 = 数据表.建筑索引(self.取物体类型(x, y))
        if 城 is None or 建筑序号 < 0 or self.取外交值(单位.势力序号, 城.势力序号, 1) != 1:
            return True
        if not 战斗模式 and 单位.势力序号 == 0 and self.显示对话框("指挥确认", "此处有敌人的建筑，指挥军队破坏他们吗?", True, 1) != 1:
            return True
        self.设置物体类型(x, y, 0)
        if 城.建筑[建筑序号] > 0:
            城.建筑[建筑序号] = 城.建筑[建筑序号] - 1
        单位.攻击冷却 = 0
        return True

    # ---------- 尝试建城 ----------
    def 尝试建城(self, 单位):
        x = 单位.地图x
        y = 单位.地图y
        if 单位.兵种 != 4 or not self.可建城(x, y) or self.显示对话框("建造城市", "此地看来非常适合建造新的城镇，确认吗?", True, 1) != 1:
            return False
        城 = self.取城市(-1, True)
        城.初始化城市(self.城市名[城.归属id - 1], x, y, 1 - self.城市们[单位.归属id - 1].等级, 1, 单位.势力序号)
        城.扩张领域()
        self.设置城市格子(x, y, 城.归属id)
        单位.转为民工(城.归属id)
        return True

    # ---------- 可建城 ----------
    def 可建城(self, x, y):
        return self.取城市(-1, False) is not None and self.取归属id(x, y) == 0 and self.取地形(x, y) == 0

    # ---------- 处理菜单输入 ----------
    def 处理菜单输入(self):
        结果 = self.菜单.处理菜单()
        if 结果 != 0:
            self.需要重绘 = True
        if not self.菜单.是否可见:
            self.菜单状态 = 1
        if 结果 == 1:
            self.菜单状态 = 10
        elif 结果 == 2:
            global 战斗模式
            战斗模式 = not 战斗模式
        elif 结果 == 3:
            游戏界面.音效开关 = not 游戏界面.音效开关
        elif 结果 == 4:
            self.显示对话框(战役名[self.对话框结果], self.对话框行[0], False, 0)
            self.添加对话框行(self.对话框行[1], 11468800)

    # ---------- 处理关卡选择 ----------
    def 处理关卡选择(self):
        结果 = self.菜单.处理菜单()
        if 结果 == 0:
            return
        if 结果 >= 0:
            self.对话框结果 = 结果 - 1
            self.重置游戏状态()
        else:
            self.需要重绘 = True
            if self.菜单.是否可见:
                return
            self.游戏结束 = True

    # ---------- 处理城市菜单 ----------
    def 处理城市菜单(self):
        if self.回合计数 > 0:
            self.回合计数 -= 1
            return
        if self.对话框宽度 == 0:
            return
        self.城市数 = self.向上滚动(self.城市数, len(self.游戏数据_动作名))
        if self.需要重绘:
            if self.城市数 == 2:
                self.外交索引 = self.选中城.归属id - 1
            else:
                self.外交索引 = 0
        elif self.对话框宽度 == -7 and (self.城市数 == 2 or self.城市数 == 0):
            self.菜单状态 = 1
            self.游戏状态 = self.选择格子(self.键码, self.键状态)
            self.对话框宽度 = 0
        if self.城市数 == 0:
            self.处理生产()
        elif self.城市数 == 1:
            self.处理贸易()
        elif self.城市数 == 2:
            self.处理城市列表()
        elif self.城市数 == 3:
            self.处理外交菜单()
        if self.回合计数 < 0:
            self.回合计数 = 4

    游戏数据_动作名 = ["制造", "信息", "城市", "外交"]

    # ---------- 处理生产 ----------
    def 处理生产(self):
        if self.菜单状态 == 5:
            self.部队数 = self.向下滚动(self.部队数, self.菜单选项数)
            self.选中城.建造成目标 = self.菜单选项索引[self.部队数]
        if self.是确认键():
            目标 = self.选中城.建造成目标
            if 目标 == len(部队.兵种名) or self.选中城.势力序号 != 0 or (部队.兵种属性[目标][4] - self.选中城.生产进度) * 5 > self.选中城.属性[6] or (部队.兵种属性[目标][4] - self.选中城.生产进度) * 5 <= 0:
                self.菜单状态 = 1
                self.游戏状态 = self.选择格子(self.键码, self.键状态)
            else:
                self.选中城.生产进度 = 部队.兵种属性[目标][4]
                属性 = self.选中城.属性
                属性[6] = 属性[6] - ((部队.兵种属性[目标][4] - self.选中城.生产进度) * 5)
                self.需要重绘 = True

    # ---------- 调整物价 ----------
    def 调整物价(self, i):
        self.视野倍率 += i
        if self.视野倍率 < 1:
            self.视野倍率 = 1
        if self.视野倍率 > 50:
            self.视野倍率 = 50

    # ---------- 处理贸易 ----------
    def 处理贸易(self):
        self.外交索引 = self.向下滚动(self.外交索引, len(self.选中城.属性) + len(self.选中城.建筑))
        if self.是确认键():
            if self.选中城.势力序号 != 0:
                self.菜单状态 = 1
                self.游戏状态 = self.选择格子(self.键码, self.键状态)
                return
            else:
                if self.选中城.属性[3] < 100:
                    return
                属性 = self.选中城.属性
                属性[3] = 属性[3] - 100
                属性 = self.选中城.属性
                属性[6] = 属性[6] + ((self.视野倍率 * 95) // 100)
                self.调整物价(-1)
                self.需要重绘 = True
                return
        if self.对话框宽度 == -7:
            if self.选中城.势力序号 != 0:
                self.菜单状态 = 1
                self.游戏状态 = self.选择格子(self.键码, self.键状态)
            else:
                if self.选中城.属性[6] < self.视野倍率:
                    return
                属性 = self.选中城.属性
                属性[3] = 属性[3] + 95
                属性 = self.选中城.属性
                属性[6] = 属性[6] - self.视野倍率
                self.调整物价(1)
                self.需要重绘 = True

    # ---------- 处理城市列表 ----------
    def 处理城市列表(self):
        self.外交索引 = self.向下滚动(self.外交索引, self.城市菜单索引)
        if self.是确认键():
            self.选中城 = self.城市们[self.外交索引]
            self.键码 = self.选中城.x
            self.键状态 = self.选中城.y
            self.菜单状态 = 1
            self.游戏状态 = self.选择格子(self.键码, self.键状态)
            self.需要重绘 = True

    # ---------- 处理外交菜单 ----------
    def 处理外交菜单(self):
        """按原版 Nokia 操作处理外交页签。

        上/下：选择国家；右软键：切换外交行动；左软键/确定：执行行动。
        左/右方向键由外层城市菜单用于切换页签。
        """
        可选国家数 = max(1, len(self.外交数据[0]) - 1) if self.外交数据 else 1
        if self.对话框宽度 == 输入.键下:
            self.外交索引 = (self.外交索引 + 1) % 可选国家数
            self.对话框宽度 = 0
            self.需要重绘 = True
            return
        if self.对话框宽度 == 输入.键上:
            self.外交索引 = (self.外交索引 - 1) % 可选国家数
            self.对话框宽度 = 0
            self.需要重绘 = True
            return
        if self.对话框宽度 == 输入.键右软键:
            # 原版 processDiplomacyMenu：右软键循环切换外交行动。
            self.外交目标 = (self.外交目标 + 1) % len(外交动作)
            self.对话框宽度 = 0
            self.需要重绘 = True
            return
        if self.对话框宽度 == 输入.键左软键 or self.对话框宽度 == 输入.键确定:
            目标势力 = self.外交索引 + 1
            if self.可执行外交(0, 目标势力, self.外交目标, self.选中城):
                # 原版先进入状态8，下一次游戏更新才执行外交并弹出使者结果。
                self.子菜单索引 = 1
                self.菜单状态 = 8
                self.对话框宽度 = 0
                self.需要重绘 = True
            else:
                self.显示对话框("外交信息", self.外交不可用原因(目标势力, self.外交目标), True, 0)
                self.对话框宽度 = 0
            return
        self.对话框宽度 = 0

    # ---------- 可执行外交 ----------
    def 可执行外交(self, i, i2, i3, 城):
        if 城 is None or i2 < 0 or i2 >= len(self.外交数据) or i3 < 0 or i3 >= len(外交动作):
            return False
        对方关系 = self.取外交值(i2, i, 1)
        if i3 == 0:
            return i2 != i and 城.属性[6] >= 100
        if i3 == 1:
            return i2 != i and 对方关系 == 0 and self.取外交值(i, i2, 1) == 0
        if i3 == 2:
            return i2 != i and 对方关系 in (1, 2)
        if i3 == 3:
            return i2 != i and 对方关系 == 2
        if i3 == 4:
            return i2 != i and 对方关系 != 1
        if i3 == 5:
            return i2 != i and 对方关系 == 1
        return False

    def 外交不可用原因(self, 目标势力, 行动):
        if 行动 == 0:
            return "送礼需要至少100金。"
        if 行动 == 1:
            return "只有关系正常且尚未结盟时才能尝试结盟。"
        if 行动 == 2:
            return "只有战争或同盟关系下才能勒索。"
        if 行动 == 3:
            return "当前并未与该势力结盟。"
        if 行动 == 4:
            return "当前已经处于战争状态。"
        if 行动 == 5:
            return "当前没有与该势力交战。"
        return "当前外交行动不可用。"

    # ---------- 执行外交 ----------
    def 执行外交(self, i, i2, i3):
        if i3 == 0:
            self.执行送礼(i, i2)
        elif i3 == 1:
            self.执行结盟(i, i2)
        elif i3 == 2:
            self.执行勒索(i, i2)
        elif i3 == 3:
            self.执行毁约(i, i2)
        elif i3 == 4:
            self.执行宣战(i, i2)
        elif i3 == 5:
            self.执行停战(i, i2)

    # ---------- 送礼 ----------
    def 执行送礼(self, i, i2):
        成功 = self.可送礼(i, i2)
        if 成功:
            self.调整关系(i2, i, 3)
            self.扣除金币(i, i2, 100)
        else:
            self.调整关系(i, i2, -4)
        if i == 0:
            参数 = [势力名[i2], 外交动作[0]]
            self.显示对话框(菜单标签[6], 格式化消息(使者结果[0 if 成功 else 1], 参数), True, 0)
        self.子菜单索引 = 0
        self.菜单状态 = 5
        self.需要重绘 = True

    def 可送礼(self, i, i2):
        if i2 == 0:
            return self.显示对话框(菜单标签[6], 格式化消息(外交消息[0], [势力名[i]]), True, 2) == 1
        return self.随机数(self.取外交值(i2, i, 0) + 50) > 40

    # ---------- 结盟 ----------
    def 执行结盟(self, i, i2):
        成功 = self.可结盟(i, i2)
        if 成功:
            self.设置外交值(i, i2, 0, 100)
            self.设置外交值(i2, i, 0, 100)
            self.设置外交值(i, i2, 1, 2)
            self.设置外交值(i2, i, 1, 2)
            self.传播关系(i, i2, 5)
        else:
            self.调整关系(i, i2, -2)
        if i == 0:
            参数 = [势力名[i2], 外交动作[1]]
            self.显示对话框(菜单标签[6], 格式化消息(使者结果[0 if 成功 else 1], 参数), True, 0)
        self.子菜单索引 = 0
        self.菜单状态 = 5
        self.需要重绘 = True

    def 可结盟(self, i, i2):
        if i2 == 0:
            return self.显示对话框(菜单标签[6], 格式化消息(外交消息[1], [势力名[i]]), True, 2) == 1
        if self.随机数(self.取外交值(i2, i, 0)) < 95:
            return False
        my = self.计算势力兵力(i)
        their = self.计算势力兵力(i2)
        return my > their + self.随机数(their)

    # ---------- 勒索 ----------
    def 执行勒索(self, i, i2):
        成功 = self.可勒索(i, i2)
        if 成功:
            self.扣除金币(i2, i, 100)
            self.调整关系(i2, i, -3)
            self.传播关系(i, i2, -1)
        else:
            self.调整关系(i, i2, -5)
            self.调整关系(i2, i, -2)
        if i == 0:
            参数 = [势力名[i2], 外交动作[2]]
            self.显示对话框(菜单标签[6], 格式化消息(使者结果[0 if 成功 else 1], 参数), True, 0)
        self.子菜单索引 = 0
        self.菜单状态 = 5
        self.需要重绘 = True

    def 可勒索(self, i, i2):
        if i2 == 0:
            return self.显示对话框(菜单标签[6], 格式化消息(外交消息[2], [势力名[i]]), True, 2) == 1
        关系 = self.取外交值(i2, i, 0)
        my = self.计算势力兵力(i)
        their = self.计算势力兵力(i2)
        return my > their + ((self.随机数(110 - 关系) * their) // 100)

    # ---------- 毁约 ----------
    def 执行毁约(self, i, i2):
        if i2 == 0:
            self.显示对话框(菜单标签[6], 格式化消息(外交消息[3], [势力名[i]]), True, 0)
        self.设置外交值(i, i2, 1, 0)
        self.设置外交值(i2, i, 1, 0)
        self.调整关系(i2, i, -20)
        self.传播关系(i, i2, -10)
        if i == 0:
            self.显示对话框("外交信息", "我们与" + 势力名[i2] + "解除了同盟。", True, 0)
        self.子菜单索引 = 0
        self.菜单状态 = 5
        self.需要重绘 = True

    # ---------- 宣战 ----------
    def 执行宣战(self, i, i2):
        self.宣战(i, i2)
        self.设置外交值(i, i2, 1, 1)
        self.设置外交值(i2, i, 1, 1)
        self.调整关系(i2, i, -50)
        self.传播关系(i, i2, -20)
        for i3 in range(len(self.外交数据)):
            if i3 != i and i3 != i2:
                if self.取外交值(i3, i, 1) == 2:
                    self.宣战(i3, i2)
                    self.设置外交值(i2, i3, 1, 1)
                    self.设置外交值(i3, i2, 1, 1)
                if self.取外交值(i3, i2, 1) == 2:
                    self.宣战(i3, i)
                    self.设置外交值(i2, i3, 1, 1)
                    self.设置外交值(i3, i2, 1, 1)
        if i == 0:
            self.显示对话框("战争", "我们对" + 势力名[i2] + "宣战了！", True, 0)
        self.子菜单索引 = 0
        self.菜单状态 = 5
        self.需要重绘 = True

    def 宣战(self, i, i2):
        if i2 != 0:
            return True
        self.显示对话框(菜单标签[6], 格式化消息(外交消息[4], [势力名[i]]), True, 0)
        return True

    # ---------- 停战 ----------
    def 执行停战(self, i, i2):
        成功 = self.可停战(i, i2)
        if 成功:
            self.设置外交值(i, i2, 1, 0)
            self.设置外交值(i2, i, 1, 0)
            self.调整关系(i, i2, 10)
            self.调整关系(i2, i, 5)
            self.传播关系(i, i2, 2)
        else:
            self.调整关系(i, i2, -5)
        if i == 0:
            参数 = [势力名[i2], 外交动作[5]]
            self.显示对话框(菜单标签[6], 格式化消息(使者结果[0 if 成功 else 1], 参数), True, 0)
        self.子菜单索引 = 0
        self.菜单状态 = 5
        self.需要重绘 = True

    def 可停战(self, i, i2):
        if i2 == 0:
            return self.显示对话框(菜单标签[6], 格式化消息(外交消息[5], [势力名[i]]), True, 2) == 1
        return self.随机数(self.计算势力兵力(i)) > (self.计算势力兵力(i2) * 2) // 3

    # ---------- 辅助：扣除/调整/传播 ----------
    def 扣除金币(self, i, i2, i3):
        if i != 0 or self.选中城 is None:
            城 = self.查找势力城市(i)
            if 城 is not None:
                城.属性[6] = 城.属性[6] - i3
        else:
            self.选中城.属性[6] = self.选中城.属性[6] - i3
        城2 = self.查找势力城市(i2)
        if 城2 is not None:
            城2.属性[6] = 城2.属性[6] - i3

    def 调整关系(self, i, i2, i3):
        关系 = self.取外交值(i, i2, 0) + i3
        if 关系 < 0:
            关系 = 0
        elif 关系 > 100:
            关系 = 100
        self.设置外交值(i, i2, 0, 关系)

    def 传播关系(self, i, i2, i3):
        for i4 in range(len(self.外交数据)):
            if i4 != i and i4 != i2:
                关系 = self.取外交值(i4, i2, 0)
                状态 = self.取外交值(i4, i2, 1)
                if 状态 == 1:
                    self.调整关系(i, i4, -i3)
                elif 状态 == 2:
                    self.调整关系(i, i4, i3)
                elif 关系 > 95:
                    self.调整关系(i, i4, i3)
                elif 关系 < 15:
                    self.调整关系(i, i4, -i3)

    # ---------- 找最佳盟友 ----------
    def 找最佳盟友(self, i):
        最优 = 100
        结果 = 0
        for i4 in range(len(self.外交数据)):
            if i4 != i:
                if self.取外交值(i, i4, 1) == 1:
                    return i4
                关系 = self.取外交值(i, i4, 0)
                if 结果 == 0 or 最优 > 关系:
                    结果 = i4
                    最优 = 关系
        return 结果

    # ---------- 找AI目标 ----------
    def 找目标(self, 单位, 位置):
        位置[0] = 单位.地图x
        位置[1] = 单位.地图y
        盟友 = self.找最佳盟友(单位.势力序号)
        if self.取外交值(单位.势力序号, 盟友, 1) != 1 and self.随机数(10) != 2 and self.取城市所属(单位.地图x, 单位.地图y) == 0:
            return False
        最近 = -1
        for i in range(self.城市信息索引):
            其他 = self.部队们[i]
            if 其他.归属id > 0 and 其他.势力序号 == 盟友 and 其他 is not 单位:
                距离 = abs(其他.地图x - 单位.地图x) + abs(其他.地图y - 单位.地图y)
                if 最近 < 0 or 最近 > 距离:
                    位置[0] = 其他.地图x
                    位置[1] = 其他.地图y
                    最近 = 距离
        for i2 in range(self.城市菜单索引):
            城 = self.城市们[i2]
            if 城.科技等级 > 0 and 城.势力序号 == 盟友:
                距离 = abs(城.x - 单位.地图x) + abs(城.y - 单位.地图y)
                if 最近 < 0 or 最近 > 距离:
                    位置[0] = 城.x
                    位置[1] = 城.y
                    最近 = 距离
        return True

    # ---------- 结束回合 ----------
    def 结束回合(self):
        self.调整物价(self.随机数(10) % 10 if self.随机数(2) == 0 else -(self.随机数(10) % 10))
        if self.检查任务完成():
            if self.游戏结束:
                return
            self.显示对话框("胜利", "你胜利完成了任务!", True, 0)
            self.初始化关卡(False)
            return
        for i in range(self.城市菜单索引):
            城 = self.城市们[i]
            if 城.科技等级 > 0:
                城.更新城市(self.游戏屏幕)
        for i2 in range(1, len(self.外交数据)):
            if self.统计城市数(i2) != 0:
                关系 = self.取外交值(i2, 0, 0)
                if self.随机数(80) > 关系:
                    状态 = self.取外交值(i2, 0, 1)
                    if 状态 == 2:
                        self.执行外交(i2, 0, 3)
                    else:
                        随机 = self.随机数(关系 + 1)
                        盟友 = self.找最佳盟友(i2)
                        if 状态 != 1:
                            if 随机 >= 4 or 关系 >= 30:
                                if 随机 < 15:
                                    self.执行外交(i2, 0, 2)
                            elif 盟友 == 0:
                                if self.随机数(3) == 1:
                                    self.执行外交(i2, 0, 4)
                            elif self.随机数(10) == 1:
                                self.执行外交(i2, 0, 4)
                else:
                    if 关系 > 0:
                        关系 -= 1
                    self.设置外交值(i2, 0, 0, 关系)
        for i3 in range(self.城市信息索引):
            单位 = self.部队们[i3]
            if 单位.归属id > 0:
                self.移动光标(单位.地图x, 单位.地图y)
                单位.更新部队(self.游戏屏幕)
                if 单位.兵种 != 3 and 单位.兵种 != 4 and ((单位.势力序号 != 0 or 战斗模式) and self.找目标(单位, self.光标位置)):
                    单位.移到目标(self.光标位置[0], self.光标位置[1], self.游戏屏幕)
        for i4 in range(self.城市信息索引):
            单位 = self.部队们[i4]
            单位.攻击冷却 = 部队.兵种属性[单位.兵种][2]
        for yy in range(len(self.地图数据)):
            for xx in range(len(self.地图数据[yy])):
                self.地图数据[yy][xx] = self.地图数据[yy][xx] & 0x7FFFFFFF
        self.菜单状态 = 1
        self.子菜单索引 = 0
        self.移动光标(self.键码, self.键状态)
        self.游戏状态 = self.选择格子(self.键码, self.键状态)

    # ---------- 检查任务完成 ----------
    def 检查任务完成(self):
        if self.所有敌人被消灭():
            return True
        if self.玩家被击败():
            self.显示对话框("失败", "失去了所有的城镇，你失败了!", True, 0)
            self.游戏结束 = True
            return True
        条件 = 任务条件[self.对话框结果]
        完成 = len(条件) > 0
        for i in range(0, len(条件), 3):
            类型 = 条件[i]
            if 类型 == 0:
                if self.统计势力数据(0, 条件[i + 1]) < 条件[i + 2]:
                    完成 = False
            elif 类型 == 1:
                if self.按类型统计部队(0, 条件[i + 1]) < 条件[i + 2]:
                    完成 = False
            elif 类型 == 2:
                势力 = 条件[i + 1]
                if self.统计城市数(势力) == 0:
                    self.显示对话框("失败", str(势力名[势力]) + "被消灭了，任务失败!", True, 0)
                    self.游戏结束 = True
                    return True
                if self.取外交值(0, 条件[1], 1) != 条件[i + 2]:
                    完成 = False
        return 完成

    def 所有敌人被消灭(self):
        for i in range(self.城市菜单索引):
            if self.城市们[i].科技等级 > 0 and self.城市们[i].势力序号 != 0:
                return False
        return True

    def 玩家被击败(self):
        for i in range(self.城市菜单索引):
            if self.城市们[i].科技等级 > 0 and self.城市们[i].势力序号 == 0:
                return False
        return True

    def 统计势力数据(self, i, i2):
        总 = 0
        for i4 in range(self.城市菜单索引):
            城 = self.城市们[i4]
            if 城.科技等级 > 0 and 城.势力序号 == i:
                if i2 == 0:
                    总 += 1
                elif i2 > 0:
                    总 += 城.属性[i2 - 1]
                else:
                    总 += 城.资源[(-i2) - 1]
        return 总

    def 统计城市数(self, i):
        数 = 0
        for i3 in range(self.城市菜单索引):
            if self.城市们[i3].科技等级 > 0 and self.城市们[i3].势力序号 == i:
                数 += 1
        return 数

    def 按类型统计部队(self, i, i2):
        数 = 0
        for i4 in range(self.城市信息索引):
            单位 = self.部队们[i4]
            if 单位.归属id > 0 and 单位.势力序号 == i:
                if i2 < 0:
                    if 单位.兵种 != 3 and 单位.兵种 != 4:
                        数 += 1
                elif i2 == 单位.兵种 or i2 == 单位.兵种 - 5:
                    数 += 1
        return 数

    def 按归属统计部队(self, i, i2):
        数 = 0
        for i4 in range(self.城市信息索引):
            单位 = self.部队们[i4]
            if 单位.归属id > 0 and 单位.归属id == i:
                if i2 < 0:
                    数 += 1
                elif 单位.兵种 == i2:
                    数 += 1
        return 数

    # ---------- 计算部队维护费 ----------
    def 计算部队维护(self, i, 增量):
        for i2 in range(self.城市信息索引):
            if self.部队们[i2].归属id > 0 and self.部队们[i2].归属id == i:
                增量[3] = 增量[3] - (self.部队们[i2].最大兵力 // 100)
                增量[6] = 增量[6] - (self.部队们[i2].最大兵力 // 400)

    # ---------- 计算势力实力 ----------
    def 计算势力兵力(self, i):
        总 = 0
        for i2 in range(self.城市菜单索引):
            城 = self.城市们[i2]
            if 城.科技等级 > 0 and 城.势力序号 == i:
                总 += 城.计算城市价值()
        for i3 in range(self.城市信息索引):
            if self.部队们[i3].归属id > 0 and self.部队们[i3].势力序号 == i:
                总 += self.部队们[i3].计算攻击()
        return 总

    # ---------- 销毁势力所属部队 ----------
    def 销毁归属部队(self, i):
        for i2 in range(self.城市信息索引):
            单位 = self.部队们[i2]
            if 单位.归属id > 0 and 单位.归属id == i:
                self.移动光标(单位.地图x, 单位.地图y)
                单位.阵亡(self.游戏屏幕)
        self.清理死亡部队()

    # ---------- 清理死亡部队 ----------
    def 清理死亡部队(self):
        while self.城市信息索引 > 0 and self.部队们[self.城市信息索引 - 1].归属id <= 0:
            self.城市信息索引 -= 1

    # ==================== 地图查询/操作原语 ====================
    def 是水(self, x, y):
        return (self.地图数据[y][x] & 255) != 0

    def 取地形(self, x, y):
        return self.地图数据[y][x] & 255

    def 取物体类型(self, x, y):
        return (self.地图数据[y][x] >> 8) & 255

    def 取城市所属(self, x, y):
        if self.取物体类型(x, y) != 12:
            return 0
        return (self.地图数据[y][x] >> 16) & 255

    def 取归属id(self, x, y):
        return (self.地图数据[y][x] >> 16) & 255

    def 取部队id(self, x, y):
        return (self.地图数据[y][x] >> 24) & 127

    def 设置部队id(self, x, y, i3):
        self.地图数据[y][x] = (self.地图数据[y][x] & 0xFFFFFF) | (i3 << 24)

    def 设置归属id(self, x, y, i3):
        self.地图数据[y][x] = (self.地图数据[y][x] & 0xFF00FFFF) | (i3 << 16)

    def 设置城市格子(self, x, y, i3):
        # 替换归属和物体字段，保留地形及高字节中的部队 ID/临时标记。
        self.地图数据[y][x] = (self.地图数据[y][x] & 0xFF0000FF) | (i3 << 16) | (12 << 8)

    def 设置物体类型(self, x, y, i3):
        self.地图数据[y][x] = (self.地图数据[y][x] & 0xFFFF00FF) | (i3 << 8)

    def 越界(self, x, y):
        return x < 0 or y < 0 or y >= len(self.地图数据) or x >= len(self.地图数据[y])

    def 可通行(self, x, y, 势力序号):
        if self.越界(x, y):
            return False
        a单位 = self.取部队(self.取部队id(x, y), False)
        if a单位 is not None and (a单位.势力序号 == 势力序号 or self.取外交值(势力序号, a单位.势力序号, 1) != 1):
            return False
        城 = self.取城市(self.取城市所属(x, y), False)
        return 城 is None or 城.势力序号 == 势力序号 or self.取外交值(势力序号, 城.势力序号, 1) == 1

    def 选择格子(self, x, y):
        if self.临时部队 is not None:
            self.临时部队 = None
        result = 0
        self.临时部队 = self.取部队(self.取部队id(x, y), False)
        if self.临时部队 is None:
            归属 = self.取城市所属(x, y)
            if 归属 != 0:
                self.选中城 = self.取城市(归属, False)
                if self.选中城 is None:
                    return 0
                return self.选中城.归属id
            result = self.取归属id(x, y)
            self.选中城 = self.取城市(result, False)
        elif self.临时部队.攻击冷却 == 0:
            return 0
        return result

    # ==================== 取城市/取部队 ====================
    def 取城市(self, i, 新建):
        if i >= 0:
            if i <= 0 or i > len(self.城市们):
                return None
            城 = self.城市们[i - 1]
            if 城.科技等级 <= 0:
                return None
            return 城
        for i2 in range(len(self.城市名)):
            if self.城市们[i2].科技等级 <= 0:
                if 新建 and self.城市菜单索引 < i2 + 1:
                    self.城市菜单索引 = i2 + 1
                return self.城市们[i2]
        return None

    def 取部队(self, i, 新建):
        if i >= 0:
            if i <= 0 or i > len(self.部队们):
                return None
            单位 = self.部队们[i - 1]
            if 单位.归属id <= 0:
                return None
            return 单位
        for i2 in range(len(self.部队们)):
            if self.部队们[i2].归属id <= 0:
                if 新建 and self.城市信息索引 < i2 + 1:
                    self.城市信息索引 = i2 + 1
                return self.部队们[i2]
        return None

    def 查找归属部队(self, i):
        for i2 in range(self.城市信息索引):
            if self.部队们[i2].归属id > 0 and self.部队们[i2].归属id == i:
                return self.部队们[i2]
        return None

    def 查找空闲工人(self, i):
        for i2 in range(self.城市信息索引):
            单位 = self.部队们[i2]
            if 单位.归属id > 0 and 单位.归属id == i and 单位.兵种 == 3 and not 单位.是否死亡 and 单位.攻击冷却 != 0:
                return 单位
        return None

    def 查找势力城市(self, i):
        for i2 in range(self.城市菜单索引):
            if self.城市们[i2].科技等级 > 0 and self.城市们[i2].势力序号 == i:
                return self.城市们[i2]
        return None

    # ==================== 随机数 ====================
    def 随机数(self, i):
        值 = self.随机源.getrandbits(32)
        值 = 值 & 0x7FFFFFFF
        if i > 0:
            值 = 值 % i
        return 值

    # ==================== 光标 ====================
    def 取光标屏幕位置(self, x, y, 输出, 加上偏移):
        输出[0] = 22 * (x + y + 1)
        输出[1] = (11 * ((len(self.地图数据[0]) - x) + y)) - 1
        if 加上偏移:
            输出[0] = 输出[0] + self.光标x
            输出[1] = 输出[1] + self.光标y

    def 移动光标(self, x, y):
        旧x = self.光标x
        旧y = self.光标y
        self.取光标屏幕位置(x, y, self.光标位置, False)
        屏幕x = self.光标位置[0]
        屏幕y = self.光标位置[1]
        self.光标x = (游戏界面.屏幕宽 >> 1) - 屏幕x
        self.光标y = ((self.信息栏高度 + self.屏幕偏移y) >> 1) - 屏幕y
        if self.光标x > 0:
            self.光标x = 0
        if self.光标y > self.屏幕偏移y:
            self.光标y = self.屏幕偏移y
        if 旧x == self.光标x and 旧y == self.光标y:
            return False
        self.需要重绘 = True
        return True

    # ==================== 滚动/确认 ====================
    def 向下滚动(self, i, i2):
        if self.对话框宽度 == -2:
            i = i + 1 if i + 1 < i2 else 0
            self.需要重绘 = True
        elif self.对话框宽度 == -1:
            i = i - 1 if i > 0 else i2 - 1
            self.需要重绘 = True
        return i

    def 向上滚动(self, i, i2):
        if self.对话框宽度 == -4:
            i = i + 1 if i + 1 < i2 else 0
            self.需要重绘 = True
        elif self.对话框宽度 == -3:
            i = i - 1 if i > 0 else i2 - 1
            self.需要重绘 = True
        return i

    def 是确认键(self):
        if self.对话框宽度 == -10 or self.对话框宽度 == -6 or self.对话框宽度 == -5:
            self.对话框宽度 = 0
            return True
        return False

    # ==================== 是否繁忙 ====================
    def 是否繁忙(self):
        if self.对话框行数 > 0:
            return True
        if self.游戏结束 or self.菜单状态 == 3 or self.菜单状态 == 1:
            return False
        return True

    # ==================== 显示对话框 ====================
    def 显示对话框(self, 标题, 内容, 阻塞, 类型):
        self.播放声音()
        宽度 = 游戏界面.屏幕宽 - 24
        if 宽度 > 140:
            宽度 = 140
        self.外交动作 = 类型
        self.对话框行数 = 0
        self.外交索引 = 0
        self.任务标题 = 标题
        self.对话框屏幕宽 = 宽度
        if 类型 == 0:
            self.对话框屏幕高 = self.字体.取高度() + 16
        else:
            self.对话框屏幕高 = (self.字体.取高度() * 2) + 16
        self.添加对话框行(内容, 9321)
        结果 = 0
        if 阻塞:
            self.游戏屏幕.重绘屏幕()
            计数 = 0
            while True:
                结果 = self.处理对话框输入()
                if 结果 != 0:
                    break
                if self.需要重绘:
                    self.游戏屏幕.重绘屏幕()
                self.游戏屏幕.睡眠(50)
                计数 += 1
                if 计数 > 50 and self.外交动作 == 0:
                    break
            self.对话框行数 = 0
        return 结果

    # ---------- 添加对话框行 ----------
    def 添加对话框行(self, 文本, 颜色):
        if 文本 is None or len(文本) <= 0 or self.对话框行数 > len(self.对话框文本):
            return
        行高 = self.字体.取高度() + 2
        可用宽 = self.对话框屏幕宽 - 4
        while len(文本) > 0 and self.对话框行数 < len(self.对话框文本):
            self.对话框文本[self.对话框行数] = ""
            self.对话框颜色[self.对话框行数] = 颜色
            当前宽 = 0
            索引 = 0
            while True:
                if 当前宽 >= 可用宽 or 索引 >= len(文本):
                    break
                字符 = 文本[索引]
                if 字符 == "|":
                    索引 += 1
                    break
                字宽 = self.字体.字符宽度(字符)
                if 当前宽 + 字宽 > 可用宽:
                    break
                当前宽 += 字宽
                索引 += 1
                self.对话框文本[self.对话框行数] += 字符
            文本 = 文本[索引:]
            self.对话框行数 += 1
            self.对话框屏幕高 += 行高
        if self.对话框屏幕高 > self.信息栏高度 - 10:
            self.对话框屏幕高 = self.信息栏高度 - 10

    # ==================== 绘制精灵 ====================
    def 绘制精灵(self, 图形对象, x, y, 序号, 输出, 应用偏移):
        if 序号 < 0 or 序号 >= len(精灵帧):
            return 0
        裁剪上 = self.屏幕偏移y if 应用偏移 else 0
        裁剪下 = self.信息栏高度 if 应用偏移 else 游戏界面.屏幕高
        帧 = 精灵帧[序号]
        if len(帧) < 4 or self.对象图像 is None:
            return 0
        中心x = 帧[4] if len(帧) > 4 else 帧[2] >> 1
        中心y = 帧[5] if len(帧) > 5 else 帧[3] >> 1
        顶部y = y - 中心y
        左侧x = x - 中心x
        宽 = 帧[2]
        高 = 帧[3]
        裁剪宽 = 宽 if 左侧x + 宽 <= 游戏界面.屏幕宽 else 游戏界面.屏幕宽 - 左侧x
        裁剪高 = 高 if 顶部y + 高 <= 裁剪下 else 裁剪下 - 顶部y
        if 左侧x >= 游戏界面.屏幕宽 or 顶部y >= 裁剪下 or 左侧x + 裁剪宽 <= 0 or 顶部y + 裁剪高 <= 裁剪上:
            return 0
        if 顶部y >= 裁剪上:
            图形对象.设置裁剪(左侧x, 顶部y, 裁剪宽, 裁剪高)
        else:
            图形对象.设置裁剪(左侧x, 裁剪上, 裁剪宽, 裁剪高 + (顶部y - 裁剪上))
        # blit 源图
        对象图像 = self.对象图像
        src_rect = pygame.Rect(帧[0], 帧[1], 帧[2], 帧[3])
        图形对象.表面.blit(对象图像, (左侧x, 顶部y), src_rect)
        图形对象.设置裁剪(0, 0, 游戏界面.屏幕宽, 游戏界面.屏幕高)
        if 输出 is not None:
            输出[0] = 左侧x
            输出[1] = 顶部y
            输出[2] = 帧[2]
            输出[3] = 帧[3]
        return 帧[2]

    # ==================== 描述 ====================
    def 取格子描述(self, i, 详细信息):
        text = ""
        资源序号 = 数据表.资源索引(i)
        if 资源序号 < 0:
            建筑序号 = 数据表.建筑索引(i)
            if 建筑序号 < 0:
                if i == 1 or i == 2:
                    text = "树木"
                elif i == 3:
                    text = "巨石"
                elif i == 4:
                    text = "山丘"
                elif i == 5:
                    text = "树桩"
                elif i == 50:
                    text = "工地"
            else:
                text = 数据表.建筑名[建筑序号]
                if 详细信息:
                    描述 = text + "("
                    效果 = 数据表.建筑效果[建筑序号]
                    for i2 in range(0, len(效果), 2):
                        if i2 != 0:
                            描述 += ","
                        if 效果[i2 + 1] > 0:
                            描述 += "+"
                        描述 += str(效果[i2 + 1]) + 数据表.城市属性名[效果[i2]]
                    text = 描述 + ")"
        else:
            text = 数据表.特产名[资源序号]
            if 详细信息:
                描述 = text + "(+" + str(数据表.特产消耗[资源序号]) + "金"
                if 资源序号 == 3:
                    text = 描述 + ",医疗)"
                else:
                    for i3 in range(len(部队.兵种资源需求)):
                        if ((部队.兵种资源需求[i3] >> 资源序号) & 1) != 0:
                            描述 += "," + 部队.兵种名[i3]
                    text = 描述 + ")"
        return text

    # ==================== 绘制主入口 ====================
    def 绘制(self, 图形对象):
        图形对象.设置颜色(0)
        图形对象.填充矩形(0, 0, 游戏界面.屏幕宽, 游戏界面.屏幕高)
        # 关卡选择菜单显示在地图初始化之前；此时地图数据为空，不能进入地图绘制流程。
        # 与原版 Game.draw 的 menuState == 11 分支保持一致。
        if self.菜单状态 == 11:
            游戏界面.绘制文本("选择起始关:", 2, 2, 图形对象, 16777184, 0)
            self.菜单.绘制(图形对象)
            self.游戏屏幕.绘制瓦片(图形对象, 1, 游戏界面.屏幕高 - 10, 0)
            self.游戏屏幕.绘制瓦片(图形对象, 游戏界面.屏幕宽 - 10, 游戏界面.屏幕高 - 10, 1)
            self.需要重绘 = False
            return
        if not self.菜单状态 in (5, 6):
            if 图形对象 is not None:
                self.绘制地图(图形对象, self.光标x, self.光标y, self.地图数据)
                self.绘制地图覆盖(图形对象, self.光标x, self.光标y, self.地图数据)
                self.绘制状态栏(图形对象)
        if self.对话框行数 > 0:
            self.绘制对话框(图形对象)
            self.需要重绘 = False
            return
        if self.菜单状态 == 1:
            self.取光标屏幕位置(self.键码, self.键状态, self.光标位置, True)
            for i in range(len(部队.箭头偏移)):
                i2 = 5 + (self.动画计数 % 2)
                self.绘制精灵(图形对象, self.光标位置[0] + ((部队.箭头偏移[i][0] * i2) // 6),
                               self.光标位置[1] + ((部队.箭头偏移[i][1] * i2) // 6),
                               61 + i, None, True)
        elif self.菜单状态 == 3:
            self.游戏屏幕.绘制瓦片(图形对象, 游戏界面.屏幕宽 - 10, 游戏界面.屏幕高 - 10, 3)
            self.选中部队.绘制箭头(图形对象)
        elif self.菜单状态 == 5 or self.菜单状态 == 6:
            self.绘制城市面板(图形对象)
        elif self.菜单状态 == 7:
            self.绘制建造菜单(图形对象)
        elif self.菜单状态 == 9:
            self.菜单.绘制(图形对象)
            self.游戏屏幕.绘制瓦片(图形对象, 1, 游戏界面.屏幕高 - 10, 0)
            self.游戏屏幕.绘制瓦片(图形对象, 游戏界面.屏幕宽 - 10, 游戏界面.屏幕高 - 10, 1)
        self.需要重绘 = False

    # ---------- 绘制状态栏 ----------
    def 绘制状态栏(self, 图形对象):
        高 = self.字体.取高度() + 6
        i = 游戏界面.屏幕高 - 高
        self.绘制面板(图形对象, 0, i, 游戏界面.屏幕宽, 高, True)
        城 = self.取城市(self.取城市所属(self.键码, self.键状态), False)
        if self.菜单状态 == 3 and self.选中部队 is not None:
            机动 = self.选中部队.攻击冷却
            if 机动 < 0:
                机动 = 0
            文本 = "机动:" + str(机动)
        elif 城 is not None:
            文本 = str(城.城名) + "(" + 势力名[城.势力序号] + ")" + str(城.属性[2] // 10000) + "万人"
        else:
            单位 = self.取部队(self.取部队id(self.键码, self.键状态), False)
            if 单位 is not None:
                城市名 = self.取城市(单位.归属id, False).城名
                前缀 = 部队.兵种名[单位.兵种] + "(" + 城市名 + ")"
                if 单位.是否死亡:
                    文本 = 前缀 + 数据表.建筑名[单位.移动目标x] + "(" + str(单位.取攻击回合数(单位.建造进度)) + ")"
                elif 单位.最大兵力 >= 10000:
                    文本 = 前缀 + str(单位.最大兵力 // 10000) + "万人"
                elif 单位.最大兵力 >= 1000:
                    文本 = 前缀 + str(单位.最大兵力 // 1000) + "千人"
                else:
                    文本 = 前缀 + str(单位.最大兵力) + "人"
            else:
                物体 = self.取物体类型(self.键码, self.键状态)
                if 物体 != 0:
                    文本 = self.取格子描述(物体, True)
                else:
                    文本 = "平地" if self.取地形(self.键码, self.键状态) == 0 else "湖泊"
        游戏界面.绘制文本(文本, 6, i + 3, 图形对象, 16777135, 2039583)

    # ---------- 绘制面板 ----------
    def 绘制面板(self, 图形对象, x, y, 宽, 高, 立体):
        图形对象.设置颜色(1583194 if 立体 else 0)
        if 立体:
            图形对象.填充矩形(x, y, 宽, 高)
        else:
            图形对象.填充矩形(x + 1, y + 1, 宽 - 2, 高 - 2)
        图形对象.设置颜色(6583200 if 立体 else 1121081)
        图形对象.画线(x + 1, y, (x + 宽) - 2, y)
        图形对象.画线(x, y + 1, x, (y + 高) - 2)
        图形对象.设置颜色(1121081 if 立体 else 6583200)
        图形对象.画线((x + 宽) - 1, y + 1, (x + 宽) - 1, (y + 高) - 2)
        图形对象.画线(x + 1, (y + 高) - 1, (x + 宽) - 2, (y + 高) - 1)

    # ---------- 绘制地图（等距地形） ----------
    def 绘制地图(self, 图形对象, 偏移x, 偏移y, 地图):
        if not 地图 or not 地图[0]:
            return
        i3 = 偏移y
        length = (22 * (len(地图[0]) - 1)) + 偏移x
        图像宽 = self.地图图像.get_width()
        for i4 in range(len(地图)):
            i5 = i3
            i6 = length
            for 列 in range(len(地图[i4]) - 1, -1, -1):
                if i6 < 游戏界面.屏幕宽 and i5 < self.信息栏高度 and i6 + 44 > 0 and i5 + 21 > self.屏幕偏移y:
                    地形序号 = 地图[i4][列] & 255
                    # 关卡数据中的低字节是瓦片编号；异常状态不能让索引越界并中断主循环。
                    if 地形序号 >= len(瓦片偏移):
                        地形序号 = 0
                    裁剪宽 = 44 if i6 + 44 < 游戏界面.屏幕宽 else 游戏界面.屏幕宽 - i6
                    裁剪高 = 21 if i5 + 21 < self.信息栏高度 else self.信息栏高度 - i5
                    if i5 > self.屏幕偏移y:
                        图形对象.设置裁剪(i6, i5, 裁剪宽, 裁剪高)
                    else:
                        图形对象.设置裁剪(i6, self.屏幕偏移y, 裁剪宽, 裁剪高)
                    偏移x2 = 瓦片偏移[地形序号][0]
                    偏移y2 = 瓦片偏移[地形序号][1]
                    if ((地图[i4][列] >> 16) & 255) == self.游戏状态 and self.游戏状态 != 0:
                        # 选中城市高亮瓦片：显示 领地图块（map.png 中第 6 个瓦片，x=220 处，黄色菱形）
                        # 取源图中 x∈[220,264] 的 44x21 区域，贴到当前格 (i6, i5)。
                        self._绘制地图块(图形对象, i6, i5, self.地图图像, (220, 0, 44, 21))
                    elif 偏移x2 == 0:
                        self._绘制地图块(图形对象, i6 - (偏移x2 * 44), i5, self.地图图像, (0, 0, 44, 21))
                    else:
                        i12 = 偏移x2 * 44
                        if (偏移y2 & 8192) != 0:
                            i12 = 图像宽 - ((偏移x2 + 1) * 44)
                        src = (i12, 0, 44, 21)
                        # 目标固定为本格 (i6, i5)，源取自 map.png 中对应瓦片
                        self._绘制地图块(图形对象, i6, i5, self.地图图像, src)
                i6 -= 22
                i5 += 11
            length += 22
            i3 += 11
        图形对象.设置裁剪(0, 0, 游戏界面.屏幕宽, 游戏界面.屏幕高)

    def _绘制地图块(self, 图形对象, x, y, 图像, src):
        图形对象.表面.blit(图像, (x, y), src)

    # ---------- 绘制地图覆盖（城市/部队/建筑） ----------
    def 绘制地图覆盖(self, 图形对象, 偏移x, 偏移y, 地图):
        if not 地图 or not 地图[0]:
            return
        i3 = 偏移y + 10
        length = (22 * len(地图[0])) + 偏移x
        for i4 in range(len(地图)):
            i5 = i3
            i6 = length
            for 列 in range(len(地图[i4]) - 1, -1, -1):
                物体 = self.取物体类型(列, i4)
                if 物体 > 0:
                    if 物体 == 12:
                        城 = self.取城市(self.取城市所属(列, i4), False)
                        if 城 is not None:
                            self.绘制精灵(图形对象, i6, i5, 城.科技等级, None, True)
                            if 城.动画帧 >= 0:
                                self.绘制精灵(图形对象, i6, i5 - 11, 城.动画帧, None, True)
                    else:
                        self.绘制精灵(图形对象, i6, i5, 物体 - 1, None, True)
                部队id = self.取部队id(列, i4)
                if 部队id > 0:
                    a单位 = self.取部队(部队id, False)
                    if a单位 is not None:
                        a单位.绘制部队(图形对象, i6, i5)
                i6 -= 22
                i5 += 11
            length += 22
            i3 += 11
        图形对象.设置裁剪(0, 0, 游戏界面.屏幕宽, 游戏界面.屏幕高)

    # ---------- 绘制对话框 ----------
    def 绘制对话框(self, 图形对象):
        行高 = self.字体.取高度() + 2
        图形对象.设置颜色(20223)
        i = (游戏界面.屏幕高 - self.对话框屏幕高) >> 1
        i2 = 行高 + 2
        i3 = (游戏界面.屏幕宽 - self.对话框屏幕宽) >> 1
        图形对象.设置颜色(4144959)
        绘制.绘制矩形(图形对象, i3 + 2, i + 3, self.对话框屏幕宽, self.对话框屏幕高, 2130706432, 0)
        i4 = i
        for i5 in range(len(状态栏颜色)):
            图形对象.设置颜色(状态栏颜色[i5])
            图形对象.填充矩形(i3, i4, self.对话框屏幕宽, i2)
            i4 += 1
            i2 -= 2
            if i2 <= 0:
                break
        游戏界面.绘制文本(self.任务标题, i3 + 4, i + 2, 图形对象, 0, 16777215)
        图形对象.设置颜色(16644325)
        i6 = i + 行高 + 2
        图形对象.填充矩形(i3, i6, self.对话框屏幕宽, (self.对话框屏幕高 - 行高) - 2)
        图形对象.设置颜色(0)
        图形对象.画矩形(i3, i, self.对话框屏幕宽 - 1, self.对话框屏幕高 - 1)
        i7 = i6 + 2
        i8 = 行高 if self.外交动作 == 0 else 行高 * 2
        i9 = (self.对话框屏幕高 - (i8 if self.外交动作 == 0 else 行高 * 2)) // 行高
        for i10 in range(self.外交索引, self.对话框行数):
            游戏界面.绘制文本(self.对话框文本[i10], i3 + 4, i7, 图形对象, self.对话框颜色[i10], 0)
            i7 += 行高
            if i7 + i8 > (self.对话框屏幕高 + i) - 6:
                break
        i11 = (i + self.对话框屏幕高) - 2
        i12 = i3 + (self.对话框屏幕宽 >> 1)
        if i9 < self.对话框行数:
            if self.外交索引 > 0:
                帧 = 44 if self.外交索引 + i9 < self.对话框行数 else 43
            else:
                帧 = 42
            self.绘制精灵(图形对象, i12, i11, 帧, None, False)
        if self.外交动作 == 0:
            self.绘制精灵(图形对象, (i3 + self.对话框屏幕宽) - 2, i11, 41, None, False)
            return
        字符串宽 = 图形.取默认字体().字符串宽度(确认文本[0]) + 6
        i13 = i3 + 6
        i14 = ((i + self.对话框屏幕高) - 行高) - 6
        图形对象.设置颜色(0)
        图形对象.画矩形(i13, i14, 字符串宽, 行高 + 2)
        图形对象.绘制字符串(确认文本[self.外交动作 - 1], i13 + 3, i14 + 2, 8, 字体=self.字体, 颜色=图形对象.取颜色())
        i15 = ((i3 + self.对话框屏幕宽) - 6) - 字符串宽
        图形对象.画矩形(i15, i14, 字符串宽, 行高 + 2)
        图形对象.绘制字符串(取消文本[self.外交动作 - 1], i15 + 3, i14 + 2, 8, 字体=self.字体, 颜色=图形对象.取颜色())

    # ---------- 选择下一城 ----------
    def 选择下一城(self, z):
        i = 0 if self.选中城 is None else self.选中城.归属id - 1
        for _ in range(self.城市菜单索引):
            i += 1
            if i > self.城市菜单索引:
                i = 0
            if self.城市们[i].科技等级 > 0 and (not z or self.城市们[i].势力序号 == 0):
                self.移动光标(self.城市们[i].x, self.城市们[i].y)
                self.选中城 = self.城市们[i]
                break
        if self.选中城 is None:
            return
        self.键码 = self.选中城.x
        self.键状态 = self.选中城.y
        self.游戏状态 = self.选择格子(self.键码, self.键状态)

    # ---------- 选择下一部队 ----------
    def 选择下一部队(self):
        i = -1 if self.临时部队 is None else self.临时部队.部队id - 1
        找到 = False
        i2 = 0
        while i2 < self.城市信息索引:
            i += 1
            if i >= self.城市信息索引:
                i = 0
            单位 = self.部队们[i]
            if (单位.归属id > 0 and 单位.势力序号 == 0 and
                    not ((战斗模式 and 单位.兵种 != 3 and 单位.兵种 != 4) or
                         单位.是否死亡 or 单位.攻击冷却 == 0)):
                self.移动光标(单位.地图x, 单位.地图y)
                self.临时部队 = 单位
                找到 = True
                break
            i2 += 1
        if not 找到:
            if self.显示对话框("操作提示", "结束本回合？", True, 1) != 1:
                return False
            self.菜单状态 = 10
            return False
        self.键码 = self.临时部队.地图x
        self.键状态 = self.临时部队.地图y
        self.游戏状态 = self.选择格子(self.键码, self.键状态)
        return True

    # ---------- 按键处理 ----------
    def 按键按下(self, i):
        if self.菜单状态 == 9 or self.菜单状态 == 11:
            self.菜单.处理按键(i)
        elif self.菜单状态 in (5, 6) and self.城市数 == 3:
            # 外交页签的左右方向键用于切换外交动作，不应被当作地图光标移动。
            if i in (输入.键左, 输入.键右, 输入.键确定, 输入.键左软键, 输入.键右软键, 输入.键上, 输入.键下):
                if self.对话框宽度 == 0:
                    self.回合计数 = -1
                self.对话框宽度 = i
                self.对话框高度 = 0
        elif self.对话框宽度 != i:
            if self.对话框宽度 == 0:
                self.回合计数 = -1
            self.对话框宽度 = i
            self.对话框高度 = 0

    def 按键释放(self):
        if self.菜单状态 == 9 or self.菜单状态 == 11 or self.对话框宽度 == 0:
            return
        self.对话框高度 = 1
# -*- coding: utf-8 -*-
# 追加段：城市面板 & 外交绘制（由主脚本拼接进 游戏.py）

    # ---------- 绘制城市面板 ----------
    def 绘制城市面板(self, 图形对象):
        self.绘制城市菜单页签(图形对象)
        行高 = self.字体.取高度() + 2
        i = ((游戏界面.屏幕高 - 6) - 行高) // 行高
        if self.城市数 == 0:
            self.绘制生产菜单(图形对象, i, 行高)
        elif self.城市数 == 1:
            self.绘制城市信息(图形对象, i, 行高)
        elif self.城市数 == 2:
            self.绘制城市列表(图形对象, i, 行高)
        elif self.城市数 == 3:
            self.绘制外交列表(图形对象, i, 行高)

    # ---------- 绘制城市菜单页签 ----------
    def 绘制城市菜单页签(self, 图形对象):
        字体 = self.字体
        高 = 字体.取高度() + 2
        图形对象.设置颜色(16711680)
        图形对象.画线(0, 高, 0, 游戏界面.屏幕高 - 1)
        图形对象.画线(0, 游戏界面.屏幕高 - 1, 游戏界面.屏幕宽 - 1, 游戏界面.屏幕高 - 1)
        图形对象.画线(游戏界面.屏幕宽 - 1, 高, 游戏界面.屏幕宽 - 1, 游戏界面.屏幕高 - 1)
        i2 = 0
        i3 = 0
        length = len(self.游戏数据_动作名)
        for i4 in range(length):
            if i4 == self.城市数:
                i3 = i2
                i = i2
                iA = 字体.字符串宽度(self.游戏数据_动作名[i4]) + 4
            else:
                i = i2
                iA = self.绘制按钮(图形对象, i2, 0, self.游戏数据_动作名[i4], 字体, False)
            i2 = i + iA
        图形对象.设置颜色(16711680)
        if i3 > 0:
            图形对象.画线(0, 高, i3, 高)
        iA2 = i3 + self.绘制按钮(图形对象, i3, 0, self.游戏数据_动作名[self.城市数], 字体, True)
        图形对象.设置颜色(16711680)
        图形对象.画线(iA2, 高, 游戏界面.屏幕宽, 高)

    # ---------- 绘制按钮 ----------
    def 绘制按钮(self, 图形对象, x, y, 文本, 字体, 选中):
        宽度 = 字体.字符串宽度(文本) + 4
        高 = 字体.取高度() + 2
        if not 选中:
            y += 2
            高 -= 2
        图形对象.设置颜色(16711680 if 选中 else 8323072)
        图形对象.画线(x, y + 高, x + 2, y)
        图形对象.画线(x, y + 高, x + 宽度, y + 高)
        图形对象.画线(x + 宽度, y + 高, x + 宽度 - 2, y)
        图形对象.设置颜色(16777215 if 选中 else 8355711)
        图形对象.绘制字符串(文本, x + 2, y + 1, 8, 字体=self.字体, 颜色=图形对象.取颜色())
        return 宽度

    # ---------- 绘制生产菜单 ----------
    def 绘制生产菜单(self, 图形对象, 行数, 行高):
        字符串宽 = 图形.取默认字体().字符串宽度(部队.兵种名[5]) + 10
        i3 = 行高 + 2
        图形对象.设置颜色(8614)
        图形对象.画矩形(4, i3, 字符串宽, ((行数 - 1) * 行高) + 2)
        i4 = (self.部队数 - 行数) + 3
        if i4 < 0:
            i4 = 0
        i5 = i3 + 1
        i6 = 11513775 if self.菜单状态 == 6 else 16777184
        for i7 in range(i4, (i4 + 行数) - 1):
            if i7 >= self.菜单选项数:
                break
            if i7 == self.部队数:
                图形对象.设置颜色(1865065)
                图形对象.填充矩形(4 + 1, i5, 字符串宽, 行高)
            if self.菜单选项索引[i7] < len(部队.兵种名):
                游戏界面.绘制文本(部队.兵种名[self.菜单选项索引[i7]], 4 + 5, i5, 图形对象, i6, 0)
            else:
                游戏界面.绘制文本("扩建", 4 + 5, i5, 图形对象, i6, 0)
            i5 += 行高
        i8 = 4 + 字符串宽 + 4
        i9 = self.菜单选项索引[self.部队数]
        i10 = 0 if i9 == len(部队.兵种名) else (部队.兵种属性[i9][4] - self.选中城.生产进度) * 5
        if i10 < 0:
            i10 = 0
        if i9 < len(部队.兵种名):
            self.绘制精灵(图形对象, i8 + 30, i3 + 25, 部队.部队图像序号(i9), None, False)
            i11 = i3 + 30
            游戏界面.绘制文本("已完成:" + str(self.选中城.生产进度), i8, i11, 图形对象, i6, 0)
            i12 = i11 + 行高
            游戏界面.绘制文本("生产力:" + str(self.选中城.取生产速率()), i8, i12, 图形对象, i6, 0)
            i13 = i12 + 行高
            游戏界面.绘制文本("完成:" + str(self.选中城.取生产回合数(i9)) + "回合", i8, i13, 图形对象, i6, 0)
            游戏界面.绘制文本("购买价:" + str(i10), i8, i13 + 行高, 图形对象, i6, 0)
        else:
            self.绘制精灵(图形对象, i8 + 30, i3 + 30, self.选中城.科技等级, None, False)
            i14 = i3 + 40
            游戏界面.绘制文本("增加城市防御", i8, i14, 图形对象, i6, 0)
            游戏界面.绘制文本("效果:" + str(self.选中城.取生产速率() // 10), i8, i14 + 行高, 图形对象, i6, 0)
        if i9 == len(部队.兵种名) or self.选中城.势力序号 != 0 or i10 > self.选中城.属性[6] or i10 <= 0:
            return
        i15 = (游戏界面.屏幕高 - 行高) - 8
        self.绘制面板(图形对象, 6, i15, 图形.取默认字体().字符串宽度(菜单标签[5]) + 8, 行高 + 3, True)
        游戏界面.绘制文本(菜单标签[5], 6 + 4, i15 + 2, 图形对象, 16777215, 1052688)

    # ---------- 绘制城市信息 ----------
    def 绘制城市信息(self, 图形对象, i, i2):
        length = len(self.选中城.属性) + len(self.选中城.建筑) + 4
        i3 = self.外交索引
        if i3 < 0:
            i3 = 0
        i5 = i2 + 2
        字符串宽 = 图形.取默认字体().字符串宽度(部队.兵种名[5]) + 8
        self.选中城.计算增量(数据表.增量缓存)
        图形对象.设置颜色(127)
        图形对象.填充矩形(1, i5, 游戏界面.屏幕宽 - 2, i2)
        for i6 in range(i3, (i3 + i) - 1):
            if i6 >= length:
                break
            图形对象.设置颜色(16777184 if i6 % 2 == 0 else 15133695)
            if i6 == 0:
                图形对象.绘制字符串("城市", 4, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
                图形对象.绘制字符串(self.选中城.城名, 4 + 字符串宽, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            elif i6 == 1:
                图形对象.绘制字符串("物价", 4, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
                图形对象.绘制字符串(str(self.视野倍率), 4 + 字符串宽, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            elif i6 == 2:
                图形对象.绘制字符串("生产力", 4, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
                图形对象.绘制字符串(str(self.选中城.取生产速率()), 4 + 字符串宽, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            elif i6 == 3:
                图形对象.绘制字符串("特产", 4, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
                文本 = ""
                for i7 in range(len(self.选中城.资源)):
                    if self.选中城.资源[i7] > 0:
                        if len(文本) > 0:
                            文本 += ","
                        文本 += 数据表.特产名[i7]
                if len(文本) == 0:
                    文本 = "无"
                图形对象.绘制字符串(文本, 4 + 字符串宽, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            elif i6 < len(self.选中城.属性) + 4:
                length2 = (len(数据表.城市属性名) - i6) + 3
                图形对象.绘制字符串(数据表.城市属性名[length2], 4, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
                i8 = self.选中城.属性[length2]
                值文本 = (str(i8 // 10000) + "万(") if i8 >= 10000 else (str(i8) + "(")
                if 数据表.增量缓存[length2] >= 0:
                    值文本 += "+"
                值文本 += str(数据表.增量缓存[length2]) + ")"
                图形对象.绘制字符串(值文本, 4 + 字符串宽, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            else:
                建筑序号 = (i6 - 4) - len(数据表.城市属性名)
                图形对象.绘制字符串(数据表.建筑名[建筑序号], 4, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
                值文本 = str(self.选中城.建筑[建筑序号]) + "(+" + 数据表.城市属性名[数据表.建筑效果[建筑序号][0]] + ")"
                图形对象.绘制字符串(值文本, 4 + 字符串宽, i5, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            i5 += i2
        if self.选中城.势力序号 != 0:
            return
        i9 = (游戏界面.屏幕高 - i2) - 8
        字符串宽2 = 图形.取默认字体().字符串宽度(确认文本[0]) + 8
        i10 = (游戏界面.屏幕宽 - 字符串宽2) - 6
        self.绘制面板(图形对象, i10, i9, 字符串宽2, i2 + 3, True)
        游戏界面.绘制文本(菜单标签[3], i10 + 4, i9 + 2, 图形对象, 16777215 if self.选中城.属性[6] >= self.视野倍率 else 8355711, 1052688)
        self.绘制面板(图形对象, 6, i9, 字符串宽2, i2 + 3, True)
        游戏界面.绘制文本(菜单标签[4], 10, i9 + 2, 图形对象, 16777215 if self.选中城.属性[3] >= 100 else 8355711, 1052688)

    # ---------- 绘制城市列表 ----------
    def 绘制城市列表(self, 图形对象, i, i2):
        i4 = i2 + 2
        字符串宽 = 4 + 图形.取默认字体().字符串宽度(部队.兵种名[5]) + 4
        i5 = 字符串宽 * 2
        i6 = (self.外交索引 - i) + 3
        if i6 < 0:
            i6 = 0
        图形对象.设置颜色(6250335)
        图形对象.填充矩形(1, i4, 游戏界面.屏幕宽 - 2, i2)
        图形对象.设置颜色(16777215)
        for 列, 标签 in enumerate(self.城市信息标签):
            图形对象.绘制字符串(标签, 4 + (列 * 字符串宽), i4 + 1, 8, 字体=self.字体, 颜色=图形对象.取颜色())
        i7 = i4 + i2 + 1
        for i8 in range(i6, (i6 + i) - 2):
            if i8 >= self.城市菜单索引:
                break
            if i8 == self.外交索引:
                图形对象.设置颜色(127)
                图形对象.填充矩形(1, i7, 游戏界面.屏幕宽 - 2, i2)
            城 = self.城市们[i8]
            图形对象.设置颜色(16777184 if 城.势力序号 == 0 else 8355711)
            图形对象.绘制字符串(城.城名, 4, i7, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            图形对象.绘制字符串(势力名[城.势力序号], 字符串宽, i7, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            图形对象.绘制字符串(str(城.属性[2] // 10000) + "万", i5, i7, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            i7 += i2
        i9 = (游戏界面.屏幕高 - i2) - 8
        self.绘制面板(图形对象, 8, i9, 图形.取默认字体().字符串宽度(确认文本[0]) + 16, i2 + 3, True)
        游戏界面.绘制文本(菜单标签[0], 16, i9 + 2, 图形对象, 16777215, 1052688)

    # ---------- 绘制外交列表 ----------
    def 绘制外交列表(self, 图形对象, i, i2):
        i4 = i2 + 2
        字符串宽 = 4 + 图形.取默认字体().字符串宽度(部队.兵种名[5]) + 4
        i5 = 字符串宽 * 2
        图形对象.设置颜色(6250335)
        图形对象.填充矩形(1, i4, 游戏界面.屏幕宽 - 2, i2)
        图形对象.设置颜色(16777215)
        for 列, 标签 in enumerate(self.国家信息标签):
            图形对象.绘制字符串(标签, 4 + (列 * 字符串宽), i4 + 1, 8, 字体=self.字体, 颜色=图形对象.取颜色())
        i6 = i4 + i2 + 1
        行 = self.外交数据[0]
        for i7 in range(1, len(行)):
            if i7 == self.外交索引 + 1:
                图形对象.设置颜色(127)
                图形对象.填充矩形(1, i6, 游戏界面.屏幕宽 - 2, i2)
            图形对象.设置颜色(16777184)
            图形对象.绘制字符串(势力名[i7], 4, i6, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            图形对象.绘制字符串(关系名[self.取外交值(i7, 0, 1)], 字符串宽, i6, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            图形对象.绘制字符串(str(self.取外交值(i7, 0, 0)), i5, i6, 8, 字体=self.字体, 颜色=图形对象.取颜色())
            i6 += i2
        if self.子菜单索引 > 0:
            return
        i8 = (游戏界面.屏幕高 - i2) - 8
        字符串宽2 = 图形.取默认字体().字符串宽度(确认文本[0]) + 8
        i9 = (游戏界面.屏幕宽 - 字符串宽2) - 6
        self.绘制面板(图形对象, i9, i8, 字符串宽2, i2 + 3, True)
        游戏界面.绘制文本(菜单标签[2], i9 + 4, i8 + 2, 图形对象, 16777215, 1052688)
        i10 = 16777215 if self.可执行外交(0, self.外交索引 + 1, self.外交目标, self.选中城) else 8355711
        self.绘制面板(图形对象, 6, i8, 字符串宽2, i2 + 3, True)
        游戏界面.绘制文本(菜单标签[1], 10, i8 + 2, 图形对象, i10, 1052688)
        i11 = (游戏界面.屏幕宽 - 字符串宽2) // 2
        图形对象.设置颜色(11468800)
        图形对象.画矩形(i11, i8, 字符串宽2, i2 + 3)
        图形对象.设置颜色(i10)
        图形对象.绘制字符串(外交动作[self.外交目标], i11 + 4, i8 + 3, 8, 字体=self.字体, 颜色=图形对象.取颜色())

    # ---------- 绘制建造菜单 ----------
    def 绘制建造菜单(self, 图形对象):
        行高 = self.字体.取高度() + 2
        i = 游戏界面.屏幕宽 - 8
        if i > 160:
            i = 160
        i2 = (行高 * 7) + 6
        i3 = (游戏界面.屏幕宽 - i) >> 1
        i4 = ((self.信息栏高度 - i2) + self.屏幕偏移y) // 2
        字符串宽 = 图形.取默认字体().字符串宽度(数据表.建筑名[0]) + 4
        绘制.绘制矩形(图形对象, i3 + 1, i4 + 2, i, i2, 2130706432, 0)
        self.绘制面板(图形对象, i3, i4, i, i2, True)
        self.绘制面板(图形对象, i3 + 3, i4 + 3, 字符串宽 + 2, (i2 - 4) - (行高 * 2), False)
        i5 = self.部队数 - 4
        if i5 < 0:
            i5 = 0
        i6 = i4 + 4
        for i7 in range(i5, i5 + 5):
            if i7 >= self.菜单选项数:
                break
            if i7 == self.部队数:
                图形对象.设置颜色(16766976)
                图形对象.填充矩形(i3 + 4, i6, 字符串宽, 行高)
            游戏界面.绘制文本(数据表.建筑名[self.菜单选项索引[i7]], i3 + 5, i6, 图形对象, 16777215, 0)
            i6 += 行高
        i8 = i3 + 字符串宽 + 8
        i9 = self.菜单选项索引[self.部队数]
        瓦片 = self.选中城.取建筑瓦片id(self.菜单选项索引[self.部队数])
        if 瓦片 > 0:
            self.绘制精灵(图形对象, i8 + 36, i4 + 16, 瓦片 - 1, None, False)
        else:
            self.绘制精灵(图形对象, i8 + 36, i4 + 16, 49, None, False)
        i10 = i4 + 26
        i11 = 数据表.建筑造价[i9]
        游戏界面.绘制文本("造价:" + str(i11 // 5) + "金", i8, i10, 图形对象, 15133695, 1052688)
        i12 = i10 + 行高
        游戏界面.绘制文本("时间:" + str(self.选中部队.取攻击回合数(i11)) + "回合", i8, i12, 图形对象, 15133695, 1052688)
        i13 = i12 + 行高
        if 瓦片 > 0:
            for i14 in range(0, len(数据表.建筑效果[i9]), 2):
                i15 = 数据表.建筑效果[i9][i14 + 1]
                i16 = 数据表.建筑效果[i9][i14]
                if i15 > 0:
                    游戏界面.绘制文本("效果:+" + str(i15) + 数据表.城市属性名[i16], i8, i13, 图形对象, 15133695, 1052688)
                else:
                    游戏界面.绘制文本("维护:" + str(abs(i15)) + 数据表.城市属性名[i16], i8, i13, 图形对象, 15133695, 1052688)
                i13 += 行高
        else:
            游戏界面.绘制文本("搜索新特产", i8, i13, 图形对象, 15133695, 1052688)
        字符串宽2 = 图形.取默认字体().字符串宽度(确认文本[2]) + 10
        i17 = 行高 + 3
        i18 = ((i4 + i2) - i17) - 3
        i19 = i3 + 12
        self.绘制面板(图形对象, i19, i18, 字符串宽2, i17, True)
        游戏界面.绘制文本(确认文本[2], i19 + 6, i18 + 2, 图形对象, 16777200, 1052688)
        i20 = ((i3 + i) - 12) - 字符串宽2
        self.绘制面板(图形对象, i20, i18, 字符串宽2, i17, True)
        游戏界面.绘制文本(取消文本[2], i20 + 6, i18 + 2, 图形对象, 16777200, 1052688)

    # ---------- 外交值读写 ----------
    def 设置外交值(self, i, i2, i3, i4):
        行 = self.外交数据[i]
        if i3 == 0:
            行[i2] = (行[i2] & 0xFFFFFF00) | i4
        elif i3 == 1:
            行[i2] = (行[i2] & 0xFFFF00FF) | (i4 << 8)
        elif i3 == 2:
            行[i2] = (行[i2] & 0xFF00FFFF) | (i4 << 16)

    def 取外交值(self, i, i2, i3):
        i4 = self.外交数据[i][i2]
        if i3 == 0:
            i4 &= 255
        elif i3 == 1:
            i4 = (i4 >> 8) & 255
        elif i3 == 2:
            i4 = (i4 >> 16) & 255
        return i4
