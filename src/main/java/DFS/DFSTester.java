package DFS;

import apk.ApkInfo;
import io.appium.java_client.AppiumDriver;
import org.dom4j.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import static DFS.XpathUtil.generateXpath;

public class DFSTester {

    private StringBuilder xpathBuilder;
    private Stack<String> xpathStack;

    private String packageName;
    //很重要，用来指示哪一个页面正在被执行
    private int pagePointer;
    //当前执行页的上一页的hashcode
    private int prePageHashCode;
    private List<Page> pageList;
    private AppiumDriver driver;
    private List<PageNode> nodeList;
    private boolean isNewPageGenerated;

    public void beginDFSTest(AppiumDriver driver, ApkInfo apkInfo) {
        //初始化测试参数
        packageName = apkInfo.getPackageName();
        this.driver = driver;
        //初始化页面
        pageList = new ArrayList<>();
        pagePointer = 0;
        prePageHashCode = 0;
        Page firstPage = generatePage();

        //第一次打开应用时候的权限检查
        if (checkPermissionRequest()) {
            Authorize();
            firstPage = generatePage();
        }
        //生成页面的hashcode
        firstPage.generateHashCode();
        pageList.add(firstPage);

        //接下来对这个page开始进行测试操作
        //这里应该是一直循环直到没有新的页面被加进来
        int pageCountBefore = pageList.size();
        int pageCountAfter = 0;
        while ((pageCountBefore != pageCountAfter) && (pagePointer != pageList.size())) {
            sendEvents(pageList.get(pagePointer));
        }
    }

