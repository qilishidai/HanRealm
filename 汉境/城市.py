# -*- coding: utf-8 -*-
"""城市 —— 原 core.City 的中文移植。

城市属性增长、建筑建造、部队生产、领域扩张/清算、被攻占、流式存档。
"""

import 数据表
import 部队


class 城市:
    def __init__(self, 游戏, 归属id):
        self.游戏 = 游戏
        self.归属id = 归属id       # ownerId
        self.x = 0
        self.y = 0
        self.势力序号 = 0          # ownerIndex
        self.等级 = 0              # level
        self.建造成目标 = 0        # buildTarget
        self.生产进度 = 0          # productionProgress
        self.科技等级 = 0         # techLevel
        self.城名 = ""
        self.资源 = [0, 0, 0, 0, 0]             # 马/金/铁/中药/铜
        self.属性 = [0, 0, 0, 0, 0, 0, 0]       # 文化/防御/人口/食物/士兵/训练/金
        self.建筑 = [0, 0, 0, 0, 0, 0, 0]       # 农田/牧场/基地/兵营/民房/书院/市场
        self.动画帧 = -1

    # ---- 城市被攻击时的处理，返回反击伤害 ----
    def 被攻击(self, 伤害, 攻方序号, 屏幕):
        减防 = clamp(伤害 // 100, 5, 50)
        属性 = self.属性
        属性[1] = 属性[1] - 减防
        if self.属性[1] < 0:
            self.属性[1] = 0
        else:
            伤害 = (伤害 - self.属性[1]) // 3
            if 伤害 < 1:
                伤害 = 1
        属性[4] = 属性[4] - 伤害
        if self.属性[4] < 0:
            self.属性[4] = 0
        if self.属性[5] > self.属性[4]:
            self.属性[5] = self.属性[4]
        属性[2] = 属性[2] - 伤害
        if self.属性[2] < 10000:
            self.属性[2] = 10000
        if self.属性[1] > 0 or self.属性[4] > 0:
            self.播放动画(50, 屏幕)
            return self.计算攻击力()
        self.播放动画(52, 屏幕)
        import 游戏 as 游戏模块
        self.游戏.显示对话框("战争信息",
                             游戏模块.格式化消息("%0攻陷了%1的城市[%2]",
                                                [数据表.势力名[攻方序号], 数据表.势力名[self.势力序号], self.城名]),
                             True, 0)
        self.势力序号 = 攻方序号
        self.清算领域()
        self.属性[0] = 0
        self.扩张领域()
        self.游戏.销毁归属部队(self.归属id)
        return 0

    # ---- 播放动画 ----
    def 播放动画(self, 帧id, 屏幕):
        self.动画帧 = 帧id
        for _ in range(6):
            屏幕.重绘屏幕()
            屏幕.睡眠(50)
            self.动画帧 = (帧id + 1) - (self.动画帧 - 帧id)
        self.动画帧 = -1

    # ---- 计算攻击力/反击伤害 ----
    def 计算攻击力(self):
        i = (self.属性[4] + self.属性[5]) // 20
        if i < 1:
            i = 1
        return i

    # ---- 城市每回合更新 ----
    def 更新城市(self, 屏幕):
        原半径 = self.取领域半径()
        self.游戏.移动光标(self.x, self.y)
        缓存 = 数据表.增量缓存
        self.计算增量(缓存)
        for i in range(len(缓存)):
            属性 = self.属性
            属性[i] = 属性[i] + 缓存[i]
        # 军费/资金不足
        if self.属性[6] < 0:
            单位 = self.游戏.查找归属部队(self.归属id)
            if 单位 is not None:
                # 先由阵亡流程播放动画，再恢复最低资金，避免随后几回合持续触发。
                单位.阵亡(屏幕)
                self.属性[6] = 100
                if self.势力序号 == 0:
                    self.游戏.显示对话框("城市信息", "由于资金不够，" + self.城名 + "的一些部队被解散了", True, 0)
            else:
                if self.势力序号 == 0 and self.属性[4] > 0:
                    self.游戏.显示对话框("城市信息", "军饷短缺，" + self.城名 + "的士兵大量开小差", True, 0)
                属性 = self.属性
                属性[4] = 属性[4] - 2000
                if self.属性[4] < 0:
                    self.属性[4] = 0
                # 与原版保持一致：没有可解散部队时不让资金无限期保持负数。
                if self.属性[6] < 0:
                    self.属性[6] = 0
        # 粮食短缺
        if self.属性[3] < 0:
            if self.势力序号 == 0:
                self.游戏.显示对话框("城市信息", "由于粮食短缺，" + self.城名 + "的居民大量逃亡。", True, 0)
            属性 = self.属性
            属性[2] = 属性[2] + self.属性[3]
            if self.属性[2] < 20000:
                self.属性[2] = 20000
            self.属性[3] = 0
        # 生产部队
        if self.建造成目标 >= len(部队.兵种名) or self.游戏.取部队id(self.x, self.y) != 0:
            self.生产进度 = 0
        else:
            self.生产进度 += self.取生产速率()
            新单位 = self.游戏.取部队(-1, False)
            if self.生产进度 >= 部队.兵种属性[self.建造成目标][4] and 新单位 is not None:
                士兵 = self.属性[4]
                造价 = 部队.兵种属性[self.建造成目标][0]
                训练 = self.属性[5]
                if self.建造成目标 == 3 or self.建造成目标 == 4:
                    士兵 = self.属性[2] - 20000
                    if 士兵 >= 造价:
                        属性 = self.属性
                        属性[2] = 属性[2] - 造价
                else:
                    if 士兵 > 造价:
                        属性 = self.属性
                        属性[4] = 属性[4] - 造价
                if 士兵 >= 造价:
                    self.生产进度 = 0
                    单位对象 = self.游戏.取部队(-1, True)
                    if 训练 > 造价:
                        训练 = 造价
                    属性 = self.属性
                    属性[5] = 属性[5] - 训练
                    单位对象.初始化部队(造价, 训练, self.建造成目标, self.归属id, self.势力序号)
                    单位对象.地图x = self.x
                    单位对象.地图y = self.y
                    self.游戏.设置部队id(self.x, self.y, 单位对象.部队id)
                    if self.势力序号 == 0:
                        self.游戏.显示对话框("部队", str(self.城名) + "的" + 部队.兵种名[self.建造成目标] + "组建完成了", True, 0)
        # 领域扩张/收缩
        新半径 = self.取领域半径()
        if 新半径 > 原半径:
            self.扩张领域()
            self.游戏.显示对话框("城市信息", "由于文化的发展，" + self.城名 + "的领域扩充了!", True, 0)
        elif 新半径 < 原半径:
            self.清算领域()
            self.扩张领域()
        if self.势力序号 == 0:
            return
        # AI 城市建设逻辑
        self.游戏.按类型统计部队(self.势力序号, -1)
        结盟 = self.游戏.取外交值(self.势力序号, self.游戏.找最佳盟友(self.势力序号), 1) == 1
        self.建造成目标 = 8
        if self.游戏.按归属统计部队(self.归属id, 3) < 1:
            self.建造成目标 = 3
        else:
            if not 结盟:
                if self.游戏.按归属统计部队(self.归属id, 3) < 2:
                    self.建造成目标 = 3
                elif self.属性[1] < 300:
                    self.建造成目标 = 8
                else:
                    结盟 = True
            if 结盟 and self.游戏.按归属统计部队(self.归属id, -1) < 6:
                for i in range(len(部队.训练优先级)):
                    if self.具备资源(部队.训练优先级[i]):
                        self.建造成目标 = 部队.训练优先级[i]
                        break
        # 决定建造哪种建筑（AI）
        最优 = 0
        最差 = 0
        for i in range(1, len(缓存)):
            if 数据表.属性索引[i] >= 0:
                if 缓存[最优] * 数据表.基础值[最优] < 缓存[i] * 数据表.基础值[i]:
                    最优 = i
                if 缓存[最差] * 数据表.基础值[最差] > 缓存[i] * 数据表.基础值[i]:
                    最差 = i
        建造属性 = 数据表.属性索引[最优]
        清除属性 = 数据表.属性索引[最差]
        while True:
            工人 = self.游戏.查找空闲工人(self.归属id)
            if 工人 is None or self.属性[6] < 数据表.建筑造价[清除属性] // 5:
                return
            属性 = self.属性
            属性[6] = 属性[6] - (数据表.建筑造价[清除属性] // 5)
            if not self.寻找建造位置(建造属性, self.游戏.光标位置, 工人.部队id):
                return
            工人.移动到(self.游戏.光标位置[0], self.游戏.光标位置[1], 屏幕)
            工人.开始建造(清除属性, self)

    # ---- 查找建造位置 ----
    def 寻找建造位置(self, 建筑类型, 输出位置, 部队id):
        半径 = self.取领域半径()
        找到 = False
        for 偏移y in range(-半径, 半径 + 1):
            for 偏移x in range(-半径, 半径 + 1):
                x = self.x + 偏移x
                y = self.y + 偏移y
                if not self.游戏.越界(x, y) and self.游戏.取归属id(x, y) == self.归属id:
                    i = self.游戏.取部队id(x, y)
                    if i == 0 or i == 部队id:
                        物体 = self.游戏.取物体类型(x, y)
                        if 物体 == 0:
                            输出位置[0] = x
                            输出位置[1] = y
                            找到 = True
                        elif not 找到 and 数据表.建筑索引(物体) == 建筑类型:
                            找到 = True
                            输出位置[0] = x
                            输出位置[1] = y
        return 找到

    # ---- 计算城市总价值（评分） ----
    def 计算城市价值(self):
        i = 0
        for i2 in range(len(self.资源)):
            i += self.资源[i2] * 数据表.特产值[i2]
        for i3 in range(len(self.属性)):
            i += (self.属性[i3] * 数据表.价值权重[i3]) // 1000
        for i4 in range(len(self.建筑)):
            i += 500 * self.建筑[i4]
        return i

    # ---- 获取属性上限 ----
    def 取最高属性(self, 属性序号):
        科技 = 数据表.科技需求[属性序号][0]
        上限 = ((self.建筑[科技] + 1) * 数据表.科技需求[属性序号][1]) if 科技 >= 0 else 数据表.科技需求[属性序号][1]
        if len(数据表.科技需求[属性序号]) > 2 and 上限 > self.取最高属性(数据表.科技需求[属性序号][2]):
            上限 = self.取最高属性(数据表.科技需求[属性序号][2])
        return 上限

    # ---- 计算每回合属性变化 ----
    def 计算增量(self, 增量):
        for i in range(len(增量)):
            增量[i] = 0
        for i2 in range(len(self.资源)):
            增量[6] = 增量[6] + (self.资源[i2] * 数据表.特产消耗[i2])
        for i3 in range(len(self.建筑)):
            for i4 in range(0, len(数据表.建筑效果[i3]), 2):
                属性i = 数据表.建筑效果[i3][i4]
                增量[属性i] = 增量[属性i] + (数据表.建筑效果[i3][i4 + 1] * (self.建筑[i3] + 1))
        增量[3] = 增量[3] - (self.属性[2] // 500)
        增量[6] = 增量[6] + (self.属性[2] // 10000)
        增量[3] = 增量[3] - (self.属性[4] // 200)
        if self.建造成目标 == len(部队.兵种名):
            增量[1] = 增量[1] + (self.取生产速率() // 10)
        self.游戏.计算部队维护(self.归属id, 增量)
        for i6 in range(len(self.属性)):
            if 增量[i6] >= 0 and 增量[i6] + self.属性[i6] > self.取最高属性(i6):
                差值 = self.取最高属性(i6) - self.属性[i6]
                if 差值 < 0:
                    差值 //= 20
                    if 差值 > -1:
                        差值 = -1
                增量[i6] = 差值

    # ---- 计算生产某兵种所需回合数 ----
    def 取生产回合数(self, 兵种):
        所需 = 部队.兵种属性[兵种][4]
        if 所需 <= self.生产进度:
            return 1
        速率 = self.取生产速率()
        return (((所需 + 速率) - 1) - self.生产进度) // 速率

    # ---- 获取生产速率（基于人口） ----
    def 取生产速率(self):
        i = self.属性[2] // 10000
        if i < 1:
            i = 1
        return i

    # ---- 检查是否拥有训练某兵种所需资源 ----
    def 具备资源(self, 兵种):
        有 = True
        for i2 in range(len(self.资源)):
            if ((部队.兵种资源需求[兵种] >> i2) & 1) != 0 and self.资源[i2] <= 0:
                有 = False
                break
        if 有 and 兵种 == 4:
            有 = self.游戏.取城市(-1, False) is not None
        return 有

    # ---- 获取建筑对应的地图瓦片ID ----
    def 取建筑瓦片id(self, 建筑类型):
        if 建筑类型 == 0:
            return 14
        if 建筑类型 == 1:
            return 15
        if 建筑类型 == 7:
            return -1
        return 14 + 建筑类型 + (self.等级 * 6)

    # ---- 从流加载 ----
    def 从流加载(self, 流):
        self.动画帧 = -1
        self.科技等级 = 流.读int()
        if self.科技等级 <= 0:
            return
        self.建造成目标 = 流.读int()
        self.生产进度 = 流.读int()
        self.城名 = 流.读utf()
        self.x = 流.读int()
        self.y = 流.读int()
        self.等级 = 流.读int()
        self.势力序号 = 流.读int()
        for i in range(len(self.属性)):
            self.属性[i] = 流.读int()
        for i2 in range(len(self.建筑)):
            self.建筑[i2] = 流.读int()
        for i3 in range(len(self.资源)):
            self.资源[i3] = 流.读int()

    # ---- 保存到流 ----
    def 存到流(self, 流):
        流.写int(self.科技等级)
        if self.科技等级 <= 0:
            return
        流.写int(self.建造成目标)
        流.写int(self.生产进度)
        流.写utf(self.城名)
        流.写int(self.x)
        流.写int(self.y)
        流.写int(self.等级)
        流.写int(self.势力序号)
        for i in range(len(self.属性)):
            流.写int(self.属性[i])
        for i2 in range(len(self.建筑)):
            流.写int(self.建筑[i2])
        for i3 in range(len(self.资源)):
            流.写int(self.资源[i3])

    # ---- 初始化城市 ----
    def 初始化城市(self, 名称, x, y, 等级, 规模, 势力序号):
        self.动画帧 = -1
        self.建造成目标 = 3
        self.生产进度 = 0
        self.城名 = 名称
        self.x = x
        self.等级 = 1 - 等级
        self.y = y
        self.科技等级 = 11 + 等级
        self.势力序号 = 势力序号
        self.属性[0] = 规模 * 100
        self.属性[1] = 规模 * 50
        self.属性[2] = 规模 * 100000
        self.属性[3] = 规模 * 1000
        self.属性[4] = 规模 * 400
        self.属性[5] = 规模 * 50
        self.属性[6] = 规模 * 500

    # ---- 获取城市领域半径（文化范围） ----
    def 取领域半径(self):
        i = 1
        while i * i * 50 < self.属性[0]:
            i += 1
        i2 = i + 1
        i3 = (self.属性[2] // 80000) + 1
        if i2 > i3:
            i2 = i3
        return i2

    # ---- 清算城市领域标记 ----
    def 清算领域(self):
        半径 = self.取领域半径() + 2
        for i in range(-半径, 半径 + 1):
            for i2 in range(-半径, 半径 + 1):
                x = self.x + i2
                y = self.y + i
                if not self.游戏.越界(x, y) and ((i != 0 or i2 != 0) and self.游戏.取归属id(x, y) == self.归属id):
                    self.游戏.设置归属id(x, y, 0)

    # ---- 扩张城市领域 ----
    def 扩张领域(self):
        半径 = self.取领域半径()
        资源备份 = [0] * len(self.资源)
        for i in range(len(self.建筑)):
            self.建筑[i] = 0
        for i2 in range(len(self.资源)):
            资源备份[i2] = self.资源[i2]
            self.资源[i2] = 0
        for i3 in range(-半径, 半径 + 1):
            for i4 in range(-半径, 半径 + 1):
                x = self.x + i4
                y = self.y + i3
                if (abs(i3) + abs(i4) <= 半径 and not self.游戏.越界(x, y) and
                        self.游戏.取地形(x, y) == 0 and (self.游戏.取归属id(x, y) == 0 or self.游戏.取归属id(x, y) == self.归属id)):
                    物体 = self.游戏.取物体类型(x, y)
                    if 物体 <= 0:
                        self.游戏.设置归属id(x, y, self.归属id)
                    else:
                        资源序号 = 数据表.资源索引(物体)
                        if 资源序号 >= 0:
                            if self.势力序号 == 0 and 资源备份[资源序号] == 0:
                                self.游戏.移动光标(x, y)
                                self.游戏.显示对话框("城市信息", str(self.城名) + "发现了特产:" + 数据表.特产名[资源序号], True, 0)
                            资源 = self.资源
                            资源[资源序号] = 资源[资源序号] + 1
                        elif 物体 != 50:
                            建筑序号 = 数据表.建筑索引(物体)
                            if 建筑序号 >= 0:
                                建筑 = self.建筑
                                建筑[建筑序号] = 建筑[建筑序号] + 1
                        self.游戏.设置归属id(x, y, self.归属id)


def clamp(值, 最小, 最大):
    if 值 < 最小:
        值 = 最小
    if 值 > 最大:
        值 = 最大
    return 值
