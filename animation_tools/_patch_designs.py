# -*- coding: utf-8 -*-
import io
p = r'C:\Users\JihanFudan\Desktop\mod\animation_tools\designs.py'
s = io.open(p, encoding='utf-8').read()
repls = [
    ("(34, (P_GRIP[0][0] + 1.4, P_GRIP[0][1] + 0.1, P_GRIP[0][2] + 0.1))",
     "(34, ((P_GRIP[0][0] + 1.4, P_GRIP[0][1] + 0.1, P_GRIP[0][2] + 0.1), P_GRIP[1]))"),
    ("(36, (P_GRIP[0][0] + 1.3, P_GRIP[0][1], P_GRIP[0][2]))",
     "(36, ((P_GRIP[0][0] + 1.3, P_GRIP[0][1], P_GRIP[0][2]), P_GRIP[1]))"),
    ("(31, (P_GRIP[0][0] + 1.5, P_GRIP[0][1], P_GRIP[0][2]))",
     "(31, ((P_GRIP[0][0] + 1.5, P_GRIP[0][1], P_GRIP[0][2]), P_GRIP[1]))"),
    ("(38, (P_GRIP[0][0] + 1.2, P_GRIP[0][1], P_GRIP[0][2]))",
     "(38, ((P_GRIP[0][0] + 1.2, P_GRIP[0][1], P_GRIP[0][2]), P_GRIP[1]))"),
    ("(37, (P_GRIP[0][0] + 1.3, P_GRIP[0][1], P_GRIP[0][2]))",
     "(37, ((P_GRIP[0][0] + 1.3, P_GRIP[0][1], P_GRIP[0][2]), P_GRIP[1]))"),
    ("(29, (P_GRIP[0][0] + 1.5, P_GRIP[0][1], P_GRIP[0][2]))",
     "(29, ((P_GRIP[0][0] + 1.5, P_GRIP[0][1], P_GRIP[0][2]), P_GRIP[1]))"),
]
for old, new in repls:
    assert old in s, old
    s = s.replace(old, new)
io.open(p, 'w', encoding='utf-8').write(s)
print("patched OK")
