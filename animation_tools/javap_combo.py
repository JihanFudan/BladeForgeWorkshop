# -*- coding: utf-8 -*-
import zipfile, subprocess, os, tempfile
jar = r'C:\Users\JihanFudan\Desktop\mod\libs\SlashBladeResharped-2.0.5-1.21.1.jar'
z = zipfile.ZipFile(jar)
names = ['mods/flammpfeil/slashblade/registry/combo/ComboState.class',
         'mods/flammpfeil/slashblade/registry/combo/ComboState$Builder.class',
         'mods/flammpfeil/slashblade/registry/combo/ComboState$TimeLineTickAction.class',
         'mods/flammpfeil/slashblade/registry/combo/ComboState$TimeLineTickAction$TimeLineTickActionBuilder.class']
for n in names:
    try:
        data = z.read(n)
    except KeyError:
        print('NOT IN JAR:', n)
        continue
    out = os.path.join(tempfile.gettempdir(), 'cb_' + os.path.basename(n).replace('$', '_'))
    open(out, 'wb').write(data)
    r = subprocess.run(['javap', '-p', out], capture_output=True, text=True)
    print('=====', n)
    print(r.stdout[:3500])
