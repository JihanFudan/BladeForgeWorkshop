# -*- coding: utf-8 -*-
import re, os
base = r'src\main\java\cn\blockforge\generated\slashbladereshslashblad'
files = {
 'InspectArmHideMixin.java': os.path.join(base, 'mixin', 'InspectArmHideMixin.java'),
 'UltimatePlayerPoseMixin.java': os.path.join(base, 'mixin', 'UltimatePlayerPoseMixin.java'),
 'UltimateBladeLayerMixin.java': os.path.join(base, 'mixin', 'UltimateBladeLayerMixin.java'),
 'UltimateMotionMixin.java': os.path.join(base, 'mixin', 'UltimateMotionMixin.java'),
 'RecipeManagerMixin.java': os.path.join(base, 'mixin', 'RecipeManagerMixin.java'),
 'RefinementDamageMixin.java': os.path.join(base, 'mixin', 'RefinementDamageMixin.java'),
}
for name, path in files.items():
    if not os.path.exists(path):
        continue
    s = open(path, encoding='utf-8').read()
    targets = re.findall(r'@Mixin\(([^)]+)\)', s)
    methods = re.findall(r'method\s*=\s*"([^"]+)"', s)
    print('---', name)
    print('  目标:', targets)
    print('  注入方法:', methods)
    doc = re.search(r'/\*\*(.*?)\*/', s, re.S)
    if doc:
        print('  说明:', ' '.join(doc.group(1).split())[:220])
    print()
