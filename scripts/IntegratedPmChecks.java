import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.os.Looper;
import java.security.MessageDigest;

/** Runs as shell in the emulator; queries the real system_server PM Binder. */
public final class IntegratedPmChecks {
    static byte[] hex(String s) {
        byte[] b=new byte[s.length()/2];
        for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(2*i,2*i+2),16);
        return b;
    }
    static String digest(Signature s) throws Exception {
        StringBuilder out=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-1").digest(s.toByteArray()))out.append(String.format("%02x",b&255));
        return out.toString();
    }
    static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
        System.out.println("PASS "+label);
    }
    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        Class<?> at=Class.forName("android.app.ActivityThread");
        Object thread=at.getMethod("systemMain").invoke(null);
        Context ctx=(Context)at.getMethod("getSystemContext").invoke(thread);
        PackageManager pm=ctx.getPackageManager();
        byte[] real=hex("9bd06727e62796c0130eb6dab39b73157451582cbd138e86c468acc395d14165");
        for(String pkg:new String[]{"com.google.android.gms","com.android.vending"}) {
            PackageInfo info=pm.getPackageInfo(pkg,PackageManager.GET_SIGNATURES|PackageManager.GET_SIGNING_CERTIFICATES);
            check((info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0,pkg+" factory system app");
            check(info.signatures!=null&&info.signatures.length==1,pkg+" legacy single signer");
            check(digest(info.signatures[0]).equals("38918a453d07199354f8b19af05ec6562ced5788"),pkg+" legacy Google compatibility certificate");
            check(info.signingInfo!=null&&!info.signingInfo.hasMultipleSigners(),pkg+" modern single signer");
            check(digest(info.signingInfo.getApkContentsSigners()[0]).equals("38918a453d07199354f8b19af05ec6562ced5788"),pkg+" modern Google compatibility certificate");
            check(pm.hasSigningCertificate(pkg,real,PackageManager.CERT_INPUT_SHA256),pkg+" actual signing identity preserved");
            check(pm.checkSignatures(pkg,"android")!=PackageManager.SIGNATURE_MATCH,pkg+" not platform signed");
        }
        check(pm.checkSignatures("com.google.android.gms","com.android.vending")==PackageManager.SIGNATURE_MATCH,"official microG pair real signatures match");
        check(pm.checkPermission("android.permission.INSTALL_LOCATION_PROVIDER","com.google.android.gms")==PackageManager.PERMISSION_GRANTED,"microG privileged location provider permission");
        System.out.println("INTEGRATED_PM_CHECKS_PASS=16");
    }
}
