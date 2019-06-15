package apk;

public class ApkInfo {
    private String launchActivity;
    private String packageName;
    private String targetSdkVersion;

    public String getLaunchActivity() {
        return launchActivity;
    }

    public void setLaunchActivity(String launchActivity) {
        this.launchActivity = launchActivity;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTargetSdkVersion() {
        return targetSdkVersion;
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
    }

    @Override
    public String toString(){
        return "apkInfo:launchActivity = " + this.launchActivity + ",packageName = " + this.packageName + ",targetSdkVersion = "+this.targetSdkVersion;
    }
}
