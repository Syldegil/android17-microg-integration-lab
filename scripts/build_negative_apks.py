#!/usr/bin/env python3
"""Benign no-code APKs for lab installer/spoofing negative controls."""
from pathlib import Path
import argparse
import re
import subprocess
import hashlib
import json
R=Path(__file__).resolve().parents[1]
def run(*args):return subprocess.run([str(x) for x in args],check=True,capture_output=True,text=True).stdout

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True);p.add_argument('--build-tools',type=Path,required=True)
    p.add_argument('--framework-res',type=Path,required=True);p.add_argument('--lab-key',type=Path,required=True);p.add_argument('--lab-cert',type=Path,required=True)
    a=p.parse_args();OUT=a.output;BT=a.build_tools
    OUT.mkdir(exist_ok=False)
    signature=re.search(r'GOOGLE = new Signature\("([a-f0-9]+)"\)',(R/'patches/0001-restrict-microg-query-signatures.patch').read_text()).group(1)
    results={}
    for name,pkg in [('unrelated','org.example.microglab.spoofnegative'),('wrong-gms-signer','com.google.android.gms')]:
        manifest=OUT/(name+'.xml')
        manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{pkg}" android:versionCode="999999999" android:versionName="lab-negative-control"><uses-sdk android:minSdkVersion="37" android:targetSdkVersion="37"/><uses-permission android:name="android.permission.FAKE_PACKAGE_SIGNATURE"/><application android:hasCode="false" android:label="ROM laboratory negative control"><meta-data android:name="fake-signature" android:value="{signature}"/></application></manifest>''')
        unsigned=OUT/(name+'-unsigned.apk');apk=OUT/(name+'.apk')
        run(BT/'aapt2','link','-I',a.framework_res,'--manifest',manifest,'-o',unsigned)
        run(BT/'zipalign','-p','4',unsigned,apk)
        run(BT/'apksigner','sign','--key',a.lab_key,'--cert',a.lab_cert,apk)
        verification=run(BT/'apksigner','verify','--verbose','--print-certs',apk)
        (OUT/(name+'-signature.txt')).write_text(verification)
        results[name]={'package':pkg,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'has_code':False,'runtime':'NOT_EXECUTED'}
    (OUT/'result.json').write_text(json.dumps(results,indent=2)+'\n')
if __name__=='__main__':main()
