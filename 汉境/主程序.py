# -*- coding: utf-8 -*-
"""汉境 —— 入口。

用法：在 汉境 目录下执行  python 主程序.py
运行后出现 176x208 逻辑屏幕、放大 2 倍（352x416）的窗口。
"""

import os
import sys

# 允许无显示环境（CI/调试）
if os.environ.get("汉境无显示"):
    os.environ.setdefault("SDL_VIDEODRIVER", "dummy")

import 游戏界面


def main():
    界面 = 游戏界面.主界面()
    界面.开始()


if __name__ == "__main__":
    main()
