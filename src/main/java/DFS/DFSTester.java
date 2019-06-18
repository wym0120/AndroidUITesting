package DFS;

import apk.ApkInfo;
import io.appium.java_client.AppiumDriver;
import org.dom4j.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class DFSTester {

    private StringBuilder xpathBuilder;
    private Stack<String> xpathStack;

    private String packageName;
    //很重要，用来指示哪一个页面正在被执行
    private int pagePointer;
    private int prePageHashCode;
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
        prePageHashCode = firstPage.getHashcode();
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

            currentNode.setXpath(simplifyXpath(xpathBuilder.toString()));
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
     * 获取某一个结点的xpath
     *
     * @param element
     * @return
     */
    private String generateXpath(Element element) {
        StringBuilder builder = new StringBuilder();
        builder.append(element.attributeValue("class") == null ? "" : element.attributeValue("class"));
        String resourceID = element.attributeValue("resource-id");
        String contentDesc = element.attributeValue("content-desc");
        String text = element.attributeValue("text");
        String index = element.attributeValue("index");
        if (resourceID != null && !resourceID.equals("")) {
            builder.append("[@resource-id='").append(element.attributeValue("resource-id")).append("']");
            return builder.toString();
        } else if (contentDesc != null && !contentDesc.equals("")) {
            builder.append("[@content-desc='").append(element.attributeValue("content-desc")).append("']");
            return builder.toString();
        } else if (text != null && !text.equals("")) {
            builder.append("[@text='").append(element.attributeValue("text")).append("']");
            return builder.toString();
        } else if (index != null && !index.equals("0")) {
            builder.append("[").append(element.attributeValue("index")).append("]");
            return builder.toString();
        }
        return builder.toString();
    }

    /**
     * 根据最复杂的xpath生成简化之后的相对路径
     *
     * @return
     */
    private String simplifyXpath(String xpath) {
        int index = xpath.lastIndexOf("[@");
        if (index != -1) {
            int j = index;
            while (xpath.charAt(j) != '/') {
                j--;
            }
            xpath = "/" + xpath.substring(j);
            return xpath;
        } else {
            return xpath;
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
        //处理可以点击的组件
        List<PageNode> clickableNodes = pageNodeList.stream().filter(PageNode::isClickable).collect(Collectors.toList());
        List<PageNode> editTextNodes = pageNodeList.stream()
                .filter(n -> n.getClassName().equals("android.widget.EditText") || n.getClassName().equals("android.widget.AutoCompleteTextView"))
                .collect(Collectors.toList());
        for (PageNode clickableNode : clickableNodes) {
//            System.out.println(clickableNode.getXpath());
            WebElement element = driver.findElementByXPath(clickableNode.getXpath());
            for (PageNode editNode : editTextNodes) {
//                System.out.println(editNode.getXpath());
                WebElement editElement = driver.findElementByXPath(editNode.getXpath());
                editElement.sendKeys("abc");
            }
            element.click();
        }
    }

    /**
     * 检查是否产生了新页面
     *
     * @return
     */
    private boolean checkGenerateNewPage() {
        Page currentPage = generatePage();
        currentPage.generateHashCode();
        return (currentPage.getHashcode() != prePageHashCode);
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
