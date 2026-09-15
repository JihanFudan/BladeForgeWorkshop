# -*- coding: utf-8 -*-
"""Binary-safe ASCII patch for the two GBK-encoded Java files."""
import io

def patch(path, pairs, enc='gbk'):
    data = open(path, 'rb').read()
    for a, b in pairs:
        ab = a.encode(enc)
        bb = b.encode(enc)
        if ab not in data:
            print('MISS in', path, ':', a[:60])
            continue
        data = data.replace(ab, bb)
    open(path, 'wb').write(data)
    print('patched', path)

base = r'C:\Users\JihanFudan\Desktop\mod\src\main\java\cn\blockforge\generated\slashbladereshslashblad'

# ---- InspectComboStates.java : sound cue frames (already applied, keep as no-op)
if False:
    patch(base + r'\InspectComboStates.java', [
    # generic A
    ("19, Cue.HOLD, 40, Cue.SHED, 76, Cue.DRAW, 84, Cue.SLASH, 150, Cue.SLASH, 210, Cue.SPIN, 266, Cue.SHEATHE",
     "19, Cue.HOLD, 40, Cue.SHED, 60, Cue.DRAW, 84, Cue.SLASH, 104, Cue.SLASH, 144, Cue.SLASH, 156, Cue.SPIN, 248, Cue.SHEATHE"),
    # fox black
    ("19, Cue.HOLD, 40, Cue.SHED, 76, Cue.DRAW, 84, Cue.SLASH, 178, Cue.SLASH, 205, Cue.SPIN, 266, Cue.SHEATHE",
     "19, Cue.HOLD, 40, Cue.SHED, 60, Cue.DRAW, 84, Cue.SLASH, 104, Cue.SLASH, 138, Cue.SPIN, 230, Cue.SHEATHE"),
    # fox white
    ("19, Cue.HOLD, 40, Cue.SHED, 76, Cue.DRAW, 84, Cue.SLASH, 160, Cue.SLASH, 198, Cue.SPIN, 266, Cue.SHEATHE",
     "19, Cue.HOLD, 40, Cue.SHED, 60, Cue.DRAW, 84, Cue.SLASH, 104, Cue.SLASH, 134, Cue.SPIN, 226, Cue.SHEATHE"),
    # wood
    ("19, Cue.HOLD, 40, Cue.SHED, 76, Cue.DRAW, 84, Cue.SLASH, 178, Cue.SLASH, 202, Cue.SLASH, 220, Cue.SPIN, 266, Cue.SHEATHE",
     "19, Cue.HOLD, 40, Cue.SHED, 60, Cue.DRAW, 84, Cue.SLASH, 104, Cue.SLASH, 136, Cue.SLASH, 152, Cue.SPIN, 244, Cue.SHEATHE"),
    # bamboo
    ("19, Cue.HOLD, 40, Cue.SHED, 76, Cue.DRAW, 84, Cue.SLASH, 138, Cue.SLASH, 178, Cue.SLASH, 200, Cue.SPIN, 266, Cue.SHEATHE",
     "19, Cue.HOLD, 40, Cue.SHED, 60, Cue.DRAW, 84, Cue.SLASH, 100, Cue.SLASH, 148, Cue.SPIN, 240, Cue.SHEATHE"),
    # muramasa
    ("19, Cue.HOLD, 40, Cue.SHED, 76, Cue.DRAW, 84, Cue.SLASH, 140, Cue.SLASH, 206, Cue.SPIN, 266, Cue.SHEATHE",
     "19, Cue.HOLD, 40, Cue.SHED, 60, Cue.DRAW, 84, Cue.SLASH, 168, Cue.SPIN, 260, Cue.SHEATHE"),
    # yamato
    ("19, Cue.HOLD, 88, Cue.NUDGE, 104, Cue.DRAW, 120, Cue.SLASH, 240, Cue.SHEATHE",
     "19, Cue.HOLD, 48, Cue.NUDGE, 64, Cue.DRAW, 80, Cue.SLASH, 206, Cue.SHEATHE"),
    ])

