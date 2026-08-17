package com.termux.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.DisplayMetrics;

public final class RexxAutoTune {

    private static final String PREFS = "rexx_autotune";

    private RexxAutoTune() {}

    public static Profile detect(Context context) {

        ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo memoryInfo =
                new ActivityManager.MemoryInfo();

        long ramMb = 4096;

        if (am != null) {
            am.getMemoryInfo(memoryInfo);
            ramMb = memoryInfo.totalMem / (1024L * 1024L);
        }

        DisplayMetrics dm =
                context.getResources().getDisplayMetrics();

        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        /*
         * Low RAM:
         * 4 GB phones -> conservative profile
         */
        boolean lowRam = ramMb <= 4608;

        int width;
        int height;
        int wineRam;
        int cores;

        if (lowRam) {

            width = 960;
            height = 540;

            wineRam = 1024;

            /*
             * Don't give all CPU cores to Wine.
             * Android itself needs CPU too.
             */
            cores = Math.min(2,
                    Math.max(1, Runtime.getRuntime()
                            .availableProcessors()));

        } else {

            width = Math.min(screenWidth, 1280);
            height = Math.min(screenHeight, 720);

            wineRam = 1536;

            cores = Math.min(4,
                    Math.max(2, Runtime.getRuntime()
                            .availableProcessors() - 1));
        }

        return new Profile(
                width,
                height,
                wineRam,
                cores,
                lowRam ? "balanced" : "performance",
                Build.VERSION.SDK_INT
        );
    }

    public static void save(Context context,
                            String appName,
                            Profile profile) {

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putInt(appName + "_width", profile.width)
                .putInt(appName + "_height", profile.height)
                .putInt(appName + "_ram", profile.ramMb)
                .putInt(appName + "_cores", profile.cores)
                .putString(appName + "_preset", profile.preset)
                .apply();
    }

    public static Profile load(Context context,
                               String appName) {

        Profile detected = detect(context);

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        return new Profile(
                prefs.getInt(
                        appName + "_width",
                        detected.width
                ),

                prefs.getInt(
                        appName + "_height",
                        detected.height
                ),

                prefs.getInt(
                        appName + "_ram",
                        detected.ramMb
                ),

                prefs.getInt(
                        appName + "_cores",
                        detected.cores
                ),

                prefs.getString(
                        appName + "_preset",
                        detected.preset
                ),

                Build.VERSION.SDK_INT
        );
    }

    public static final class Profile {

        public final int width;
        public final int height;
        public final int ramMb;
        public final int cores;
        public final int androidApi;
        public final String preset;

        public Profile(
                int width,
                int height,
                int ramMb,
                int cores,
                String preset,
                int androidApi
        ) {
            this.width = width;
            this.height = height;
            this.ramMb = ramMb;
            this.cores = cores;
            this.preset = preset;
            this.androidApi = androidApi;
        }

        public String resolution() {
            return width + "x" + height;
        }
    }
}
