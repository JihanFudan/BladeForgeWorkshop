# -*- coding: utf-8 -*-
"""渲染寒霜刀柄与刀镡接缝近景，检查新增封顶面是否封住空腔。"""
from pathlib import Path
from PIL import Image

import preview_frost_blade as preview

ROOT = Path(__file__).resolve().parent.parent
OBJ = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost/frost.obj"
TEX = ROOT / "src/main/resources/assets/slashbladeresh_slashblad/model/named/frost/frost.png"
OUT = ROOT / "model-previews/frost_handle_cap_closeup.png"


def load_closeup():
    vertices, uvs, faces = [], [], []
    group = ""
    for line in OBJ.read_text(encoding="utf-8").splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == "v":
            vertices.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "vt":
            uvs.append(tuple(map(float, fields[1:3])))
        elif fields[0] == "g":
            group = fields[1]
        elif fields[0] == "f" and group in {"handle", "guard"}:
            vi = tuple(int(token.split("/")[0]) for token in fields[1:4])
            ti = tuple(int(token.split("/")[1]) for token in fields[1:4])
            points = [vertices[index - 1] for index in vi]
            if max(point[0] for point in points) <= -20.0:
                faces.append((vi, ti))
    return vertices, uvs, faces


def main():
    geometry = load_closeup()
    texture = Image.open(TEX).convert("RGBA")
    left = preview.render(*geometry, texture, -1.35, -0.12, 0.2, (600, 600))
    right = preview.render(*geometry, texture, 1.35, 0.12, -0.2, (600, 600))
    canvas = Image.new("RGB", (1200, 600), (15, 15, 20))
    canvas.paste(left, (0, 0))
    canvas.paste(right, (600, 0))
    canvas.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
