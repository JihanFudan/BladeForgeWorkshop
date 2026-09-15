# -*- coding: utf-8 -*-
import zipfile, subprocess, os, tempfile
javap = r'C:\Users\JihanFudan\Desktop\buildtools\jdk21\jdk-21.0.12.1+1\bin\javap.exe'
jar = r'C:\Users\JihanFudan\Desktop\mod\libs\SlashBladeResharped-2.0.5-1.21.1.jar'
z = zipfile.ZipFile(jar)
n = 'mods/flammpfeil/slashblade/compat/playerAnim/PlayerAnimationOverrider.class'
data = z.read(n)
out = os.path.join(tempfile.gettempdir(), 'pao.class')
open(out, 'wb').write(data)
r = subprocess.run([javap, '-p', '-c', '-l', out], capture_output=True, text=True)
print(r.stdout)
