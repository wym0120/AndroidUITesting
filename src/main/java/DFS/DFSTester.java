package DFS;

import apk.ApkInfo;
import io.appium.java_client.AppiumDriver;
import org.dom4j.*;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class DFSTester {
    private String packageName;
    //很重要，用来指示哪一个页面正在被执行
    private int pagePointer;
    private int prePageHashCode;
    private List<Page> pageList;
    private AppiumDriver driver;

    public void beginDFSTest(AppiumDriver driver, ApkInfo apkInfo) {
        //初始化测试参数
        packageName = apkInfo.getPackageName();
        this.driver = driver;
        //初始化页面
        pageList = new ArrayList<>();
        pagePointer = 0;
        Page firstPage = generatePage();
        //生成页面的hashcode
        firstPage.generateHashCode();
        prePageHashCode = firstPage.getHashcode();
        pageList.add(firstPage);

        //接下来对这个page开始进行测试操作
        //这里应该是一直循环直到没有新的页面被加进来
        sendEvents(firstPage);
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
            List<PageNode> nodeList = new ArrayList<>();
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
        currentNode.setText(node.attributeValue("text"));
        currentNode.setClassName(node.attributeValue("class"));
        currentNode.setClickable(Boolean.parseBoolean(node.attributeValue("clickable")));
        currentNode.setScrollable(Boolean.parseBoolean(node.attributeValue("scrollable")));
        currentNode.setSelectable(Boolean.parseBoolean(node.attributeValue("selectable")));
        //判断是否能进行任何操作或者给出关于页面的提示
        boolean operational = currentNode.isClickable() || currentNode.isScrollable() || currentNode.isSelectable();
        boolean hasInfo = (currentNode.getText() != null) && (!currentNode.getText().equals(""));
        if (operational || hasInfo) {
            //加入list中
            nodeList.add(currentNode);
        }
        //深度+1
        depth++;
        //递归遍历当前节点所有的子节点
        List<Element> listElement = node.elements();//所有一级子节点的list
        for (Element e : listElement) {//遍历所有一级子节点
            generatePageNode(e, depth, nodeList);
            //深度-1
            depth--;
        }
    }

    /**
     * 发送事件
     * @param oriPage
     */
    private void sendEvents(Page oriPage){
        //首先检查是否有权限要求
        boolean needPermission = checkPermissionRequest(oriPage);
        if(needPermission){
            /**
             * 这里要考虑是直接刚进应用就需要授权还是做了某个操作之后再授权
             * 前者没想明白
             * 后者可能跳转到新的页面
             */
//            pageList.remove(oriPage);
//            Page newPage = generatePage();
//            pageList.add(newPage);
        }

        //检查是否有弹窗

        //检查是不是登录页面，如果是登陆页那么就特殊处理
        //挨个遍历节点，当要点击button之前把所有能填充的地方都给填充了
        //根据组件类型进行操作，记录行为，检查页面是否有变动

        //如果跳转到了一个新的页面就把这个页面解析并且加进列表里面，然后回退到原页面，这里登陆页面不需要回退，弹窗页面处理完就需要删掉
        //根据不同的组件选择不同的跳转方式，记录下来跳转到的新页面的最后一次行为，方便做反向的行为或者再做一次这个行为
        //todo 创建一个Action类去记录这个行为
    }

    /**
     * 如果有需要权限的地方就把它全部允许并且重新识别页面
     * @param page
     * @return
     */
    private boolean checkPermissionRequest(Page page){
        String by = "new UiSelector().className(\"android.widget.Button\").textMatches(\".*允许.*\")";
        WebElement element = driver.findElementByAndroidUIAutomator(by);
        if(element!=null){
            keepClick(by);
            return true;
        }else{
            return false;
        }
    }

    private void keepClick(String by){
        WebElement element = driver.findElementByAndroidUIAutomator(by);
        while (true) {
            if (element.isDisplayed()) {
                element.click();
            } else {
                break;
            }
        }
    }

    //todo:是否需要检查业务弹窗还要再想一下
    private boolean checkBussinessRequest(Page page){
        return false;
    }

    private boolean checkLoginPage(Page page){
        return false;
    }

}
