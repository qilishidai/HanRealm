#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""HanRealm 存档修改器。

默认读取项目根目录的 china.dat，并写入 china_mod.dat；原存档不会被覆盖。
示例：
  python 存档修改器.py --list
  python 存档修改器.py --unlock-level 6 --output china_test.dat
  python 存档修改器.py --city-resource 1 0 999999 --unit-force 1 99999
"""

import argparse
import os
import sys


根目录 = os.path.dirname(os.path.abspath(__file__))
包目录 = os.path.join(根目录, "汉境")
sys.path.insert(0, 包目录)

# 修改器不需要显示窗口或播放声音。
os.environ.setdefault("SDL_VIDEODRIVER", "dummy")
os.environ.setdefault("SDL_AUDIODRIVER", "dummy")

import 图形引擎
import 游戏
import 游戏界面


def 存档基名(路径):
    """游戏存档接口使用不带 .dat 的基名。"""
    return os.path.splitext(os.path.abspath(路径))[0]


def 创建上下文(输入文件):
    图形引擎.初始化("HanRealm Save Editor")
    界面 = 游戏界面.主界面()
    界面.存档名 = 存档基名(输入文件)
    界面.初始化游戏()
    if not 界面.加载游戏(True):
        raise RuntimeError("无法读取存档: " + 输入文件)
    # 加载函数保留主菜单状态；强制进入游戏状态，确保保存完整游戏数据。
    界面.菜单状态 = 22
    return 界面, 界面.游戏


def 显示摘要(界面, 游戏对象):
    print("当前关卡:", 游戏.当前关卡)
    print("地图尺寸:", len(游戏对象.地图数据[0]), "x", len(游戏对象.地图数据))
    print("城市:", 游戏对象.城市菜单索引, "部队:", 游戏对象.城市信息索引)
    for i in range(游戏对象.城市菜单索引):
        城 = 游戏对象.城市们[i]
        print("  城市 %d: %s  势力=%d  科技=%d  资源=%s" %
              (i + 1, 城.城名, 城.势力序号, 城.科技等级, 城.资源))
    for i in range(游戏对象.城市信息索引):
        单位 = 游戏对象.部队们[i]
        if 单位.归属id > 0:
            print("  部队 %d: 城市=%d  兵种=%d  兵力=%d/%d  阵亡=%s" %
                  (i + 1, 单位.归属id, 单位.兵种, 单位.当前兵力,
                   单位.最大兵力, 单位.是否死亡))


def 修改(游戏对象, 参数):
    if 参数.unlock_level is not None:
        游戏.当前关卡 = max(1, min(6, 参数.unlock_level))
    if 参数.battle_mode is not None:
        游戏.战斗模式 = 参数.battle_mode
    for 城市编号, 资源编号, 数值 in 参数.city_resource:
        if not 1 <= 城市编号 <= 游戏对象.城市菜单索引:
            raise ValueError("城市编号超出范围: %d" % 城市编号)
        城 = 游戏对象.城市们[城市编号 - 1]
        if not 0 <= 资源编号 < len(城.资源):
            raise ValueError("资源编号超出范围: %d" % 资源编号)
        城.资源[资源编号] = max(0, 数值)
    for 部队编号, 兵力 in 参数.unit_force:
        if not 1 <= 部队编号 <= 游戏对象.城市信息索引:
            raise ValueError("部队编号超出范围: %d" % 部队编号)
        单位 = 游戏对象.部队们[部队编号 - 1]
        单位.当前兵力 = max(0, min(兵力, 单位.最大兵力))
        单位.是否死亡 = False
    for 部队编号 in 参数.unit_heal:
        if not 1 <= 部队编号 <= 游戏对象.城市信息索引:
            raise ValueError("部队编号超出范围: %d" % 部队编号)
        单位 = 游戏对象.部队们[部队编号 - 1]
        单位.当前兵力 = 单位.最大兵力
        单位.是否死亡 = False
        单位.建造进度 = 0


def 主程序():
    解析器 = argparse.ArgumentParser(description="HanRealm china.dat 存档修改器")
    解析器.add_argument("--input", default="china.dat", help="输入存档")
    解析器.add_argument("--output", default="china_mod.dat", help="输出存档")
    解析器.add_argument("--list", action="store_true", help="显示存档摘要")
    解析器.add_argument("--unlock-level", type=int, help="解锁到第几关（1-6）")
    解析器.add_argument("--battle-mode", type=lambda x: x.lower() in ("1", "true", "yes"), help="指挥战斗: true/false")
    解析器.add_argument("--city-resource", nargs=3, action="append", type=int, metavar=("城市", "资源", "数量"), default=[])
    解析器.add_argument("--unit-force", nargs=2, action="append", type=int, metavar=("部队", "兵力"), default=[])
    解析器.add_argument("--unit-heal", nargs="*", type=int, metavar="部队", default=[])
    参数 = 解析器.parse_args()
    try:
        界面, 游戏对象 = 创建上下文(参数.input)
        if 参数.list or not any((参数.unlock_level is not None, 参数.battle_mode is not None,
                                  参数.city_resource, 参数.unit_force, 参数.unit_heal)):
            显示摘要(界面, 游戏对象)
        修改(游戏对象, 参数)
        if any((参数.unlock_level is not None, 参数.battle_mode is not None,
                参数.city_resource, 参数.unit_force, 参数.unit_heal)):
            界面.存档名 = 存档基名(参数.output)
            界面.保存游戏()
            print("已写入:", os.path.abspath(参数.output))
    except (OSError, RuntimeError, ValueError) as 异常:
        print("错误:", 异常, file=sys.stderr)
        return 1
    finally:
        try:
            import pygame
            pygame.quit()
        except Exception:
            pass
    return 0


if __name__ == "__main__":
    raise SystemExit(主程序())
