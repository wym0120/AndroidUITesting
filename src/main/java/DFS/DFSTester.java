package DFS;

import apk.ApkInfo;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import org.dom4j.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.*;
import java.util.stream.Collectors;

import static DFS.XpathUtil.generateXpath;

public class DFSTester {

    private StringBuilder xpathBuilder;
    private Stack<String> xpathStack;

    private String packageName;
    //很重要，用来指示哪一个页面正在被执行
    private int pagePointer;
    //当前执行页的上一页的hashcode
    private List<Page> pageList;
    private AppiumDriver driver;
    private List<PageNode> nodeList;

    public void beginDFSTest(AppiumDriver driver, ApkInfo apkInfo) {
        //初始化测试参数
        packageName = apkInfo.getPackageName();
        this.driver = driver;
        //初始化页面
        pageList = new ArrayList<>();
        pagePointer = 0;
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
        //todo:这里要改一下判定的条件
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
//            page.setLoginPage(false);
//            page.setIndex(pagePointer);
            nodeList = new ArrayList<>();

            //初始化xpathBuilder
            xpathBuilder = new StringBuilder();
            xpathStack = new Stack<>();
            generatePageNode(root, 0, nodeList);
            page.setNodeList(nodeList);
            Collections.reverse(page.getNodeList());
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return page;
    }

