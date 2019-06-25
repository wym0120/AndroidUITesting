package DFS;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class AuthorizationUtil {

    /**
     * 如果有需要权限的地方就把它全部允许并且重新识别页面
     *
     * @return 是否需要授权
     */
    public static boolean handlePermissionRequest(AppiumDriver driver, List<PageNode> nodeList) {
        List<PageNode> res = nodeList.stream()
                .filter(PageNode::isClickable)
                .filter(n -> n.getClassName().equals("android.widget.Button"))
                .filter(n -> n.getText().equals("允许") || n.getText().equals("始终允许"))
                .collect(Collectors.toList());
        if (res.size() != 0) {
            String xpath = res.get(0).getXpath();
            WebElement element = driver.findElementByXPath(xpath);
            element.click();
            while (true) {
                if (element.isDisplayed()) {
                    element.click();
                } else {
                    break;
                }
            }
            return true;
        }
        return false;
    }
}
