#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Design 7 inspect animations (311 frames each) and write VMD files.

Render chain (verified against LayerMainBlade + BladeFirstPersonRender 1.21.1
source and the user's in-game video):
  VMD pos -> player eye space:  eye = (0.15*px, 0.125*py - 1.5, -0.125*pz - 0.5)
  depth = 0.125*pz + 0.5
  so design (view space: x right, y up, depth forward) -> VMD:
      px = vx / 0.15,  py = (vy + 1.5) / 0.125,  pz = (depth - 0.5) / 0.125
  Tip dir in eye = (1.2*R00, R10, -R20); front face (model -Z) toward player.
  Note: in the view space the depth is -eye_z; a tip-direction z component
  NEGATIVE points into the screen (away), POSITIVE toward the player.

All 6 normal blades share the same skeleton:
  f0    rest at the left hip (== the "stationary" sheath position)
  f0-19 sheathed blade lifts to horizontal in front of the player
  f19-60 blade slides out along the sheath mouth (draw)
  f60-84 flip over to the show pose
  f84-M per-blade inspection
  M-M+104 rotation-sheathe: the blade swings out right, drops deep and
         sweeps back into the left-hip sheath along a smooth arc
  M+104-311 rest hold
Yamato follows the user's explicit script: standing sheath on the LEFT
(x negative), push up along the sheath axis, draw, forward swing, overhead
jodan, then directly sheathe back into the standing sheath and settle.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vmd_gen import Track, view_to_vmd, build_vmd_tracks, ease_smooth
from vmd_io import write_vmd


def P(pos_view, tip_view, roll=0.0):
    """view-space pose -> (pos_vmd, rot_vmd); front face defaults toward player."""
    return view_to_vmd(pos_view, tip_view, front=None, roll_deg=roll)


def n3(v):
    v = np.array(v, dtype=float)
    return tuple(v / np.linalg.norm(v))


# ---------------------------------------------------------------------------
# Shared key poses (view space: x right, y up, depth forward)
# ---------------------------------------------------------------------------
REST     = ((-0.38, -0.52, 1.12), n3((-0.55, -0.55, -0.63)))   # left-hip rest
PRESENT  = ((-0.20, -0.18, 1.10), n3((0.99, -0.05, -0.086)))    # horizontal in front
DRAW_MID = ((-0.62, -0.15, 1.14), n3((0.99, -0.05, -0.086)))    # half drawn
DRAW_END = ((-1.19, -0.13, 1.19), n3((0.99, -0.05, -0.086)))    # fully drawn, handle left
FLIP_MID = ((-0.55, 0.30, 1.25),  n3((0.20, 0.85, -0.48)))      # blade sweeping up


def shared_draw_A():
    return [
        (0, P(*REST)),
        (19, P(*PRESENT)),
        (40, P(*DRAW_MID)),
        (60, P(*DRAW_END)),
        (72, P(*FLIP_MID)),
    ]


def shared_draw_B():
    return [
        (0, P(*REST)),
        (19, P(*PRESENT)),
        (76, P(*PRESENT)),
        (84, P(*REST)),
        (311, P(*REST)),
    ]


# ---------------------------------------------------------------------------
# Rotation-sheathe arc (offset frames from the show end M; ends at the rest).
# The handle sweeps right-down, drops deep, then sweeps back left into the
# left-hip sheath; the blade rotates from the show tip to the rest tip.
# ---------------------------------------------------------------------------
def arc_sheathe_A(show_end_pos, show_end_tip):
    return [
        (16, ((0.34, -0.32, 1.18), n3((0.55, 0.35, -0.76)))),
        (32, ((0.46, -0.54, 1.20), n3((0.35, 0.65, -0.67)))),
        (48, ((0.36, -0.63, 1.44), n3((0.05, 0.55, -0.83)))),
        (64, ((0.14, -0.61, 1.70), n3((-0.25, 0.20, -0.95)))),
        (80, ((-0.10, -0.57, 1.60), n3((-0.45, -0.10, -0.89)))),
        (96, ((-0.30, -0.54, 1.32), n3((-0.52, -0.40, -0.76)))),
        (104, REST),
    ]


# ---------------------------------------------------------------------------
# Per-blade inspection sections  (84..M) in view-space (pos, tip, roll).
# ---------------------------------------------------------------------------
SHOWS = {
    # generic: the blade circles in the vertical plane in front
    "inspect_a": dict(
        M=156,
        show=[
            (84, ((0.05, 0.10, 1.20), n3((0.15, -0.95, -0.19)), 0)),
            (104, ((0.10, -0.10, 1.30), n3((0.45, 0.60, -0.66)), 0)),
            (124, ((0.00, 0.15, 1.25), n3((0.30, 0.94, -0.16)), 0)),
            (144, ((-0.10, 0.25, 1.20), n3((-0.45, -0.75, -0.49)), 0)),
            (156, ((0.05, 0.10, 1.20), n3((0.15, -0.95, -0.19)), 0)),
        ],
    ),
    # wood: horizontal show + two practice swings
    "inspect_wood": dict(
        M=152,
        show=[
            (84, ((0.00, 0.05, 1.25), n3((0.45, -0.20, -0.87)), 0)),
            (104, ((0.22, 0.38, 1.15), n3((-0.30, 0.75, -0.59)), 0)),
            (120, ((0.05, -0.10, 1.28), n3((0.50, 0.10, -0.86)), 0)),
            (136, ((-0.12, -0.32, 1.22), n3((0.68, 0.28, -0.68)), 0)),
            (152, ((0.00, 0.05, 1.25), n3((0.45, -0.20, -0.87)), 0)),
        ],
    ),
    # bamboo: one crisp diagonal slash, then hold the blade vertical
    "inspect_bamboo": dict(
        M=148,
        show=[
            (84, ((0.05, 0.05, 1.25), n3((0.60, -0.55, -0.58)), 0)),
            (100, ((-0.25, 0.45, 1.15), n3((0.25, 0.80, -0.55)), 0)),
            (116, ((0.00, 0.20, 1.25), n3((0.20, 0.97, -0.13)), 0)),
            (148, ((0.00, 0.20, 1.25), n3((0.20, 0.97, -0.13)), 0)),
        ],
    ),
    # muramasa: vertical blade, slow roll to show both faces, then tilt down
    "inspect_muramasa": dict(
        M=168,
        show=[
            (84, ((0.00, 0.10, 1.25), n3((0.20, 0.97, -0.13)), 0)),
            (120, ((0.00, 0.10, 1.25), n3((0.20, 0.97, -0.13)), 50)),
            (150, ((0.00, 0.10, 1.25), n3((0.20, 0.97, -0.13)), -10)),
            (168, ((-0.05, -0.10, 1.28), n3((-0.40, -0.65, -0.65)), 0)),
        ],
    ),
    # fox_black: horizontal sweep across the view
    "inspect_fox_black": dict(
        M=138,
        show=[
            (84, ((0.05, 0.05, 1.30), n3((0.55, -0.10, -0.83)), 0)),
            (104, ((-0.35, 0.10, 1.25), n3((0.80, 0.35, -0.49)), 0)),
            (128, ((0.20, -0.05, 1.30), n3((-0.35, 0.15, -0.93)), 0)),
            (138, ((0.05, 0.05, 1.30), n3((0.55, -0.10, -0.83)), 0)),
        ],
    ),
    # fox_white: light flip (roll 180) then drop down-left
    "inspect_fox_white": dict(
        M=134,
        show=[
            (84, ((0.00, 0.10, 1.30), n3((0.20, 0.95, -0.24)), 0)),
            (104, ((0.00, 0.10, 1.30), n3((0.20, 0.95, -0.24)), 180)),
            (124, ((-0.05, -0.15, 1.30), n3((-0.35, -0.70, -0.62)), 0)),
            (134, ((-0.05, -0.15, 1.30), n3((-0.35, -0.70, -0.62)), 0)),
        ],
    ),
}


def gen_inspect(name):
    spec = SHOWS[name]
    M = spec["M"]
    A_ev = shared_draw_A()
    for f, (pos, tip, roll) in spec["show"]:
        A_ev.append((f, P(pos, tip, roll)))
    last = spec["show"][-1]
    end_pos, end_tip = last[1][0], last[1][1]
    for d, pose in arc_sheathe_A(end_pos, end_tip):
        A_ev.append((M + d, P(*pose)))
    A_ev.append((311, P(*REST)))
    B_ev = shared_draw_B()
    return build(A_ev, B_ev)


# ---------------------------------------------------------------------------
# Yamato (user's explicit script)
#   standing sheath on the left -> push blade up along the sheath axis ->
#   draw out -> forward swing -> overhead jodan -> direct sheathe
# ---------------------------------------------------------------------------
def gen_inspect_yamato():
    STAND  = ((-0.52, 0.20, 1.10), n3((0.0, -1.0, 0.0)))     # standing sheath, mouth up
    PUSH   = ((-0.52, 0.72, 1.10), n3((0.0, -1.0, 0.0)))     # handle pushed up 0.52
    DRAWN  = ((0.05, 0.30, 1.20),  n3((0.40, 0.35, -0.85)))  # blade out, diagonal
    SWUNG  = ((0.00, 0.05, 1.30),  n3((0.55, -0.10, -0.83))) # forward slash
    JODAN  = ((0.10, 0.60, 1.25),  n3((0.05, -0.45, -0.89))) # overhead stance
    ARC1   = ((0.05, 0.30, 1.25),  n3((0.10, -0.80, -0.59))) # swing down in front
    ATMOUTH = ((-0.52, 0.20, 1.10), n3((0.0, -1.0, 0.0)))    # handle at the mouth
    SLIDIN = ((-0.52, -0.90, 1.10), n3((0.0, -1.0, 0.0)))    # handle slid deep in

    A_ev = [
        (0, P(*REST)),
        (16, P(*REST)),
        (32, P(*STAND)),
        (48, P(*PUSH)),
        (64, P(*DRAWN)),
        (80, P(*SWUNG)),
        (96, P(*JODAN)),
        (150, P(*JODAN)),
        (176, P(*ARC1)),
        (206, P(*ATMOUTH)),
        (230, P(*SLIDIN)),
        (250, P(*REST)),
        (311, P(*REST)),
    ]
    B_ev = [
        (0, P(*REST)),
        (16, P(*REST)),
        (32, P(*STAND)),
        (230, P(*STAND)),
        (250, P(*REST)),
        (311, P(*REST)),
    ]
    return build(A_ev, B_ev)


GENERATORS = {
    "inspect_a": gen_inspect,
    "inspect_wood": gen_inspect,
    "inspect_bamboo": gen_inspect,
    "inspect_muramasa": gen_inspect,
    "inspect_fox_black": gen_inspect,
    "inspect_fox_white": gen_inspect,
    "inspect_yamato": gen_inspect_yamato,
}


def build(A_events, B_events):
    A = Track("hardpointA")
    for f, pose in A_events:
        A.key(f, pose)
    B = Track("hardpointB")
    for f, pose in B_events:
        B.key(f, pose)
    return A, B


def generate_all(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, gen in GENERATORS.items():
        if name == "inspect_yamato":
            A, B = gen()
        else:
            A, B = gen(name)
        tracks = build_vmd_tracks({"hardpointA": A, "hardpointB": B}, total_frames=311,
                                  ease=ease_smooth, frames_per_key=1)
        path = os.path.join(out_dir, name + ".vmd")
        size = write_vmd(path, "bladeholder", tracks)
        print(f"{name}.vmd  {size} bytes")


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "out_vmd"
    generate_all(out)