    /**
     * 生成页面上需要的结点
     *
     * @param node 根节点
     * @param depth 深度
     * @param nodeList 节点列表
     */
    private void generatePageNode(Element node, int depth, List<PageNode> nodeList) {
        PageNode currentNode = new PageNode();
        currentNode.setDepth(depth);
        currentNode.setClickable(Boolean.parseBoolean(node.attributeValue("clickable")));
        currentNode.setScrollable(Boolean.parseBoolean(node.attributeValue("scrollable")));
        boolean operational = currentNode.isClickable() || currentNode.isScrollable();
        String className = node.attributeValue("class");
        String contentDesc = node.attributeValue("content-desc");
        currentNode.setClassName(className);
        boolean isReturnButton = (className != null && className.equals("android.widget.ImageButton")) && (contentDesc != null && contentDesc.contains("上一层"));

        //xpath
        String path = generateXpath(node);
        xpathStack.push(path);
        xpathBuilder.append("/").append(path);

        //判断是否能进行任何操作
        if (operational && !isReturnButton) {
            String currentNodeText = node.attributeValue("text");
            if (currentNode.isScrollable()) {
                currentNode.setText(currentNodeText);
            } else {
                List<Element> child = node.elements();
                String totalText = child.stream().map(n -> n.attribute("text").getValue()).reduce(currentNodeText, (a, b) -> a + b);
                currentNode.setText(totalText);
            }

            currentNode.setXpath(XpathUtil.simplifyXpath(xpathBuilder.toString()));
            //加入list中
            nodeList.add(currentNode);
            currentNode.setIndex(nodeList.size() - 1);
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
     * @param oriPage 原始页面
     */
    private void sendEvents(Page oriPage) {
        //检查是不是登录页面，如果是登陆页那么就特殊处理
        boolean isLoginPage = checkLoginPage();
        if (isLoginPage) {
            System.out.println("当前是登陆页面");
//            login();
            return;
        }
        List<PageNode> pageNodeList = oriPage.getNodeList();
//        //处理可以滑动的组件
//        List<PageNode> scrollableNodes = pageNodeList.stream().filter(PageNode::isScrollable).collect(Collectors.toList());
//        JavascriptExecutor js = (JavascriptExecutor) driver;
//        String directions[] = {"down", "up", "left", "right"};
//        for (PageNode scrollableNode : scrollableNodes) {
//            WebElement element = driver.findElementByXPath(scrollableNode.getXpath());
//            HashMap<String, String> scrollObject = new HashMap<>();
//            for (String direction : directions) {
//                scrollObject.put("direction", direction);
//                scrollObject.put("element", ((RemoteWebElement) element).getId());
//                js.executeScript("mobile: scroll", scrollObject);
//            }
//        }
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
            oriPage.getNodeList().get(clickableNode.getIndex()).setVisited(true);
            element.click();
            //检查权限和登陆
            if (checkPermissionRequest()) Authorize();
            if (checkLoginPage()) {
                login();
                return;
            }
            //检查是否产生了新的页
            Page pageAfterClick = generatePage();
            int pageAfterClickHashCode = pageAfterClick.getHashcode();
            boolean isNewPageGenerated = checkGenerateNewPage(pageAfterClick);
            if (isNewPageGenerated) {
                //出现了从没见过的页面
                if (tryReturn(pageAfterClick)) {
                    pageList.add(pageAfterClick);
                    pagePointer = pageList.indexOf(pageAfterClick);
                    element.click();
                    return;
                } else {
                    return;
                }
            } else {
                if (pageAfterClickHashCode != oriPage.getHashcode()) {
                    //到达了访问过的页面(未必执行过)
                    for (int i = 0; i < pageList.size(); i++) {
                        if (pageList.get(i).getHashcode() == pageAfterClickHashCode) {
                            pagePointer = i;
                            return;
                        }
                    }
                }  //还在当前页面,啥都不做继续循环
            }
        }
        driver.sendKeyEvent(4);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 尝试回退之后的页面hashcode
     */
    private boolean tryReturn(Page pageAfterClick) {
        //进行一次回退尝试
        driver.sendKeyEvent(4);
        Page unknownPageAfterTry = generatePage();
        unknownPageAfterTry.generateHashCode();
        int pageAfterClickHashCode = pageAfterClick.getHashcode();
        int unknownPageHashCode = unknownPageAfterTry.getHashcode();
        if (pageAfterClickHashCode == unknownPageHashCode) {
            //回退失败
            pageList.add(unknownPageAfterTry);
            pagePointer = pageList.indexOf(unknownPageAfterTry);
            return false;
        } else {
            //处理多重弹窗
            while (!checkGenerateNewPage(unknownPageAfterTry)) {
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
                    pagePointer = pageList.indexOf(unknownPageAfterTry);
                    return false;
                }
                unknownPageHashCode = unknownPageAfterTry.getHashcode();
            }
        }
        return true;
    }

    /**
     * 检查是否产生了新页面
     *
     * @return 是否产生新页面
     */
    private boolean checkGenerateNewPage(Page currentPage) {
        currentPage.generateHashCode();
        return pageList.stream()
                .map(Page::getHashcode)
                .noneMatch(hashcode -> hashcode == currentPage.getHashcode());
    }

    /**
     * 如果有需要权限的地方就把它全部允许并且重新识别页面
     *
     * @return 是否需要授权
     */
    private boolean checkPermissionRequest() {
        String by = "new UiSelector().className(\"android.widget.Button\").textMatches(\".*允许.*|.*确认.*|.*确定.*|ok|OK|\")";
        try {
            WebElement element = driver.findElementByAndroidUIAutomator(by);
            return element != null;
        } catch (NoSuchElementException e) {
//            System.out.println("当前页面不需要授权");
        }
        return false;
    }

    /**
     * 保持点击授权按钮，防止多个权限要求
     */
    private void Authorize() {
        String by = "new UiSelector().className(\"android.widget.Button\").childSelector(new UiSelector().textMatches(\".*允许.*|.*确认.*|.*确定.*|ok|OK|\"))";
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
//            System.out.println("当前页面不需要授权");
        }
    }

    /**
     * 检查是否有登录的按钮
     *
     * @return 是否有登陆按钮
     */
    private boolean checkLoginPage() {
        String by = "new UiSelector().className(\"android.widget.Button\").childSelector(new UiSelector().textMatches(\".*登陆.*\"))";
        try {
//            WebElement element = driver.findElementByAndroidUIAutomator(by);
            WebElement element = driver.findElement(MobileBy.AndroidUIAutomator(by));
            return element != null;
        } catch (NoSuchElementException e) {
//            System.out.println("当前页面不是登录页面");
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
