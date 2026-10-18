# -*- coding: utf-8 -*-
"""部队 —— 原 core.Unit 的中文移植。

兵种类别、移动/攻击/建造/探索/升级、攻击城市、被攻击反击、生存流式存档。
所有算法与原 Java 逐行对应，仅将标识符与数组表改为中文。
"""

import 游戏界面
import 数据表
import 城市
import 图形引擎


class 部队:
    # 类级/共享状态
    动画帧 = 0
    屏幕x = 0
    屏幕y = 0

    def __init__(self, 游戏, 部队id):
        self.游戏 = 游戏
        self.部队id = 部队id
        self.最大兵力 = 0
        self.当前兵力 = 0
        self.初始最大兵力 = 0
        self.移动目标x = 0
        self.移动目标y = 0
        self.建造进度 = 0
        self.兵种 = 0
        self.归属id = -1       # ownerId
        self.势力序号 = 0           # factionIndex
        self.移动相位 = 0           # movePhase
        self.移动计时 = 0           # moveTick
        self.经验 = 0
        self.等级 = 0
        self.地图x = 0
        self.地图y = 0
        self.方向掩码 = 0           # directionMask
        self.是否死亡 = False
        self.显示动画 = False
        self.伤害加成 = 0           # damageBonus
        self.攻击冷却 = 0           # attackCooldown
        self.目标部队 = None         # targetUnit

    # ---- 转换为民工 ----
    def 转为民工(self, 城市id):
        self.归属id = 城市id
        self.兵种 = 3
        造价 = 数据表.兵种属性[self.兵种][0]
        self.最大兵力 = 造价
        self.初始最大兵力 = 造价
        self.当前兵力 //= 5
        self.攻击冷却 = 3

    # ---- 更新移动方向掩码，检测可移动方向和目标 ----
    def 更新移动(self, 回合结束):
        self.方向掩码 = 0
        if 回合结束:
            return False
        for i in range(len(数据表.方向偏移)):
            if self.游戏.可通行(self.地图x + 数据表.方向偏移[i][0],
                                self.地图y + 数据表.方向偏移[i][1],
                                self.势力序号):
                self.方向掩码 |= 1 << i
        self.目标部队 = self.寻找远程目标()
        return self.方向掩码 > 0

    # ---- 远程兵种寻找攻击目标（弓兵/弩兵） ----
    def 寻找远程目标(self):
        if self.兵种 != 1 and self.兵种 != 6:
            return None
        for i in range(len(数据表.方向偏移)):
            for 距离 in range(1, 3):
                x = self.地图x + (数据表.方向偏移[i][0] * 距离)
                y = self.地图y + (数据表.方向偏移[i][1] * 距离)
                if not self.游戏.越界(x, y):
                    a单位 = self.游戏.取部队(self.游戏.取部队id(x, y), False)
                    if (a单位 is not None and a单位.势力序号 != self.势力序号 and
                            self.游戏.取外交值(self.势力序号, a单位.势力序号, 1) == 1):
                        return a单位
        return None

    # ---- 攻击城市 ----
    def 攻击城市(self, 城, 屏幕):
        if self.势力序号 == 城.势力序号 or self.游戏.取外交值(self.势力序号, 城.势力序号, 1) != 1:
            return
        self.游戏.移动光标(城.x, 城.y)
        self.攻击冷却 = -1
        if self.势力序号 == 城.势力序号:
            return
        反击 = 城.被攻击(self.计算随机伤害(self.计算攻击()), self.势力序号, 屏幕)
        self.当前兵力 += self.最大兵力 // 10
        if 反击 > 0:
            self.承受伤害(反击, 屏幕, True)
        else:
            self.当前兵力 += self.最大兵力 // 5
            if self.当前兵力 > self.最大兵力:
                self.当前兵力 = self.最大兵力
            self.归属id = 城.归属id
            self.获得经验(4, 屏幕)
        self.游戏.移动光标(self.地图x, self.地图y)

    # ---- 承受伤害 ----
    def 承受伤害(self, 伤害, 屏幕, 远程与否):
        if (self.兵种 == 1 or self.兵种 == 6) and not 远程与否:
            return
        self.最大兵力 -= self.计算随机伤害(伤害)
        if self.最大兵力 <= 0:
            self.最大兵力 = 0
            self.当前兵力 = 0
            self.阵亡(屏幕)
        elif self.当前兵力 > self.最大兵力:
            self.当前兵力 = self.最大兵力

    # ---- 获得经验，检查升级 ----
    def 获得经验(self, 经验增量, 屏幕):
        self.经验 += 经验增量
        if self.游戏.随机数(self.经验) > 8 * self.等级:
            self.经验 = 0
            if self.等级 < len(数据表.军衔名):
                self.等级 += 1
                self.游戏.取光标屏幕位置(self.地图x, self.地图y, self.游戏.光标位置, True)
                self.显示特效(self.游戏.光标位置[0], self.游戏.光标位置[1], 屏幕, 54)

    # ---- 清除死亡标记 ----
    def 清除死亡标记(self):
        self.是否死亡 = False
        self.游戏.设置物体类型(self.地图x, self.地图y, 0)

    # ---- 部队阵亡/销毁 ----
    def 阵亡(self, 屏幕):
        if self.归属id <= 0 and not self.是否死亡:
            return
        if self.是否死亡:
            self.清除死亡标记()
        self.方向掩码 = 0
        self.游戏.取光标屏幕位置(self.地图x, self.地图y, self.游戏.光标位置, True)
        self.显示特效(self.游戏.光标位置[0], self.游戏.光标位置[1], 屏幕, 52)
        self.重置部队()
        self.游戏.设置部队id(self.地图x, self.地图y, 0)

    # ---- 显示特效动画 ----
    def 显示特效(self, x, y, 屏幕, 特效id):
        部队.动画帧 = 特效id
        部队.屏幕x = x
        部队.屏幕y = y - 12
        self.显示动画 = True
        for _ in range(8):
            屏幕.重绘屏幕()
            屏幕.睡眠(50)
            部队.动画帧 = (特效id + 1) - (部队.动画帧 - 特效id)
        self.显示动画 = False

    # ---- 攻击敌方部队 ----
    def 攻击部队(self, 目标, 屏幕):
        if self.势力序号 == 目标.势力序号 or self.游戏.取外交值(self.势力序号, 目标.势力序号, 1) != 1:
            return
        self.游戏.移动光标(目标.地图x, 目标.地图y)
        self.攻击冷却 = -1
        if self.势力序号 == 目标.势力序号:
            return
        反击 = 目标.被攻击(self.计算随机伤害(self.计算攻击()), 屏幕)
        治疗 = self.最大兵力 // 10
        if 反击 == 0:
            治疗 = self.最大兵力 // 5
            if 目标.兵种 != 3:
                self.获得经验(1, 屏幕)
        else:
            self.承受伤害(反击, 屏幕, False)
        self.当前兵力 += 治疗
        if self.当前兵力 > self.最大兵力:
            self.当前兵力 = self.最大兵力
        self.游戏.移动光标(self.地图x, self.地图y)

    # ---- 被攻击时的响应，返回反击伤害 ----
    def 被攻击(self, 伤害, 屏幕):
        self.游戏.取光标屏幕位置(self.地图x, self.地图y, self.游戏.光标位置, True)
        实伤 = (伤害 * (100 - self.计算防御())) // 100
        if 实伤 < 1:
            实伤 = 1
        self.显示特效(self.游戏.光标位置[0], self.游戏.光标位置[1], 屏幕, 50)
        self.最大兵力 -= 实伤
        if self.最大兵力 <= 0:
            self.阵亡(屏幕)
            return 0
        if self.当前兵力 > self.最大兵力:
            self.当前兵力 = self.最大兵力
        return (self.计算攻击() // 4) + 1

    # ---- 显示部队信息 ----
    def 显示部队信息(self):
        self.游戏.显示对话框(str(数据表.势力名[self.势力序号]) + str(数据表.兵种名[self.兵种]),
                             "国家:" + str(数据表.势力名[self.势力序号]), False, 0)
        城 = self.游戏.取城市(self.归属id, False)
        if 城 is not None:
            self.游戏.添加对话框行("城市:" + str(城.城名), 8995872)
        self.游戏.添加对话框行("兵员:" + str(self.最大兵力), 9321)
        self.游戏.添加对话框行("老兵:" + str(self.当前兵力), 8995872)
        self.游戏.添加对话框行("等级:" + 数据表.军衔名[self.等级 - 1] + "级部队", 9321)
        self.游戏.添加对话框行("攻击:" + str(self.计算攻击()), 8995872)
        self.游戏.添加对话框行("防御:" + str(self.计算防御() * 50), 9321)
        self.游戏.添加对话框行("机动:" + str(数据表.兵种属性[self.兵种][2]), 8995872)

    # ---- 计算随机伤害（带随机浮动） ----
    def 计算随机伤害(self, 基础伤害):
        i = 基础伤害 // 5
        if i < 4:
            i = 4
        return 基础伤害 + (self.游戏.随机数(i) - (i >> 1))

    # ---- 计算攻击力 ----
    def 计算攻击(self):
        return ((数据表.兵种属性[self.兵种][1] * (self.最大兵力 + (self.当前兵力 * 2))) * (1 + self.等级)) // 400

    # ---- 计算防御力 ----
    def 计算防御(self):
        i2 = (数据表.兵种属性[self.兵种][3] * (self.最大兵力 + (self.当前兵力 * 2))) // (3 * self.最大兵力)
        i3 = (100 - i2) // 3
        for _ in range(1, self.等级):
            i2 += i3
            i3 = (100 - i2) // 3
        return i2

    # ---- 重置部队数据 ----
    def 重置部队(self):
        self.归属id = -1
        self.移动相位 = 0
        self.攻击冷却 = 数据表.兵种属性[self.兵种][2]
        self.方向掩码 = 0
        self.显示动画 = False

    # ---- 更新移动动画相位 ----
    def 更新移动动画(self):
        if self.归属id < 0:
            return False
        if self.移动计时 < 6:
            self.移动计时 += 1
            return False
        self.移动计时 = 0
        self.移动相位 = 1 - self.移动相位
        return True

    # ---- 获取绘制帧索引 ----
    def 取绘制帧(self, 移动中):
        相位 = self.移动相位 if 移动中 else 0
        if self.是否死亡:
            return 37 + 相位
        return (45 + 相位) if self.游戏.是水(self.地图x, self.地图y) else 数据表.部队图像序号(self.兵种) + (相位 * 5)

    # ---- 朝某方向移动 ----
    def 移到方向(self, 方向序号, 屏幕):
        if ((self.方向掩码 >> 方向序号) & 1) == 0:
            return True
        x = self.地图x + 数据表.方向偏移[方向序号][0]
        y = self.地图y + 数据表.方向偏移[方向序号][1]
        a单位 = self.游戏.取部队(self.游戏.取部队id(x, y), False)
        if a单位 is not None:
            self.方向掩码 = 0
            self.攻击部队(a单位, 屏幕)
            return False
        城 = self.游戏.取城市(self.游戏.取城市所属(x, y), False)
        if 城 is None:
            self.移动到(x, y, 屏幕)
            物体 = self.游戏.取物体类型(x, y)
            if 物体 <= 0 or 物体 > 5:
                self.攻击冷却 -= 1
            else:
                self.攻击冷却 = 0
            return self.攻击冷却 > 0
        if 城.势力序号 != self.势力序号:
            self.方向掩码 = 0
            self.攻击城市(城, 屏幕)
            return False
        if self.游戏.显示对话框("部队", "回城的部队将被解散，你确认吗?", True, 1) != 1:
            return True
        self.移动到(x, y, 屏幕)
        self.阵亡(屏幕)
        return False

    # ---- 绘制移动方向箭头 ----
    def 绘制箭头(self, 图形):
        if self.方向掩码 == 0:
            return
        self.游戏.取光标屏幕位置(self.地图x, self.地图y, self.游戏.光标位置, True)
        x = self.游戏.光标位置[0]
        y = self.游戏.光标位置[1]
        for i in range(len(数据表.箭头偏移)):
            if ((self.方向掩码 >> i) & 1) != 0:
                self.游戏.绘制精灵(图形, x + ((数据表.箭头偏移[i][0] * (5 + self.移动相位)) // 5),
                                   y + ((数据表.箭头偏移[i][1] * (5 + self.移动相位)) // 5),
                                   56 + i, None, True)
        if self.目标部队 is not None:
            self.游戏.绘制精灵(图形, x, y, 60, None, True)

    # ---- 绘制部队 ----
    def 绘制部队(self, 图形, x, y):
        iArr = [0, 0, 0, 0]
        if self.游戏.绘制精灵(图形, x, y, self.取绘制帧(True), iArr, True) != 0 and iArr[1] >= self.游戏.屏幕偏移y:
            px = x - 12
            py = y + 6
            iArr[1] = iArr[1] - 5
            iArr[2] = self.游戏.精灵帧[self.取绘制帧(False)][2]
            self.游戏.绘制精灵(图形, iArr[0], iArr[1], 48 if self.攻击冷却 == 0 else 47, None, True)
            iArr[0] = iArr[0] + 7
            iArr[2] = iArr[2] - 7
            长 = (iArr[2] * self.最大兵力) // self.初始最大兵力
            图形.设置颜色(数据表.势力颜色[self.势力序号])
            图形.填充矩形(iArr[0], iArr[1] + 2, 长, 2)
            图形.设置颜色(0)
            图形.画矩形(iArr[0], iArr[1] + 1, iArr[2], 3)
            if self.显示动画:
                self.游戏.绘制精灵(图形, 部队.屏幕x, 部队.屏幕y, 部队.动画帧, None, True)
            if self.伤害加成 != 0:
                游戏界面.绘制文本("+" + str(self.伤害加成 & 65535), px - 8, py - (self.伤害加成 >> 24),
                                   图形, 16711680, 16777215)

    # ---- 存档加载 ----
    def 从流加载(self, 流):
        self.归属id = 流.读int()
        self.方向掩码 = 0
        if self.归属id < 0:
            return
        位 = 流.读int()
        self.是否死亡 = (位 & 1) != 0
        self.显示动画 = ((位 >> 2) & 1) != 0
        self.攻击冷却 = 流.读int()
        self.移动相位 = 0
        self.移动计时 = self.部队id % 6
        self.地图x = 流.读int()
        self.地图y = 流.读int()
        self.最大兵力 = 流.读int()
        self.初始最大兵力 = 流.读int()
        self.当前兵力 = 流.读int()
        self.兵种 = 流.读int()
        self.势力序号 = 流.读int()
        self.经验 = 流.读int()
        self.等级 = 流.读int()
        if self.是否死亡:
            self.移动目标x = 流.读int()
            self.建造进度 = 流.读int()
            self.移动目标y = 流.读int()

    # ---- 存档保存 ----
    def 存到流(self, 流):
        流.写int(self.归属id)
        if self.归属id < 0:
            return
        流.写int((1 if self.是否死亡 else 0) | (4 if self.显示动画 else 0))
        流.写int(self.攻击冷却)
        流.写int(self.地图x)
        流.写int(self.地图y)
        流.写int(self.最大兵力)
        流.写int(self.初始最大兵力)
        流.写int(self.当前兵力)
        流.写int(self.兵种)
        流.写int(self.势力序号)
        流.写int(self.经验)
        流.写int(self.等级)
        if self.是否死亡:
            流.写int(self.移动目标x)
            流.写int(self.建造进度)
            流.写int(self.移动目标y)

    # ---- 初始化部队 ----
    def 初始化部队(self, 最大兵力, 当前兵力, 兵种, 城市id, 势力序号):
        self.目标部队 = None
        self.方向掩码 = 0
        self.等级 = 1
        self.经验 = 0
        self.是否死亡 = False
        self.显示动画 = False
        self.移动相位 = 0
        self.移动计时 = self.部队id % 6
        self.最大兵力 = 最大兵力
        self.初始最大兵力 = self.最大兵力
        self.当前兵力 = 当前兵力
        self.兵种 = 兵种
        self.归属id = 城市id
        self.势力序号 = 势力序号
        self.攻击冷却 = 数据表.兵种属性[self.兵种][2]

    # ---- 开始建造（民工/移民） ----
    def 开始建造(self, 建筑类型, 城):
        self.是否死亡 = True
        self.攻击冷却 = 0
        self.移动目标x = 建筑类型
        物体 = self.游戏.取物体类型(self.地图x, self.地图y)
        if 物体 > 0 and 数据表.建筑索引(物体) >= 0:
            索引 = 数据表.建筑索引(物体)
            建筑 = 城.建筑
            建筑[索引] = 建筑[索引] - 1
        self.移动目标y = 城.取建筑瓦片id(建筑类型)
        self.建造进度 = 数据表.建筑造价[建筑类型]
        属性 = 城.属性
        属性[6] = 属性[6] - (self.建造进度 // 5)
        self.游戏.设置物体类型(self.地图x, self.地图y, 50)

    # ---- 计算攻击所需回合数 ----
    def 取攻击回合数(self, 目标防御):
        攻 = self.计算攻击()
        return ((目标防御 + 攻) - 1) // 攻

    # ---- 显示治疗/恢复特效 ----
    def 显示治疗特效(self, 治疗量, 屏幕):
        self.伤害加成 = 治疗量
        self.最大兵力 += 治疗量
        self.当前兵力 += 治疗量
        if self.当前兵力 > self.最大兵力:
            self.当前兵力 = self.最大兵力
        for i in range(6):
            屏幕.重绘屏幕()
            self.伤害加成 &= 65535
            self.伤害加成 |= (i + 10) << 24
            屏幕.睡眠(50)
        self.伤害加成 = 0

    # ---- 部队每回合更新（治疗、建造、探索等） ----
    def 更新部队(self, 屏幕):
        self.游戏.移动光标(self.地图x, self.地图y)
        城 = self.游戏.取城市(self.归属id, False)
        if 城 is not None and 城.资源[3] > 0:
            治疗 = (self.初始最大兵力 * 城.资源[3]) // 20
            if 治疗 + self.最大兵力 > self.初始最大兵力:
                治疗 = self.初始最大兵力 - self.最大兵力
            if 治疗 > 0:
                self.显示治疗特效(治疗, 屏幕)
        if self.是否死亡:
            self.建造进度 -= self.计算攻击()
            城2 = self.游戏.取城市(self.归属id, False)
            self.当前兵力 += 100
            if self.当前兵力 > self.最大兵力:
                self.当前兵力 = self.最大兵力
            if self.建造进度 > 0:
                return
            self.建造进度 = 0
            if self.移动目标x == 7:  # 搜索新特产
                随机 = self.游戏.随机数(20)
                if 随机 < len(数据表.特产名):
                    self.游戏.显示对话框("好消息", "经过长时间探索，在" + 城2.城名 +
                                          "附近发现了新特产:" + 数据表.特产名[随机], True, 0)
                    self.游戏.设置物体类型(self.地图x, self.地图y, 随机 + 6)
                    资源 = 城2.资源
                    资源[随机] = 资源[随机] + 1
                else:
                    self.游戏.设置物体类型(self.地图x, self.地图y, 0)
                    self.游戏.显示对话框("提示", str(城2.城名) + "附近漫长的探索探索结束了，耗资巨大但一无所获", True, 0)
            else:
                if self.游戏.取归属id(self.地图x, self.地图y) == 城2.归属id:
                    建筑 = 城2.建筑
                    建筑类型 = self.移动目标x
                    建筑[建筑类型] = 建筑[建筑类型] + 1
                self.游戏.设置物体类型(self.地图x, self.地图y, self.移动目标y)
            self.是否死亡 = False

    # ---- 寻找最优移动方向（AI寻路） ----
    def 寻找最优先方向(self, 目标x, 目标y):
        最优 = -1
        最短 = -1
        for i in range(len(数据表.方向偏移)):
            if ((self.方向掩码 >> i) & 1) != 0:
                x = self.地图x + 数据表.方向偏移[i][0]
                y = self.地图y + 数据表.方向偏移[i][1]
                城 = self.游戏.取城市(self.游戏.取城市所属(x, y), False)
                if (self.游戏.取城市所属(x, y) == 0 or 城 is None or 城.势力序号 != self.势力序号):
                    曼哈顿 = abs(x - 目标x) + abs(y - 目标y)
                    if 最短 < 0 or 曼哈顿 < 最短:
                        最优 = i
                        最短 = 曼哈顿
        return 最优

    # ---- 攻击当前锁定的目标 ----
    def 攻击目标(self, 屏幕):
        if self.目标部队 is None:
            return False
        self.攻击部队(self.目标部队, 屏幕)
        self.攻击冷却 = 0
        return True

    # ---- 向目标位置移动（AI自动移动） ----
    def 移到目标(self, 目标x, 目标y, 屏幕):
        while self.攻击冷却 > 0 and self.更新移动(False):
            if self.目标部队 is not None:
                self.方向掩码 = 0
                self.攻击部队(self.目标部队, 屏幕)
                self.目标部队 = None
                return
            else:
                i = self.寻找最优先方向(目标x, 目标y)
                if i < 0:
                    return
                if not self.移到方向(i, 屏幕):
                    self.游戏.处理部队回合(self, False)
                self.方向掩码 = 0

    # ---- 移动到指定位置（逐格移动） ----
    def 移动到(self, 目标x, 目标y, 屏幕):
        self.游戏.移动光标(self.地图x, self.地图y)
        屏幕.重绘屏幕()
        while True:
            if 目标x == self.地图x and 目标y == self.地图y:
                return
            if self.游戏.取部队id(self.地图x, self.地图y) == self.部队id:
                self.游戏.设置部队id(self.地图x, self.地图y, 0)
            if self.地图x < 目标x:
                self.地图x += 1
            elif self.地图x > 目标x:
                self.地图x -= 1
            if self.地图y > 目标y:
                self.地图y -= 1
            elif self.地图y < 目标y:
                self.地图y += 1
            if self.游戏.取部队id(self.地图x, self.地图y) == 0:
                self.游戏.设置部队id(self.地图x, self.地图y, self.部队id)
            self.游戏.移动光标(self.地图x, self.地图y)
            self.移动相位 = 1 - self.移动相位
            屏幕.重绘屏幕()
            屏幕.睡眠(50)


# ===== 静态表再导出（对应原 Unit 的 static 成员，供其它模块以 部队.xx 访问）=====
兵种属性 = 数据表.兵种属性
兵种名 = 数据表.兵种名
兵种资源需求 = 数据表.兵种资源需求
军衔名 = 数据表.军衔名
训练优先级 = 数据表.训练优先级
方向偏移 = 数据表.方向偏移
箭头偏移 = 数据表.箭头偏移
部队图像序号 = 数据表.部队图像序号
