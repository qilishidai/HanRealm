# -*- coding: utf-8 -*-
"""图片工具 —— 对应原 J2ME 的 Image 类（Image.createImage）。

把 res 下的图片资源加载进内存，并提供与 Midp 坐标一致的绘制入口。
"""

import os

# 资源目录（相对本包）
资源目录 = os.path.join(os.path.dirname(__file__), "资源")


def 相对路径(名称: str):
    """把 J2ME 资源名(如 '/map.png')映射为本包资源目录下的文件。"""
    基名 = 名称.lstrip("/")
    # 中文替代名映射
    映射 = {
        "map.png": "地图.png",
        "objs.png": "精灵.png",
        "mmm.png": "界面瓦片.png",
        "mt.png": "启动画面.png",
        "st.png": "结束画面.png",
        "data.png": "关卡数据.png",
    }
    实际 = 映射.get(基名, 基名)
    return os.path.join(资源目录, 实际)


class 图片:
    """包装一个 pygame Surface，提供 getWidth/getHeight。"""

    def __init__(self, surface):
        self.表面 = surface

    def 取宽度(self):
        return self.表面.get_width()

    def 取高度(self):
        return self.表面.get_height()


_缓存 = {}


def 创建图片(名称: str) -> 图片:
    """Image.createImage 等价物，带缓存。"""
    if 名称 in _缓存:
        return _缓存[名称]
    import 图形引擎
    面 = 图形引擎.加载图像(相对路径(名称))
    图片对象 = 图片(面)
    _缓存[名称] = 图片对象
    return 图片对象
