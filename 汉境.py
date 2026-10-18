#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""汉境 —— 启动脚本。

把 汉境 目录加入模块路径后运行 主程序.main()。
用法:
    python 汉境.py
"""

import os
import sys

# 把汉境 包目录（本脚本同级的 汉境/ 子目录）加入模块搜索路径
脚本目录 = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(脚本目录, "汉境"))

# 允许无显示环境（如 CI 测试）：设置环境变量 汉境无显示=1
if os.environ.get("汉境无显示"):
    os.environ.setdefault("SDL_VIDEODRIVER", "dummy")

import 主程序

if __name__ == "__main__":
    主程序.main()
