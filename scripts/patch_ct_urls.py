#!/usr/bin/env python3
"""Narrow CT distribution-host adaptation; verifier and all trust anchors unchanged."""
import hashlib,json,re,subprocess,zipfile
from pathlib import Path
OLD='https://www.gstatic.com/android/certificate_transparency/'
NEW='https://gstatic.grapheneos.org/android/certificate_transparency/'
EXPECTED={'CertificateTransparencyService.smali':{'v2/log_list.sig','v2/log_list.json','v3/log_list.sig','v3/log_list.ctfb'},'CertificateTransparencyDownloader.smali':{'log_list.pub'}}
def patch(text,names):
    urls=re.findall(r'"('+re.escape(OLD)+r'[^"\n]*)"',text)
    if sorted(urls)!=sorted(OLD+n for n in names):raise ValueError('unreviewed CT URL literals')
    return text.replace(OLD,NEW)
def compare_classes(before,after):
    if set(before)!=set(after):raise ValueError('class set changed')
    pattern=r'(?m)^(\s*invoke-custom(?:/range)? \{[^}]*\}, )call_site_(\d+)\('
    mapping={};reverse={}
    for name in before:
        left=re.findall(pattern,before[name]);right=re.findall(pattern,after[name])
        if len(left)!=len(right):raise ValueError('invoke-custom count changed')
        for (_,old),(_,new) in zip(left,right):
            if mapping.get(old,new)!=new or reverse.get(new,old)!=old:raise ValueError('call-site sharing changed')
            mapping[old]=new;reverse[new]=old
    def defaults(text):
        return re.sub(r'(?m)^(\.field (?=[^\n]*\bstatic\b)[^\n]*:Z) = false$',r'\1',text)
    changed=[]
    for name in before:
        right=re.sub(pattern,lambda m:m[1]+'call_site_'+reverse[m[2]]+'(',after[name])
        if defaults(before[name])!=defaults(right):changed.append(name)
    if changed:raise ValueError('class semantics changed: '+str(changed))
    return mapping
