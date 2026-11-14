package com.example.fortnitenews;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootHelper {

    public interface RootCallback {
        void onRootResult(boolean isRooted, String output);
    }

    /**
     * يطلب صلاحيات الروت من Magisk / KernelSU عند استدعائه وينفذ أمر الاختبار
     */
    public static void requestRootAndExecute(final String command, final RootCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process process = null;
                DataOutputStream os = null;
                BufferedReader reader = null;
                StringBuilder output = new StringBuilder();
                boolean success = false;

                try {
                    // تشغيل عملية su لإظهار نافذة Magisk للمستخدم
                    process = Runtime.getRuntime().exec("su");
                    os = new DataOutputStream(process.getOutputStream());
                    
                    os.writeBytes(command + "\n");
                    os.writeBytes("exit\n");
                    os.flush();

                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        success = true;
                    }
                } catch (Exception e) {
                    output.append("Error: ").append(e.getMessage());
                    success = false;
                } finally {
                    try {
                        if (os != null) os.close();
                        if (reader != null) reader.close();
                        if (process != null) process.destroy();
                    } catch (Exception ignored) {}
                }

                if (callback != null) {
                    callback.onRootResult(success, output.toString().trim());
                }
            }
        }).start();
    }
}
