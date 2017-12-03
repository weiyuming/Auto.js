package com.stardust.scriptdroid.tool;

/**
 * Created by Stardust on 2017/2/2.
 */


import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.stardust.scriptdroid.App;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.accessibility.AccessibilityService;
import com.stardust.util.IntentUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class CrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static int crashCount = 0;
    private static long firstCrashMillis = 0;
    private final Class<?> mErrorReportClass;

    public CrashHandler(Class<?> errorReportClass) {
        this.mErrorReportClass = errorReportClass;
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        if (causedByBadWindowToken(ex)) {
            Toast.makeText(App.getApp(), R.string.text_no_floating_window_permission, Toast.LENGTH_SHORT).show();
            IntentUtil.goToAppDetailSettings(App.getApp());
            return;
        }
        try {
            Log.e(TAG, "Uncaught Exception", ex);
            if (crashTooManyTimes())
                return;
            String msg = App.getApp().getString(R.string.sorry_for_crash) + ex.toString();
            startErrorReportActivity(msg, throwableToString(ex));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static boolean causedByBadWindowToken(Throwable e) {
        while (e != null) {
            if (e instanceof WindowManager.BadTokenException) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    private void startErrorReportActivity(String msg, String detail) {
        Intent intent = new Intent(App.getApp(), this.mErrorReportClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("message", msg);
        intent.putExtra("error", detail);
        App.getApp().startActivity(intent);
    }

    private boolean crashTooManyTimes() {
        if (crashIntervalTooLong()) {
            resetCrashCount();
            return false;
        }
        crashCount++;
        return crashCount >= 5;
    }

    private void resetCrashCount() {
        firstCrashMillis = System.currentTimeMillis();
        crashCount = 0;
    }

    private boolean crashIntervalTooLong() {
        return System.currentTimeMillis() - firstCrashMillis > 3000;
    }

    public static String throwableToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace();
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}