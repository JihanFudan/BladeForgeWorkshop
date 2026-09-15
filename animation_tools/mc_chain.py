#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Full render-chain simulation of LayerMainBlade:
VMD pose (hardpointA pos/rot) -> MC-local coordinates of handle/tip/edge.
Returns MC-local positions (player at origin, facing -Z by default).
"""
import math
import sys
import numpy as np


def quat_to_mat(q):
    x, y, z, w = q
    return np.array([
        [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
        [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
        [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)],
    ])


def rz180(p):
    return np.array([-p[0], -p[1], p[2]])


def sx(p):
    return np.array([-p[0], p[1], p[2]])


def full_chain(pos, rot, v_obj, model_scale=0.0625, motion_scale=0.125):
    """v_obj: OBJ-space point. Returns MC-local point."""
    R = quat_to_mat(rot)
    w = np.array(v_obj, dtype=float) * model_scale          # OBJ -> MMD
    # Sx·M·Sx applied to w:
    h = sx(pos) + sx(R @ sx(w))
    # Rz(180) then scale then translate(0,1.5,0)
    p = rz180(h) * motion_scale
    p = p + np.array([0.0, 1.5, 0.0])
    return p


def analyze(name, pos, rot, blade_len_obj=296.0):
    handle = np.array(pos, dtype=float)
    tip_obj = np.array([-blade_len_obj, 0.0, 0.0])
    edge_obj = np.array([0.0, 0.0, 1.0])   # edge ridge direction in OBJ
    spine_obj = np.array([0.0, 1.0, 0.0])

    h_mc = full_chain(pos, rot, np.zeros(3))
    t_mc = full_chain(pos, rot, tip_obj)
    e_dir = full_chain(pos, rot, edge_obj) - h_mc
    s_dir = full_chain(pos, rot, spine_obj) - h_mc
    t_dir = t_mc - h_mc
    e_dir = e_dir / np.linalg.norm(e_dir)
    s_dir = s_dir / np.linalg.norm(s_dir)

    def f(v):
        return f"({v[0]:7.2f},{v[1]:7.2f},{v[2]:7.2f})"

    print(f"{name}: handle={f(h_mc)} tip={f(t_mc)} len={np.linalg.norm(t_dir):.2f} "
          f"tipDir={f(t_dir/np.linalg.norm(t_dir))} edgeDir={f(e_dir)} spineDir={f(s_dir)}")


if __name__ == "__main__":
    poses = [
        ("idle f0", (4.1720, 6.3646, -4.3748), (-0.60396, 0.00750, -0.79639, 0.03082)),
        ("grip f19", (-1.4438, 10.1142, -4.1178), (0.04190, -0.02047, -0.02090, 0.99869)),
        ("drawout f76", (-6.7186, 10.3050, -4.3278), (0.04164, -0.02099, -0.00831, 0.99888)),
        ("slash f82", (-2.1039, 16.0536, -5.2899), (-0.45237, -0.56750, 0.27282, 0.63157)),
        ("hold f84", (-4.6278, 7.8041, -0.2191), (-0.32977, 0.64649, 0.46232, -0.50947)),
        ("spinmid f119", (2.8903, 18.2004, -11.1022), (0.10489, -0.81541, -0.23711, 0.51757)),
        ("rotstart f132", (-2.8355, 7.5418, -4.9261), (0.81908, 0.15609, -0.46798, -0.29279)),
        ("rotend f161", (-2.9338, 6.6326, -5.5868), (-0.74834, -0.58089, 0.27834, 0.15839)),
        ("slidein f186", (1.7974, 6.3646, -4.3748), (-0.81548, -0.00212, -0.57791, 0.03165)),
        ("cur hold f210", (-4.63, 7.80, -0.22), (0.6946, 0.0938, -0.6800, -0.2100)),
    ]
    for name, pos, rot in poses:
        analyze(name, pos, rot)
