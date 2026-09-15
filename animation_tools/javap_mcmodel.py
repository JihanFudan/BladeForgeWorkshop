# -*- coding: utf-8 -*-
import zipfile, subprocess, os, tempfile
javap = r'C:\Users\JihanFudan\Desktop\buildtools\jdk21\jdk-21.0.12.1+1\bin\javap.exe'
jar = r'C:\Users\JihanFudan\Desktop\mod\build\moddev\artifacts\neoforge-21.1.244-merged.jar'
z = zipfile.ZipFile(jar)
for n in ['net/minecraft/client/model/EntityModel.class',
          'net/minecraft/client/model/HumanoidModel.class']:
    data = z.read(n)
    out = os.path.join(tempfile.gettempdir(), 'mc_' + os.path.basename(n))
    open(out, 'wb').write(data)
    r = subprocess.run([javap, '-p', out], capture_output=True, text=True)
    print('=====', n)
    print(r.stdout[:2500])
