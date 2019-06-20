package apk;

import apk.ApkInfo;

import java.io.*;

public class ApkUtil {
    public static final String LAUNCHABLE_ACTIVITY = "launchable";
    public static final String PACKAGE = "package";
    public static final String TARGET_SDK_VERSION = "targetSdkVersion";
    private static final String SPLIT_REGEX = "(: )|(=')|(' )|'";
    private ProcessBuilder builder;
    // aapt 所在目录
//    private static String aaptToolPath = "resources/aapt/";
    private static String aaptToolPath = "src/main/resources/aapt/";

    public ApkUtil() {
        builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
    }

    public ApkInfo parseApk(String apkPath) {
        String aaptTool = aaptToolPath + getAaptToolName();
        Process process = null;
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;

        File test = new File(aaptTool);
//        System.out.println("文件是否存在"+test.exists());
        try {
            process = builder.command(aaptTool, "d", "badging", apkPath).start();
            inputStream = process.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
            ApkInfo apkInfo = new ApkInfo();
            String temp = null;
            while ((temp = bufferedReader.readLine()) != null) {
                setApkInfoProperty(apkInfo, temp);
            }
            return apkInfo;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setApkInfoProperty(ApkInfo apkInfo, String source) {
        if (source.startsWith(LAUNCHABLE_ACTIVITY)) {
            apkInfo.setLaunchActivity(getPropertyInQuote(source));
        } else if (source.startsWith(PACKAGE)) {
            String[] packageInfo = source.split(SPLIT_REGEX);
            apkInfo.setPackageName(packageInfo[2]);
        } else if (source.startsWith(TARGET_SDK_VERSION)) {
            apkInfo.setTargetSdkVersion(getPlatformVersion(getPropertyInQuote(source)));
        }
    }

    private String getPropertyInQuote(String source) {
        int index = source.indexOf("'") + 1;
        return source.substring(index, source.indexOf('\'', index));
    }

    private String getAaptToolName() {
        String aaptToolName = "aapt";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            aaptToolName += ".exe";
        }
        return aaptToolName;
    }

    private static String getPlatformVersion(String APILevel){
        switch (APILevel){
            case "28":
                return "9";
            case "27":
                return "8.1";
            case "26":
                return "8.0";
            case "25":
                return "7.1.1";
            case "24":
                return "7.0";
            case "23":
                return "6.0";
            case "22":
                return "5.1";
            case "21":
                return "5.0";
            case "20":
                return "4.4W";
            case "19":
                return "4.4";
            case "18":
                return "4.3";
            case "17":
                return "4.2";
            case "16":
                return "4.1";
            case "15":
                return "4.0.3";
            case "14":
                return "4.0";
        }
        return null;
    }
}
