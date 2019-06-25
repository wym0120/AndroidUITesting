package DFS;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class ScrollUtil {

    public static void moveToLeft(AppiumDriver driver, WebElement element) {
        Point p = element.getLocation();
        Dimension d = element.getSize();
        int width = d.getWidth();
        int height = d.getHeight();
        int startX = p.getX();
        int startY = p.getY();
        driver.swipe(startX + width / 4, startY + height / 2, startX + 3 * width / 4, startY + height / 2, 500);
    }

    public static void moveToRight(AppiumDriver driver, WebElement element) {
        Point p = element.getLocation();
        Dimension d = element.getSize();
        int width = d.getWidth();
        int height = d.getHeight();
        int startX = p.getX();
        int startY = p.getY();
        driver.swipe(startX + 3 * width / 4, startY + height / 2, startX + width / 4, startY + height / 2, 500);
    }

    public static void moveToUp(AppiumDriver driver, WebElement element) {
        Point p = element.getLocation();
        Dimension d = element.getSize();
        int width = d.getWidth();
        int height = d.getHeight();
        int startX = p.getX();
        int startY = p.getY();
        driver.swipe(startX + width / 2, startY + height / 4, startX + width / 2, startY + 3 * height / 4, 500);
    }

    public static void moveToDown(AppiumDriver driver, WebElement element) {
        Point p = element.getLocation();
        Dimension d = element.getSize();
        int width = d.getWidth();
        int height = d.getHeight();
        int startX = p.getX();
        int startY = p.getY();
        driver.swipe(startX + width / 2, startY + 3 * height / 4, startX + width / 2, startY + height / 4, 500);
    }
}
