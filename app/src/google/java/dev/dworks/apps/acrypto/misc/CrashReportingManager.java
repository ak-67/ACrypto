package dev.dworks.apps.acrypto.misc;

import com.android.volley.error.VolleyError;
import com.google.firebase.crash.FirebaseCrash;

import dev.dworks.apps.acrypto.BuildConfig;

/**
 * Created by HaKr on 23/05/16.
 */

public class CrashReportingManager {

    public static void enable(boolean enable){
        FirebaseCrash.setCrashCollectionEnabled(enable);
    }

    public static void logException(Exception e) {
        logException(e, false);
    }

    public static void logException(Exception e, boolean log) {
        if(BuildConfig.DEBUG){
            e.printStackTrace();
        } else if(log) {
            FirebaseCrash.report(e);
        }
    }

    public static void log(String s) {
        FirebaseCrash.log(s);
    }

    public static void log(String tag, String s) {
        FirebaseCrash.log(tag+":"+s);
    }
}