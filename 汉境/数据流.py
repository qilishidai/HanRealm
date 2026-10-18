# -*- coding: utf-8 -*-
"""数据流 —— 对应原 java.io.DataInputStream / DataOutputStream。

存档/读档用二进制流，与 Java 写入顺序一致。此类封装字节数组，
提供 读int/写int、读bool/写bool、读utf/写utf。
"""

import struct
import io


class 输出流:
    def __init__(self):
        self._buf = io.BytesIO()

    def 写int(self, 值):
        # Java int 是有符号 32 位；地图状态可能使用最高位作为临时标记。
        # Python 先按 32 位截断，再转换到 struct 的有符号取值范围。
        值 &= 0xFFFFFFFF
        if 值 >= 0x80000000:
            值 -= 0x100000000
        self._buf.write(struct.pack(">i", 值))

    def 写bool(self, 值):
        self._buf.write(b"\x01" if 值 else b"\x00")

    def 写utf(self, 文本):
        数据 = 文本.encode("utf-8")
        self._buf.write(struct.pack(">H", len(数据)))
        self._buf.write(数据)

    def 取字节(self) -> bytes:
        return self._buf.getvalue()


class 输入流:
    def __init__(self, 字节):
        self._buf = io.BytesIO(字节)

    def 读int(self) -> int:
        值 = self._buf.read(4)
        if len(值) < 4:
            return 0
        return struct.unpack(">i", 值)[0]

    def 读bool(self) -> bool:
        return self._buf.read(1) != b"\x00"

    def 读utf(self) -> str:
        n = struct.unpack(">H", self._buf.read(2))[0]
        return self._buf.read(n).decode("utf-8")
