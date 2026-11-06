import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.os.Looper;
import java.security.MessageDigest;
public final class NegativePmChecks {
    static String sha(byte[] bytes)throws Exception {
        StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format("%02x",b&255));return s.toString();
    }
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);System.out.println("PASS "+msg);}
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("expected real lab certificate SHA256");
        Looper.prepareMainLooper();Class<?> at=Class.forName("android.app.ActivityThread");Object t=at.getMethod("systemMain").invoke(null);Context c=(Context)at.getMethod("getSystemContext").invoke(t);PackageManager pm=c.getPackageManager();
        String pkg="org.example.microglab.spoofnegative";PackageInfo info=pm.getPackageInfo(pkg,PackageManager.GET_SIGNATURES|PackageManager.GET_SIGNING_CERTIFICATES);
        check((info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==0,"unrelated probe is ordinary installed app");
        check(sha(info.signatures[0].toByteArray()).equals(args[0]),"legacy query retains real lab certificate despite spoof metadata");
        check(sha(info.signingInfo.getApkContentsSigners()[0].toByteArray()).equals(args[0]),"modern query retains real lab certificate despite spoof metadata");
        check(pm.checkPermission("android.permission.FAKE_PACKAGE_SIGNATURE",pkg)==PackageManager.PERMISSION_DENIED,"arbitrary spoof permission denied");
        check(pm.checkSignatures(pkg,"com.google.android.gms")!=PackageManager.SIGNATURE_MATCH,"probe cannot share microG signing identity");
        check(pm.checkSignatures(pkg,"android")!=PackageManager.SIGNATURE_MATCH,"probe cannot share platform signing identity");
        PackageInfo gsf=pm.getPackageInfo("com.google.android.gsf",PackageManager.GET_SIGNATURES|PackageManager.GET_SIGNING_CERTIFICATES);
        check(sha(gsf.signatures[0].toByteArray()).equals("9bd06727e62796c0130eb6dab39b73157451582cbd138e86c468acc395d14165"),"GSF proxy reports actual official certificate, no spoof");
        System.out.println("NEGATIVE_PM_CHECKS_PASS=7");
    }
}
