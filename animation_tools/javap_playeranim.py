# -*- coding: utf-8 -*-
import zipfile, subprocess, os, tempfile
javap = r'C:\Users\JihanFudan\Desktop\buildtools\jdk21\jdk-21.0.12.1+1\bin\javap.exe'
jar = r'C:\Users\JihanFudan\Desktop\mod\libs\SlashBladeResharped-2.0.5-1.21.1.jar'
z = zipfile.ZipFile(jar)
targets = [
    'mods/flammpfeil/slashblade/compat/playerAnim/PlayerAnimationOverrider.class',
    'mods/flammpfeil/slashblade/compat/playerAnim/VmdAnimation.class',
]
for n in targets:
    try:
        data = z.read(n)
    except KeyError:
        print('NOT IN JAR:', n)
        continue
    out = os.path.join(tempfile.gettempdir(), 'pp_' + os.path.basename(n).replace('$', '_'))
    open(out, 'wb').write(data)
    r = subprocess.run([javap, '-p', '-c', out], capture_output=True, text=True)
    print('=====', n)
    print(r.stdout[:4000])
