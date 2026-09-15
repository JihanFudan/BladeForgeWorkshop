#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Compute blade handle/tip/edge orientation for a hardpoint pose (VMD space).
Blade model: tip along local -X, edge ridges along local +/-Z (flat faces +/-Y).
Length in MMD units (12 units = 1.5 blocks): visible blade ~16 units (2 blocks).
"""
import math
import sys


def qmul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return (
        aw * bx + ax * bw + ay * bz - az * by,
        aw * by - ax * bz + ay * bw + az * bx,
        aw * bz + ax * by - ay * bx + az * bw,
        aw * bw - ax * bx - ay * by - az * bz,
    )


def qrot(q, v):
    qv = (v[0], v[1], v[2], 0.0)
    qc = (-q[0], -q[1], -q[2], q[3])
    r = qmul(qmul(q, qv), qc)
    return (r[0], r[1], r[2])


def analyze(name, pos, rot, blade_len=16.0):
    tip_dir = qrot(rot, (-1.0, 0.0, 0.0))
    edge_pos_dir = qrot(rot, (0.0, 0.0, 1.0))
    spine_dir = qrot(rot, (0.0, 1.0, 0.0))
    tip = (pos[0] + tip_dir[0] * blade_len,
           pos[1] + tip_dir[1] * blade_len,
           pos[2] + tip_dir[2] * blade_len)
    print(f"{name}: handle=({pos[0]:7.2f},{pos[1]:7.2f},{pos[2]:7.2f}) "
          f"tip=({tip[0]:7.2f},{tip[1]:7.2f},{tip[2]:7.2f}) "
          f"tipDir=({tip_dir[0]:6.2f},{tip_dir[1]:6.2f},{tip_dir[2]:6.2f}) "
          f"edgeDir=({edge_pos_dir[0]:6.2f},{edge_pos_dir[1]:6.2f},{edge_pos_dir[2]:6.2f}) "
          f"spineDir=({spine_dir[0]:6.2f},{spine_dir[1]:6.2f},{spine_dir[2]:6.2f})")


if __name__ == "__main__":
    # Reference SlashBladeInspect key poses (hardpointA): name, pos, rot
    poses = [
        ("ref idle f0", (4.1720, 6.3646, -4.3748), (-0.60396, 0.00750, -0.79639, 0.03082)),
        ("ref grip f19", (-1.4438, 10.1142, -4.1178), (0.04190, -0.02047, -0.02090, 0.99869)),
        ("ref drawout f76", (-6.7186, 10.3050, -4.3278), (0.04164, -0.02099, -0.00831, 0.99888)),
        ("ref slash f82", (-2.1039, 16.0536, -5.2899), (-0.45237, -0.56750, 0.27282, 0.63157)),
        ("ref hold f84", (-4.6278, 7.8041, -0.2191), (-0.32977, 0.64649, 0.46232, -0.50947)),
        ("ref spinmid f119", (2.8903, 18.2004, -11.1022), (0.10489, -0.81541, -0.23711, 0.51757)),
        ("ref rotstart f132", (-2.8355, 7.5418, -4.9261), (0.81908, 0.15609, -0.46798, -0.29279)),
        ("ref rotend f161", (-2.9338, 6.6326, -5.5868), (-0.74834, -0.58089, 0.27834, 0.15839)),
        ("ref slidein f186", (1.7974, 6.3646, -4.3748), (-0.81548, -0.00212, -0.57791, 0.03165)),
        # current mod generic hold pose (inspection hold f210)
        ("cur hold f210", (-4.63, 7.80, -0.22), (0.6946, 0.0938, -0.6800, -0.2100)),
    ]
    for name, pos, rot in poses:
        analyze(name, pos, rot)
