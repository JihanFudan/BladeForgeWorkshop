# -*- coding: utf-8 -*-
"""Finish the remaining UTF-8 mixin replacements."""
def patch(path, pairs, enc='utf-8'):
    data = open(path, 'rb').read()
    for a, b in pairs:
        ab = a.encode(enc)
        bb = b.encode(enc)
        if ab not in data:
            print('MISS:', a[:70])
            continue
        data = data.replace(ab, bb)
    open(path, 'wb').write(data)
    print('patched', path)

base = r'C:\Users\JihanFudan\Desktop\mod\src\main\java\cn\blockforge\generated\slashbladereshslashblad'
patch(base + r'\mixin\UltimatePlayerPoseMixin.java', [
    ("sweep = Mth.sin(Mth.clamp((frame - 174.0f) / 14.0f, 0.0f, 1.0f) * Mth.PI);    // \u72d0\u5f0f\u6a2a\u626b 174..188",
     "sweep = Mth.sin(Mth.clamp((frame - 104.0f) / 14.0f, 0.0f, 1.0f) * Mth.PI);    // \u72d0\u5f0f\u6a2a\u626b 104..118"),
    ("sweep = Mth.sin(Mth.clamp((frame - 156.0f) / 14.0f, 0.0f, 1.0f) * Mth.PI);    // \u8f7b\u7075\u7ffb\u8f6c 156..170",
     "sweep = Mth.sin(Mth.clamp((frame - 104.0f) / 14.0f, 0.0f, 1.0f) * Mth.PI);    // \u8f7b\u7075\u7ffb\u8f6c 104..118"),
    ("slash = Mth.sin(Mth.clamp((frame - 174.0f) / 12.0f, 0.0f, 1.0f) * Mth.PI)\n                  + Mth.sin(Mth.clamp((frame - 198.0f) / 10.0f, 0.0f, 1.0f) * Mth.PI);    // \u4e24\u8bb0\u7ec3\u4e60\u6325\u780d 178/202",
     "slash = Mth.sin(Mth.clamp((frame - 104.0f) / 12.0f, 0.0f, 1.0f) * Mth.PI)\n                  + Mth.sin(Mth.clamp((frame - 136.0f) / 10.0f, 0.0f, 1.0f) * Mth.PI);    // \u4e24\u8bb0\u7ec3\u4e60\u6325\u780d 104/136"),
    ("slash = Mth.sin(Mth.clamp((frame - 174.0f) / 12.0f, 0.0f, 1.0f) * Mth.PI);    // \u4e00\u8bb0\u8106\u780d 178",
     "slash = Mth.sin(Mth.clamp((frame - 100.0f) / 12.0f, 0.0f, 1.0f) * Mth.PI);    // \u4e00\u8bb0\u8106\u780d 100"),
    ("bob = Mth.sin(Mth.clamp((frame - 108.0f) / 96.0f, 0.0f, 1.0f) * Mth.PI * 4.0f) * 0.06f; // \u7ffb\u817e\u8f7b\u98a4",
     "bob = Mth.sin(Mth.clamp((frame - 120.0f) / 48.0f, 0.0f, 1.0f) * Mth.PI * 4.0f) * 0.06f; // \u7ffb\u817e\u8f7b\u98a4"),
    ("float align = smooth(frame, 198.0f, 216.0f);",
     "float sheathe = smooth(frame, 150.0f, 206.0f);"),
    ("float insert = smooth(frame, 216.0f, 240.0f);",
     "float slide = smooth(frame, 206.0f, 230.0f);"),
])
print('done')
