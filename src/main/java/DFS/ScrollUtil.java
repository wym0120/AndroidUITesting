package DFS;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class ScrollUtil {

    private int width;
    private int height;
    private int startX;
    private int startY;

    public ScrollUtil(WebElement element) {
        Point p = element.getLocation();
        Dimension d = element.getSize();
        width = d.getWidth();
        height = d.getHeight();
        startX = p.getX();
        startY = p.getY();
    }

    public void moveToLeft(AppiumDriver driver) {
        driver.swipe(startX + width / 4, startY + height / 2, startX + 3 * width / 4, startY + height / 2, 500);
    }

    public void moveToRight(AppiumDriver driver) {
        driver.swipe(startX + 3 * width / 4, startY + height / 2, startX + width / 4, startY + height / 2, 500);
    }

    public void moveToUp(AppiumDriver driver) {
        driver.swipe(startX + width / 2, startY + height / 4, startX + width / 2, startY + 3 * height / 4, 500);
    }

    public void moveToDown(AppiumDriver driver) {
        driver.swipe(startX + width / 2, startY + 3 * height / 4, startX + width / 2, startY + height / 4, 500);
    }
}
