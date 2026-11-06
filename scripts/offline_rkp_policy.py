"""Use the OEM RKP implementation's explicit no-endpoint path; do not fake attestation."""
HOST=b'remote_provisioning.hostname'
OLD=HOST+b'=remoteprovisioning.googleapis.com\n'
NEW=HOST+b'=\n'
def without_google_endpoint(data):
 lines=data.splitlines()
 if sum(line.strip().split(b'=',1)[0].strip()==HOST for line in lines)!=1 or data.count(OLD)!=1:raise ValueError('unexpected or duplicate RKP endpoint')
 if sum(line.strip().split(b'=',1)[0].strip()==b'remote_provisioning.enable_rkpd' for line in lines)!=1 or b'remote_provisioning.enable_rkpd=true\n' not in data:raise ValueError('unexpected RKP service configuration')
 return data.replace(OLD,NEW,1)
