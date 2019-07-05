import DFS.DFSTester;
import apk.ApkInfo;
import apk.ApkUtil;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.*;

public class Main {
    private static AppiumDriver driver = null;

    public static void main(String[] args){
        //获取命令行参数
        File appPath = new File(args[0]);
//        File appPath = new File("src/main/resources/apk/" + args[0]);
        String udid = args[1];
        String serverPort = args[2];
        int runtime = Integer.parseInt(args[3]);
        //获取ApkInfo
        ApkUtil apkUtil = new ApkUtil();
        ApkInfo apkInfo = apkUtil.parseApk(appPath.getAbsolutePath());
        //初始化appium
        initAppiumTest(appPath, udid, serverPort, apkInfo, runtime);
        //执行测试
        autoTest(runtime,apkInfo);

    }

    public static AppiumDriver initAppiumTest(File appPath, String udid, String serverPort, ApkInfo apkInfo, int runtime) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName","myPhone");
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("platformVersion", apkInfo.getTargetSdkVersion());
        capabilities.setCapability("udid", udid);
        capabilities.setCapability("appPackage", apkInfo.getPackageName());
        capabilities.setCapability("appActivity", apkInfo.getLaunchActivity());
        capabilities.setCapability("noSign", "true");
        capabilities.setCapability("noReset", "true");
        capabilities.setCapability("app", appPath.getAbsolutePath());
        capabilities.setCapability("automationName", "uiautomator2");
        //开启unicode，支持输入中文和特殊字符
        capabilities.setCapability("unicodeKeyboard", true);
        //重置键盘
        capabilities.setCapability("resetKeyboard", true);
        //设置session持续时间
        capabilities.setCapability("newCommandTimeout", runtime);

        String url = "http://127.0.0.1:"+serverPort+"/wd/hub";
        try {
            driver = new AppiumDriver(new URL(url), capabilities);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return driver;
    }

    public static void autoTest(int runtime,ApkInfo apkInfo){
        final ExecutorService exec = Executors.newFixedThreadPool(1);
        Callable<Boolean> call = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //生成测试用例
                while (true) {
                    DFSTester tester = new DFSTester();
                    Thread.sleep(5000);
                    tester.beginDFSTest(driver, apkInfo, true);
                    driver.closeApp();
                    driver.launchApp();
                    Thread.sleep(5000);
                    tester.beginDFSTest(driver, apkInfo, false);
                    driver.closeApp();
                    driver.launchApp();
                    Thread.sleep(5000);
                }
            }
        };
        Future<Boolean> future = exec.submit(call);
        try {
            future.get(runtime, TimeUnit.SECONDS);
            System.out.println("结束任务");
        } catch (InterruptedException e) {
            System.out.println("future在睡着时被打断");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println("future在尝试去的任务结果时出错");
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("future时间超时");
            e.printStackTrace();
        }finally{
            exec.shutdown();
        }
    }
}
