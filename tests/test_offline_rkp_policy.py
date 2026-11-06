import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from offline_rkp_policy import without_google_endpoint,OLD,NEW
class OfflineRkpPolicyTest(unittest.TestCase):
 def test_only_endpoint_changes(self):
  prefix=b'# original settings\nremote_provisioning.enable_rkpd=true\n';suffix=b'ro.security.fixture=unchanged\n'
  self.assertEqual(without_google_endpoint(prefix+OLD+suffix),prefix+NEW+suffix)
 def test_duplicate_or_unknown_endpoint_rejected(self):
  for endpoint in [OLD+OLD,b'remote_provisioning.hostname=unknown.example\n',OLD+b' remote_provisioning.hostname = other.example\n']:
   with self.assertRaises(ValueError):without_google_endpoint(b'remote_provisioning.enable_rkpd=true\n'+endpoint)
 def test_service_disable_is_not_hidden(self):
  for setting in [b'',b'remote_provisioning.enable_rkpd=false\n',b'remote_provisioning.enable_rkpd=true\nremote_provisioning.enable_rkpd=false\n']:
   with self.assertRaises(ValueError):without_google_endpoint(setting+OLD)
