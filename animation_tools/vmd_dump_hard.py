#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Dump hardpointA/B keyframes of a VMD, one line per key."""
import sys
from vmd_parse import parse_vmd

d = parse_vmd(sys.argv[1])
want = set(sys.argv[2:]) if len(sys.argv) > 2 else {"hardpointA", "hardpointB"}
for b in d["bones"]:
    if b["name"] in want:
        print(f"f{b['frame']:>4} {b['name']:10s} pos=({b['pos'][0]:9.4f},{b['pos'][1]:9.4f},{b['pos'][2]:9.4f}) "
              f"rot=({b['rot'][0]:8.5f},{b['rot'][1]:8.5f},{b['rot'][2]:8.5f},{b['rot'][3]:8.5f})")
