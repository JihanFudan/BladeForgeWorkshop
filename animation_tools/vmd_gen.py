#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""VMD animation generator library.

Conventions (render space, from LayerMainBlade chain):
  - MC render space: forward = +Z, up = +Y, left = +X (player's left), right = -X.
  - VMD space: origin at MC y=1.5 (neck), 8 VMD units = 1 block.
  - Blade model: tip along OBJ -X, flat faces OBJ +/-Z, width along OBJ Y.
"""
import math
import numpy as np


# --------------------------------------------------------------------------
# quaternion / matrix helpers
# --------------------------------------------------------------------------

def mat_to_quat(R):
    """Standard (x,y,z,w) quaternion from rotation matrix (column-major logic)."""
    tr = R[0, 0] + R[1, 1] + R[2, 2]
    if tr > 0:
        s = math.sqrt(tr + 1.0) * 2
        w = 0.25 * s
        x = (R[2, 1] - R[1, 2]) / s
        y = (R[0, 2] - R[2, 0]) / s
        z = (R[1, 0] - R[0, 1]) / s
    elif R[0, 0] > R[1, 1] and R[0, 0] > R[2, 2]:
        s = math.sqrt(1.0 + R[0, 0] - R[1, 1] - R[2, 2]) * 2
        w = (R[2, 1] - R[1, 2]) / s
        x = 0.25 * s
        y = (R[0, 1] + R[1, 0]) / s
        z = (R[0, 2] + R[2, 0]) / s
    elif R[1, 1] > R[2, 2]:
        s = math.sqrt(1.0 + R[1, 1] - R[0, 0] - R[2, 2]) * 2
        w = (R[0, 2] - R[2, 0]) / s
        x = (R[0, 1] + R[1, 0]) / s
        y = 0.25 * s
        z = (R[1, 2] + R[2, 1]) / s
    else:
        s = math.sqrt(1.0 + R[2, 2] - R[0, 0] - R[1, 1]) * 2
        w = (R[1, 0] - R[0, 1]) / s
        x = (R[0, 2] + R[2, 0]) / s
        y = (R[1, 2] + R[2, 1]) / s
        z = 0.25 * s
    q = np.array([x, y, z, w])
    return q / np.linalg.norm(q)


def quat_to_mat(q):
    x, y, z, w = q
    return np.array([
        [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
        [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
        [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)],
    ])


def qmul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return np.array([
        aw * bx + ax * bw + ay * bz - az * by,
        aw * by - ax * bz + ay * bw + az * bx,
        aw * bz + ax * by - ay * bx + az * bw,
        aw * bw - ax * bx - ay * by - az * bz,
    ])


def qnorm(q):
    n = np.linalg.norm(q)
    return q / n if n > 0 else np.array([0.0, 0.0, 0.0, 1.0])


def qslerp(q0, q1, t):
    q0 = qnorm(q0)
    q1 = qnorm(q1)
    dot = float(q0[0] * q1[0] + q0[1] * q1[1] + q0[2] * q1[2] + q0[3] * q1[3])
    if dot < 0:
        q1 = -q1
        dot = -dot
    dot = min(1.0, max(-1.0, dot))
    if dot > 0.9995:
        r = q0 + t * (q1 - q0)
        return qnorm(r)
    theta = math.acos(dot)
    s0 = math.sin((1 - t) * theta) / math.sin(theta)
    s1 = math.sin(t * theta) / math.sin(theta)
    return qnorm(q0 * s0 + q1 * s1)


def ease_smooth(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def ease_io(t, k=2.0):
    """cosine ease-in-out, k>1 sharpens mid-crossing."""
    t = min(1.0, max(0.0, t))
    return (math.cos(math.pi * (t ** k)) + 1.0) * 0.5


def ease_in(t):
    t = min(1.0, max(0.0, t))
    return t * t


def ease_out(t):
    t = min(1.0, max(0.0, t))
    return 1.0 - (1 - t) * (1 - t)


# --------------------------------------------------------------------------
# pose conversion
# --------------------------------------------------------------------------

def mc_handle_to_vmd(h):
    """MC-local handle (blocks, player at 0) -> VMD position."""
    return np.array([h[0] / 0.125, (1.5 - h[1]) / 0.125, h[2] / 0.125])


def dir_roll_to_quat(d, roll_deg):
    """Blade direction d (MC render space, normalized) + roll around blade axis
    (deg, 0 = flat faces camera as close as possible) -> VMD rotation quat.

    Mapping (from LayerMainBlade chain, verified against the reference idle):
        MC direction = (R00, -R10, R20)  for OBJ +X tip
        MC flat      = (R02, -R12, R22)  for OBJ +Z normal
    So the matrix columns are built with Y negated relative to MC-space design.
    """
    u = np.array(d, dtype=float)
    u = u / np.linalg.norm(u)
    # column 1 (maps OBJ -X tip to MC dir): c1 = (dx, -dy, dz)
    c1 = np.array([u[0], -u[1], u[2]])
    # flat normal (OBJ +Z) toward camera: choose c3 = (0,0,1) projected onto plane
    # perpendicular to c1, normalized (the MC image keeps (x, -y, z) so (0,0,1)->(0,0,1))
    zhat = np.array([0.0, 0.0, 1.0])
    c3 = zhat - np.dot(zhat, c1) * c1
    if np.linalg.norm(c3) < 1e-5:
        c3 = np.array([0.0, 1.0, 0.0])
    else:
        c3 = c3 / np.linalg.norm(c3)
    c2 = np.cross(c3, c1)  # right-handed: c1 x c2 = c3
    th = math.radians(roll_deg)
    c2r = c2 * math.cos(th) + c3 * math.sin(th)
    c3r = -c2 * math.sin(th) + c3 * math.cos(th)
    R = np.column_stack([c1, c2r, c3r])
    return mat_to_quat(R)


def view_to_vmd(pos_view, tip_view, front=None, roll_deg=0.0):
    """Eye/view-space design pose -> (pos_vmd, rot_vmd).

    View space (what the first-person player sees): x = screen-right,
    y = screen-up, depth = positive forward.

    Verified end-to-end against LayerMainBlade + BladeFirstPersonRender
    (SlashBlade Resharped 1.21.1) and the user's in-game video:
      eye = (0.15*px, 0.125*py - 1.5, -0.125*pz - 0.5),  depth = 0.125*pz + 0.5
    So  px = vx/0.15,  py = (vy + 1.5)/0.125,  pz = (depth - 0.5)/0.125.

    Blade-tip direction in eye space = (1.2*R00, R10, -R20) with R = VMD quat
    matrix; front face (model -Z normal) in eye space = (-1.2*R02, -R12, R22).
    """
    vx, vy, depth = pos_view
    pos = np.array([vx / 0.15, (vy + 1.5) / 0.125, (depth - 0.5) / 0.125])
    t = np.array(tip_view, dtype=float)
    t = t / np.linalg.norm(t)
    c1 = np.array([t[0] / 1.2, t[1], -t[2]])
    if front is None:
        zhat = np.array([0.0, 0.0, 1.0])
        c2 = zhat - np.dot(zhat, c1) * c1
        if np.linalg.norm(c2) < 1e-5:
            c2 = np.array([0.0, 1.0, 0.0])
        else:
            c2 = c2 / np.linalg.norm(c2)
    else:
        f = np.array(front, dtype=float)
        f = f / np.linalg.norm(f)
        c2 = np.array([-f[0] / 1.2, -f[1], f[2]])
    c3 = np.cross(c1, c2)
    th = math.radians(roll_deg)
    c2r = c2 * math.cos(th) + c3 * math.sin(th)
    c3r = -c2 * math.sin(th) + c3 * math.cos(th)
    R = np.column_stack([c1, c2r, c3r])
    return pos, mat_to_quat(R)


def blade_tip_vmd(pos, rot, blade_len=16.0):
    """VMD handle pos+rot -> tip pos (VMD space, blade extends OBJ -X)."""
    tip_dir = quat_to_mat(rot) @ np.array([-1.0, 0.0, 0.0])
    return pos + tip_dir * blade_len


def pose_to_vmd(handle_mc, d_mc, roll_deg=0.0):
    """Design pose -> (pos_vmd, rot_vmd)."""
    pos = mc_handle_to_vmd(handle_mc)
    rot = dir_roll_to_quat(d_mc, roll_deg)
    return pos, rot


# --------------------------------------------------------------------------
# keyframe timeline generation
# --------------------------------------------------------------------------

class Track:
    """A pose timeline: events list of (frame, pose) where pose = (pos3, rot4)."""

    def __init__(self, name):
        self.name = name
        self.events = []

    def key(self, frame, pose):
        self.events.append((int(frame), (np.array(pose[0], dtype=float),
                                         qnorm(np.array(pose[1], dtype=float)))))
        return self

    def keys(self, *frame_pose_pairs):
        for fp in frame_pose_pairs:
            self.key(*fp)
        return self

    def generate(self, total_frames, ease=ease_smooth, frames_per_key=1):
        """Dense frames: every `frames_per_key` frame, eased between events."""
        ev = sorted(self.events, key=lambda e: e[0])
        assert ev, "no keyframes"
        out = {}
        for i in range(0, total_frames, frames_per_key):
            # find surrounding events
            j = 0
            while j < len(ev) - 1 and ev[j + 1][0] <= i:
                j += 1
            f0, p0 = ev[j]
            if j + 1 >= len(ev):
                out[i] = (p0[0].copy(), p0[1].copy())
                continue
            f1, p1 = ev[j + 1]
            if i <= f0:
                out[i] = (p0[0].copy(), p0[1].copy())
                continue
            if f1 == f0:
                out[i] = (p1[0].copy(), p1[1].copy())
                continue
            t = ease((i - f0) / (f1 - f0))
            out[i] = (p0[0] + t * (p1[0] - p0[0]), qslerp(p0[1], p1[1], t))
        return out


def build_vmd_tracks(tracks, total_frames, ease=ease_smooth, frames_per_key=1):
    """tracks: dict name -> Track. Returns list for vmd_io.write_vmd."""
    return [(name, sorted(t.generate(total_frames, ease, frames_per_key).items()))
            for name, t in tracks.items()]