# ---- UltimatePlayerPoseMixin.java : arm timeline tables ----
patch(base + r'\mixin\UltimatePlayerPoseMixin.java', [
    # events table
    ("case WOOD -> new float[]{19, 76, 110, 206, 220, 270, 292};",
     "case WOOD -> new float[]{19, 60, 84, 152, 152, 244, 256};"),
    ("case BAMBOO -> new float[]{19, 76, 108, 178, 200, 250, 272};",
     "case BAMBOO -> new float[]{19, 60, 84, 148, 148, 240, 252};"),
    ("case MURAMASA -> new float[]{19, 76, 108, 204, 206, 256, 278};",
     "case MURAMASA -> new float[]{19, 60, 84, 168, 168, 260, 272};"),
    ("case FOX_BLACK -> new float[]{19, 76, 108, 194, 205, 255, 277};",
     "case FOX_BLACK -> new float[]{19, 60, 84, 138, 138, 230, 242};"),
    ("case FOX_WHITE -> new float[]{19, 76, 106, 176, 198, 248, 270};",
     "case FOX_WHITE -> new float[]{19, 60, 84, 134, 134, 226, 238};"),
    ("default -> new float[]{19, 76, 96, 194, 210, 260, 282};",
     "default -> new float[]{19, 60, 84, 156, 156, 248, 260};"),
    # insert window
    ("float insert = smooth(frame, rotEnd + 10, settle - 4);",
     "float insert = smooth(frame, rotEnd, settle);"),
    # per-blade show motions
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
    # generic hold arm values
    ("if (wood || bamboo) {\n            hX = -1.55f; hY = -0.15f; hZ = 0.15f;\n            lX = -1.00f; lY = 0.25f; lZ = -0.05f;\n        } else {\n            hX = -1.25f; hY = -0.55f; hZ = 0.45f;\n            lX = -0.55f; lY = 0.60f; lZ = -0.30f;\n        }\n        if (muramasa) { hX = -1.30f; hY = -0.80f; hZ = 0.70f; }",
     "if (wood || bamboo) {\n            hX = -1.35f; hY = -0.10f; hZ = 0.25f;\n            lX = -0.90f; lY = 0.35f; lZ = -0.10f;\n        } else {\n            hX = -1.15f; hY = -0.25f; hZ = 0.30f;\n            lX = -0.65f; lY = 0.50f; lZ = -0.30f;\n        }\n        if (muramasa) { hX = -1.20f; hY = -0.50f; hZ = 0.55f; }"),
    # yamato arm schedule
    ("float stand = smooth(frame, 19.0f, 36.0f);",
     "float stand = smooth(frame, 16.0f, 32.0f);"),
    ("float holdSheath = smooth(frame, 36.0f, 62.0f) * (1.0f - smooth(frame, 260.0f, 280.0f));",
     "float holdSheath = smooth(frame, 32.0f, 60.0f) * (1.0f - smooth(frame, 230.0f, 250.0f));"),
    ("float push = smooth(frame, 62.0f, 88.0f);",
     "float push = smooth(frame, 32.0f, 48.0f);"),
    ("float draw = smooth(frame, 88.0f, 104.0f);",
     "float draw = smooth(frame, 48.0f, 64.0f);"),
    ("float swing = smooth(frame, 104.0f, 120.0f);",
     "float swing = smooth(frame, 64.0f, 80.0f);"),
    ("float jodan = smooth(frame, 120.0f, 136.0f) * (1.0f - smooth(frame, 176.0f, 198.0f));",
     "float jodan = smooth(frame, 80.0f, 96.0f) * (1.0f - smooth(frame, 150.0f, 176.0f));"),
    ("float align = smooth(frame, 198.0f, 216.0f);\n        float insert = smooth(frame, 216.0f, 240.0f);",
     "float sheathe = smooth(frame, 150.0f, 206.0f);\n        float slide = smooth(frame, 206.0f, 230.0f);"),
    ("float layDown = smooth(frame, 260.0f, 280.0f);",
     "float layDown = smooth(frame, 230.0f, 250.0f);"),
    ("float freeHand = draw + swing + jodan * 0.9f + align + insert;",
     "float freeHand = draw + swing + jodan * 0.9f + sheathe + slide;"),
    ("rX = Mth.lerp(insert, rX, -0.90f);",
     "rX = Mth.lerp(slide, rX, -0.90f);"),
    ("rY = Mth.lerp(insert, rY, -0.55f);",
     "rY = Mth.lerp(slide, rY, -0.55f);"),
    ("rZ = Mth.lerp(insert, rZ, 0.25f);",
     "rZ = Mth.lerp(slide, rZ, 0.25f);"),
    # head/body enma references to align/insert -> sheathe/slide
    ("model.body.yRot = stand * -0.22f + freeHand * 0.10f * (1.0f - insert);",
     "model.body.yRot = stand * -0.22f + freeHand * 0.10f * (1.0f - slide);"),
    ("model.head.xRot += jodan * 0.12f + insert * 0.08f;",
     "model.head.xRot += jodan * 0.12f + slide * 0.08f;"),
])
print('done')