    /**
     * 生成一个新的页面
     *
     * @return
     */
    private Page generatePage() {
        String pageXMLText = driver.getPageSource();
        Page page = new Page();
        try {
            Document doc = DocumentHelper.parseText(pageXMLText);
            Element root = doc.getRootElement();
            //建立新的页面
            page.setLoginPage(false);
            page.setPageIndex(pagePointer);
            page.setPointer(0);
            nodeList = new ArrayList<>();

            //初始化xpathBuilder
            xpathBuilder = new StringBuilder();
            xpathStack = new Stack<>();
            generatePageNode(root, 0, nodeList);
            page.setNodeList(nodeList);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return page;
    }

    /**
     * 生成页面上需要的结点
     *
     * @param node
     * @param depth
     * @param nodeList
     */
    private void generatePageNode(Element node, int depth, List<PageNode> nodeList) {
        //todo：这里关于imagebutton返回上一级界面要处理，最后先过滤后reverse访问这样比较稳
        PageNode currentNode = new PageNode();
        currentNode.setDepth(depth);
        currentNode.setClickable(Boolean.parseBoolean(node.attributeValue("clickable")));
        currentNode.setScrollable(Boolean.parseBoolean(node.attributeValue("scrollable")));
        boolean operational = currentNode.isClickable() || currentNode.isScrollable();
        currentNode.setClassName(node.attributeValue("class"));

        //xpath
        String path = generateXpath(node);
        xpathStack.push(path);
        xpathBuilder.append("/").append(path);

        //判断是否能进行任何操作
        if (operational) {
            String currentNodeText = node.attributeValue("text");
            if (currentNode.isScrollable()) {
                currentNode.setText(currentNodeText);
            } else {
                List<Element> child = node.elements();
                String totalText = child.stream().map(n -> n.attribute("text").getValue()).reduce(currentNodeText, (a, b) -> a + b);
                currentNode.setText(totalText);
            }

            currentNode.setXpath(XpathUtil.simplifyXpath(xpathBuilder.toString()));
            System.out.println(currentNode.getXpath());
            //加入list中
            nodeList.add(currentNode);
        }
        //深度+1
        depth++;
        //递归遍历当前节点所有的子节点
        List<Element> childNodes = node.elements();
        for (Element e : childNodes) {
            generatePageNode(e, depth, nodeList);
            //深度-1
            depth--;
            int length = xpathBuilder.length();
            xpathBuilder.delete(length - xpathStack.pop().length() - 1, length);
        }
    }

    /**
     * 发送事件
     *
     * @param oriPage
     */
    private void sendEvents(Page oriPage) {
        //检查是不是登录页面，如果是登陆页那么就特殊处理
        boolean isLoginPage = checkLoginPage();
        if (isLoginPage) {
            login();
            return;
        }
        List<PageNode> pageNodeList = oriPage.getNodeList();
        //处理可以滑动的组件
        List<PageNode> scrollableNodes = pageNodeList.stream().filter(PageNode::isScrollable).collect(Collectors.toList());
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String directions[] = {"down", "up", "left", "right"};
        for (PageNode scrollableNode : scrollableNodes) {
            WebElement element = driver.findElementByXPath(scrollableNode.getXpath());
            HashMap<String, String> scrollObject = new HashMap<>();
            for (String direction : directions) {
                scrollObject.put("direction", direction);
                scrollObject.put("element", ((RemoteWebElement) element).getId());
                js.executeScript("mobile: scroll", scrollObject);
            }
        }
        //处理可以点击的组件
        List<PageNode> clickableNodes = pageNodeList.stream()
                .filter(PageNode::isClickable)
                .filter(n -> !n.isVisited())
                .collect(Collectors.toList());
        List<PageNode> editTextNodes = pageNodeList.stream()
                .filter(n -> n.getClassName().equals("android.widget.EditText") || n.getClassName().equals("android.widget.AutoCompleteTextView"))
                .collect(Collectors.toList());
        for (PageNode clickableNode : clickableNodes) {
            WebElement element = driver.findElementByXPath(clickableNode.getXpath());
            for (PageNode editNode : editTextNodes) {
                WebElement editElement = driver.findElementByXPath(editNode.getXpath());
                editElement.sendKeys("abc");
                driver.sendKeyEvent(66);
                driver.hideKeyboard();
            }
            clickableNode.setVisited(true);
            element.click();
            //检查权限和登陆
            if (checkPermissionRequest()) Authorize();
            if (checkLoginPage()) {
                login();
                return;
            }
            //检查是否产生了新的页
            Page currentPage = generatePage();
            boolean isNewPageGenerated = checkGenerateNewPage(currentPage);
            if (isNewPageGenerated) {//出现了从没见过的页面

            } else {
                //判断是否为上一页
                if (currentPage.getHashcode() == prePageHashCode) {
                    //是上一页(比如点了非系统返回键的返回键就会出现这种情况)

                } else if (currentPage.getHashcode() == oriPage.getHashcode()) {
                    //还在当前页面
                } else {
                    //到达了访问过的页面(未必执行过)
                }
            }
        }
    }

    /**
     * @return 尝试回退之后的页面hashcode
     */
    private boolean tryReturn(Page currentPage, Action action) {
        driver.sendKeyEvent(4);
        Page unknownPageAfterTry = generatePage();
        unknownPageAfterTry.generateHashCode();
        int currentPageHashCode = currentPage.getHashcode();
        int unknownPageHashCode = unknownPageAfterTry.getHashcode();
        //todo：处理多重弹窗的逻辑不一定对
        //处理多重弹窗
        while (unknownPageHashCode == currentPageHashCode) {
            driver.sendKeyEvent(4);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            unknownPageAfterTry = generatePage();
            unknownPageAfterTry.generateHashCode();
            //弹窗通过返回键也处理不掉或者已经回不去了
            if (unknownPageHashCode == unknownPageAfterTry.getHashcode()) {
                pageList.add(unknownPageAfterTry);
                prePageHashCode = 0;
                return false;

            }
            unknownPageHashCode = unknownPageAfterTry.getHashcode();
        }
        return true;
    }

    /**
     * 检查是否产生了新页面
     *
     * @return
     */
    private boolean checkGenerateNewPage(Page currentPage) {
        currentPage.generateHashCode();
        return pageList.stream()
                .map(Page::getHashcode)
                .anyMatch(hashcode -> hashcode == currentPage.getHashcode());
    }

    /**
     * 如果有需要权限的地方就把它全部允许并且重新识别页面
     *
     * @return
     */
    private boolean checkPermissionRequest() {
        String by = "new UiSelector().className(\"android.widget.Button\").textMatches(\".*允许.*|.*确认.*|.*确定.*|ok|OK|\")";
        try {
            WebElement element = driver.findElementByAndroidUIAutomator(by);
            return element != null;
        } catch (NoSuchElementException e) {
            System.out.println("当前页面不需要授权");
        }
        return false;
    }

    /**
     * 保持点击授权按钮，防止多个权限要求
     */
    private void Authorize() {
        String by = "new UiSelector().className(\"android.widget.Button\").textMatches(\".*允许.*|.*确认.*|.*确定.*|ok|OK|\")";
        try {
            WebElement element = driver.findElementByAndroidUIAutomator(by);
            while (true) {
                if (element.isDisplayed()) {
                    element.click();
                } else {
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("当前页面不需要授权");
        }
    }

    /**
     * 检查是否有登录的按钮
     *
     * @return
     */
    private boolean checkLoginPage() {
        String by = "new UiSelector().className(\"android.widget.Button\").textMatches(\".*登陆.*|.*登錄.*|login\")";
        try {
            WebElement element = driver.findElementByAndroidUIAutomator(by);
            return element != null;
        } catch (NoSuchElementException e) {
            System.out.println("当前页面不是登录页面");
        }
        return false;
    }

    /**
     * 根据包名选择特定的登陆方法
     * 登陆之后不跳回原界面
     * 检查登陆之后的页面是否和之前的页面有重叠
     * 有重叠就把指针指向之前的页面
     * 没有就添加新的页面然后丢掉之前的页面
     */
    private void login() {

    }
}
