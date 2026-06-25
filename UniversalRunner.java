package com.pentest.ide;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class UniversalRunner {
    private static PackageManager pm;

    public static void main(String[] args) {
        try {
            bypassHiddenApis();
            ActivityThread activityThread = ActivityThread.systemMain();
            Context context = activityThread.getSystemContext();
            pm = context.getPackageManager();

            System.out.println("READY");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String command;
            while ((command = reader.readLine()) != null) {
                command = command.trim();
                if (command.equalsIgnoreCase("EXIT")) break;
                handleCommand(command);
            }
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void handleCommand(String command) {
        try {
            String[] parts = command.split(":", 2);
            String action = parts[0];
            String target = parts.length > 1 ? parts[1] : "";

            switch (action) {
                case "GET_LABELS":
                    executeGetLabels();
                    break;
                case "GET_COMPONENTS":
                    executeGetComponents(target);
                    break;
                default:
                    System.out.println("ERROR\tUnknown command");
            }
            System.out.println("EOF"); 
        } catch (Exception e) {
            System.out.println("ERROR\t" + e.getMessage());
            System.out.println("EOF");
        }
    }

    private static void executeGetLabels() {
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo app : apps) {
            CharSequence label = pm.getApplicationLabel(app);
            String appLabel = (label != null) ? label.toString().trim() : app.packageName;
            System.out.println("DATA\t" + app.packageName + "\t" + appLabel);
        }
    }

    private static void executeGetComponents(String packageName) throws Exception {
        PackageInfo info = pm.getPackageInfo(packageName, 
            PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        if (info.activities != null) {
            for (int i=0; i < info.activities.length; i++) System.out.println("DATA\tactivity\t" + info.activities[i].name);
        }
        if (info.services != null) {
            for (int i=0; i < info.services.length; i++) System.out.println("DATA\tservice\t" + info.services[i].name);
        }
    }

    private static void bypassHiddenApis() {
        try {
            Class<?> vmRuntimeClass = Class.forName("dalvik.system.VMRuntime");
            java.lang.reflect.Method getRuntimeMethod = vmRuntimeClass.getDeclaredMethod("getRuntime");
            Object vmRuntime = getRuntimeMethod.invoke(null);
            java.lang.reflect.Method setExemptionsMethod = vmRuntimeClass.getDeclaredMethod("setHiddenApiExemptions", String[].class);
            setExemptionsMethod.invoke(vmRuntime, (Object) new String[]{"L"});
        } catch (Throwable t) {}
    }
}
