# -*- coding: utf-8 -*-
"""关卡数据 —— 解析原 res/data.png（实为二进制关卡包）。

文件结构（均大端）:
  头部:
    int 关卡数
    int[关卡数] 偏移表（本移植不依赖，直接顺序读取更稳妥）
  每个关卡块（顺序存放）:
    int 城名数
    城名[]  （int 长度2字节 + UTF-8 字节）
    int 任务标题行数=2
    任务行[2]（UTF）
    int 宽
    int 高
    int[宽*高] 地图数据（逐格位打包）

实测第 1 关数据从文件偏移 28（即 4 + 关卡数*4 = 跳过整个头部）开始，
六关顺序紧密相连。
"""

import struct
import io

import 图片


class 关卡:
    def __init__(self):
        self.城名 = []
        self.任务行 = []
        self.宽 = 0
        self.高 = 0
        self.地图数据 = []  # 二维 int 数组 [高][宽]


class 关卡包:
    """读取整个关卡文件，可逐关取用。"""

    def __init__(self):
        self.关卡 = []

    def 读取(self):
        路径 = 图片.相对路径("/data.png")
        with open(路径, "rb") as f:
            原始 = f.read()
        流 = io.BytesIO(原始)

        def 读int():
            return struct.unpack(">i", 流.read(4))[0]

        def 读utf():
            n = struct.unpack(">H", 流.read(2))[0]
            return 流.read(n).decode("utf-8")

        关卡数 = 读int()
        偏移表 = [读int() for _ in range(关卡数)]
        # 首关数据紧跟头部（4 + 关卡数*4）之后
        流.seek(4 + (关卡数 * 4))

        for _ in range(关卡数):
            关 = 关卡()
            城名数 = 读int()
            关.城名 = [读utf() for _ in range(城名数)]
            # 任务行为 2 行，直接紧跟城名数组，无独立计数
            row1 = 读utf()
            row2 = 读utf()
            关.任务行 = [row1, row2]
            关.宽 = 读int()
            关.高 = 读int()
            地图 = [[0 for _ in range(关.宽)] for _ in range(关.高)]
            for yy in range(关.高):
                for xx in range(关.宽):
                    地图[yy][xx] = 读int() & 0x7FFFFFFF
            关.地图数据 = 地图
            self.关卡.append(关)
        return self


# 全局单例
全局关卡包 = None


def 获取关卡包():
    global 全局关卡包
    if 全局关卡包 is None:
        全局关卡包 = 关卡包()
        全局关卡包.读取()
    return 全局关卡包
