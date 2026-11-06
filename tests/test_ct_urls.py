import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from patch_ct_urls import patch,OLD,NEW
class CtUrls(unittest.TestCase):
 def test_exact_literal_change_only(self):
  text='const-string v0, "'+OLD+'log_list.pub"\ninvoke-static verifySignature\n'
  self.assertEqual(patch(text,{'log_list.pub'}),text.replace(OLD,NEW))
 def test_missing_or_extra_urls_rejected(self):
  for text in ['', '"'+OLD+'log_list.pub"\n"'+OLD+'unexpected"', '"'+OLD+'log_list.pub"\n"'+OLD+'log_list.pub"']:
   with self.assertRaises(ValueError):patch(text,{'log_list.pub'})
 def test_callsite_pool_renumbering_preserves_sharing(self):
  from patch_ct_urls import compare_classes
  a={'a':'invoke-custom {p0}, call_site_0("hashCode", I)\n','b':'invoke-custom {p0}, call_site_1("equals", Z)\n'}
  b={'a':a['a'].replace('call_site_0','call_site_4'),'b':a['b'].replace('call_site_1','call_site_2')}
  self.assertEqual(compare_classes(a,b),{'0':'4','1':'2'})
  with self.assertRaises(ValueError):compare_classes(a,{'a':b['a'],'b':b['b'].replace('call_site_2','call_site_4')})
  with self.assertRaises(ValueError):compare_classes(a,{'a':b['a'].replace('hashCode','changed'),'b':b['b']})
 def test_true_static_initializer_is_not_normalized(self):
  from patch_ct_urls import compare_classes
  self.assertEqual(compare_classes({'a':'.field static f:Z = false\n'},{'a':'.field static f:Z\n'}),{})
  with self.assertRaises(ValueError):compare_classes({'a':'.field static f:Z = true\n'},{'a':'.field static f:Z\n'})
