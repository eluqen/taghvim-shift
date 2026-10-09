package com.eluqen.shiftcalendar.update;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.util.Collections;
import java.util.List;
public final class AppUpdateManager {
    private AppUpdateManager(){}
    public static UpdateSource selectedSource(Context c){return UpdateSource.GOOGLE_PLAY;}
    public static List<UpdateSource> selectableSources(Context c){return Collections.singletonList(UpdateSource.GOOGLE_PLAY);}
    public static boolean isPlayPolicyRestricted(Context c){return true;}
    public static boolean isMarketPolicyRestricted(Context c){return false;}
    public static boolean isDirectPolicyRestricted(Context c){return false;}
    public static boolean supportsReview(Context c){return true;}
    public static boolean supportsDeveloperPage(Context c){return false;}
    public static String activeStoreLabel(Context c){return "گوگل‌پلی";}
    public static OpenResult openReview(Context c){return openStore(c,UpdateSource.GOOGLE_PLAY);}
    public static OpenResult openDeveloperPage(Context c){return new OpenResult(false,"صفحه توسعه‌دهنده در دسترس نیست");}
    public static OpenResult openStore(Context c,UpdateSource source){
        String pkg=c.getPackageName();
        Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+pkg));
        i.setPackage("com.android.vending");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {c.startActivity(i);return new OpenResult(true,"");}
        catch(Exception e){
            try{
                Intent fallback=new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+pkg));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(fallback);return new OpenResult(true,"");
            }catch(Exception ignored){return new OpenResult(false,"صفحه برنامه در دسترس نیست");}
        }
    }
    static boolean isRemoteNewer(int remoteVersionCode, int installedVersionCode) {
        return remoteVersionCode > installedVersionCode;
    }
    public static int currentVersionCode(Context c) throws Exception {
        PackageInfo i=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
        return Build.VERSION.SDK_INT>=28?(int)i.getLongVersionCode():i.versionCode;
    }
    public static final class OpenResult {
        public final boolean opened; public final String message;
        public OpenResult(boolean opened,String message){this.opened=opened;this.message=message;}
    }
}
