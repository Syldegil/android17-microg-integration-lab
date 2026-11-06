"""Fill the Smali template from the reviewed public certificate constants in the patch."""
import argparse,re
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];patch=(r/'patches/0001-restrict-microg-query-signatures.patch').read_text();text=(r/'config/MicrogSignaturePolicy.smali.in').read_text()
for placeholder,constant in [('REAL_CERT','REAL'),('GOOGLE_CERT','GOOGLE')]:
 matches=re.findall(constant+r' = new Signature\("([a-f0-9]+)"\)',patch)
 if len(matches)!=1:raise ValueError('public certificate constant missing or ambiguous')
 text=text.replace('@'+placeholder+'@',matches[0])
if re.search(r'@[A-Z_]+@',text):raise ValueError('unresolved placeholder')
with a.output.open('x') as f:f.write(text)
