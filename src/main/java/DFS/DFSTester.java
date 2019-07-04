package DFS;

import apk.ApkInfo;
import io.appium.java_client.AppiumDriver;
import org.dom4j.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static DFS.AuthorizationUtil.handlePermissionRequest;
import static DFS.ScrollUtil.*;
import static DFS.XpathUtil.generateXpath;

public class DFSTester {

    private int SLEEP_TIME = 500;
    private StringBuilder xpathBuilder;
    private Stack<String> xpathStack;

    private String packageName;
    //用来指示哪一个页面正在被执行
    private int pagePointer;
    private List<Page> pageList;
    private AppiumDriver driver;
    private List<PageNode> nodeList;
    //用来处理"更多选项"的情况
    private Stack<PageNode> moreOptionsNodeStack;

    public void beginDFSTest(AppiumDriver driver, ApkInfo apkInfo) {
        //初始化测试参数
        packageName = apkInfo.getPackageName();
        this.driver = driver;
        moreOptionsNodeStack = new Stack<>();
        //初始化页面
        pageList = new ArrayList<>();
        pagePointer = 0;
        Page firstPage = generatePage();

        //第一次打开应用时候的权限检查
        if (handlePermissionRequest(driver, firstPage.getNodeList())) {
            firstPage = generatePage();
        }
        pageList.add(firstPage);
        int pageIndex = pageList.size() - 1;
        firstPage.getNodeList().forEach(n -> n.setBelonging(pageIndex));
        firstPage.setPageIndex(pageIndex);

        //接下来对这个page开始进行测试操作
        int pageCountBefore = pageList.size();
        int pageCountAfter = 0;
        int pagePointerBefore = pagePointer;
        int pagePointerAfter = 0;
        while (pageCountBefore != pageCountAfter || pagePointerBefore != pagePointerAfter) {
            pageCountBefore = pageList.size();
            pagePointerBefore = pagePointer;
            sendEvents(pageList.get(pagePointer));
            pageCountAfter = pageList.size();
            pagePointerAfter = pagePointer;
        }
        System.out.println("一次任务结束完毕");
    }

    /**
     * 生成一个新的页面
     *
     * @return 生成的页
     */
    private Page generatePage() {
        System.out.println(driver.currentActivity());
        final String[] pageXMLText = {""};
        ExecutorService exec = Executors.newFixedThreadPool(1);
        Callable<Boolean> call = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                pageXMLText[0] = driver.getPageSource();
                return true;
            }
        };
        Future<Boolean> future = exec.submit(call);
        try {
            future.get(5, TimeUnit.SECONDS);
            System.out.println("成功执行");
        } catch (InterruptedException e) {
            System.out.println("future在睡着时被打断");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println("future在尝试去的任务结果时出错");
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("future时间超时");
            driver.closeApp();
            driver.launchApp();
            try {
                Thread.sleep(SLEEP_TIME * 2);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
            System.out.println(driver.currentActivity());
            pageXMLText[0] = driver.getPageSource();
        } finally {
            exec.shutdown();
        }
        pageXMLText[0] = pageXMLText[0].replace("&#", "");
        Page page = new Page();
        try {
            Document doc = DocumentHelper.parseText(pageXMLText[0]);
            Element root = doc.getRootElement();
            //建立新的页面
            nodeList = new ArrayList<>();
            //初始化xpathBuilder
            xpathBuilder = new StringBuilder();
            xpathStack = new Stack<>();
            generatePageNode(root, 0, nodeList);
            page.setNodeList(nodeList);
            nodeList.forEach(n -> n.setIndex(nodeList.indexOf(n)));
            page.generateHashCode();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return page;
    }

    /**
     * 特殊的处理
     *
     * @param node 某一结点
     * @return 特殊处理过的页
     */
    private Page generatePage(PageNode node) {
        Page page = generatePage();
        page.setSpecialNode(node);
        return page;
    }

    /**
     * 生成页面上需要的结点
     *
     * @param node     根节点
     * @param depth    深度
     * @param nodeList 节点列表
     */
    private void generatePageNode(Element node, int depth, List<PageNode> nodeList) {
        PageNode currentNode = new PageNode();
        currentNode.setDepth(depth);
        currentNode.setClickable(Boolean.parseBoolean(node.attributeValue("clickable")));
        currentNode.setScrollable(Boolean.parseBoolean(node.attributeValue("scrollable")));
        boolean operational = currentNode.isClickable() || currentNode.isScrollable();
        String path = generateXpath(node, currentNode);
        String className = currentNode.getClassName();
        String contentDesc = currentNode.getContentDesc();
        //逼乎日报需要特别处理，用判断是否为首页来解决
        boolean isReturnButton;
        if (packageName.equals("com.white.bihudaily")) {
            isReturnButton =
                    (pagePointer != 0)
                            && (className != null && className.equals("android.widget.ImageButton"))
                            && (contentDesc != null && (contentDesc.contains("上一层")));
        } else {
            isReturnButton = (className != null && className.equals("android.widget.ImageButton"))
                    && (contentDesc != null && (contentDesc.contains("上一层") || contentDesc.equals("Navigate up")));
        }
        //是否可见
        boolean visible = !Boolean.parseBoolean(node.attributeValue("NAF"));
        //一部分的应用里使用英文search Search
        boolean isSearchButton = contentDesc != null && (contentDesc.contains("搜索") || contentDesc.contains("earch"));

        //xpath
        xpathStack.push(path);
        xpathBuilder.append("/").append(path);

        //判断是否能进行任何操作
        if (operational && visible && !isReturnButton && !isSearchButton) {
            String currentNodeText = node.attributeValue("text");
            if (currentNode.isScrollable()) {
                currentNode.setText(currentNodeText);
            } else {
                if (currentNodeText == null || currentNodeText.equals("")) {
                    String totalText = getTotalText(node);
                    currentNode.setText(totalText);
                } else {
                    currentNode.setText(currentNodeText);
                }
            }

            currentNode.setXpath(xpathBuilder.toString());
            //加入list中
            nodeList.add(currentNode);
        }
        //深度+1
        depth++;
        //递归遍历当前节点所有的子节点
        List<Element> childNodes = node.elements();
        for (Element e : childNodes) {
            generatePageNode(e, depth, nodeList);
            int length = xpathBuilder.length();
            xpathBuilder.delete(length - xpathStack.pop().length() - 1, length);
        }
    }


    /**
     * 获取该节点的子节点的文本，只要获取第一个非空
     *
     * @param root 节点
     * @return 文本
     */
    private String getTotalText(Element root) {
        Stack<Element> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Element tmp = stack.pop();
            String text = tmp.attributeValue("text");
            if (text != null && !text.equals("")) return text;
            List<Element> child = tmp.elements();
            for (Element element : child) {
                stack.push(element);
            }
        }
        return "";
    }

    /**
     * 发送事件
     *
     * @param oriPage 原始页面
     */
    private void sendEvents(Page oriPage) {
        //检查是不是登录页面，如果是登陆页那么就特殊处理
        boolean isLoginPage = handleLoginPage();
        if (isLoginPage) {
            System.out.println("当前是登陆页面");
            return;
        }
        List<PageNode> pageNodeList = oriPage.getNodeList();
        //处理可以点击的组件
        List<PageNode> highPriorityClickableNodes = getClickableNodeList(pageNodeList);
        List<PageNode> lowPriorityClickableNodes = new ArrayList<>();

        //同层次的不互相点击
        PageNode specialNode = oriPage.getSpecialNode();
        if (specialNode != null && oriPage.getNodeList().stream().anyMatch(n -> n.equals(specialNode))) {
            lowPriorityClickableNodes = new ArrayList<>(highPriorityClickableNodes);
            highPriorityClickableNodes = highPriorityClickableNodes.stream()
                    .filter(n -> !(n.getClassName().equals(specialNode.getClassName()) && n.getDepth() == specialNode.getDepth()))
                    .collect(Collectors.toList());
            lowPriorityClickableNodes = lowPriorityClickableNodes.stream()
                    .filter(n -> (n.getClassName().equals(specialNode.getClassName()) && n.getDepth() == specialNode.getDepth()))
                    .collect(Collectors.toList());
        }

        //这里特殊处理一下侧边栏
        if (packageName.equals("com.codeest.geeknews") || packageName.equals("org.gateshipone.odyssey") || packageName.equals("com.xiecc.seeWeather")) {
            for (int i = 0; i < pageNodeList.size(); i++) {
                String contentDesc = pageNodeList.get(i).getContentDesc();
                if (contentDesc != null && (contentDesc.equals("打开") || contentDesc.equals("Open navigation drawer"))) {
                    if (oriPage.getPageIndex() == 0) {
                        List<PageNode> tmp = new ArrayList<>();
                        tmp.add(pageNodeList.get(i));
                        tmp.addAll(lowPriorityClickableNodes);
                        lowPriorityClickableNodes = tmp;
                    } else {
                        lowPriorityClickableNodes.add(pageNodeList.get(i));
                    }
                    highPriorityClickableNodes.remove(pageNodeList.get(i));
                    break;
                }
            }
        }

        //处理高优先级
        for (PageNode clickableNode : highPriorityClickableNodes) {
            if (clickElement(oriPage, clickableNode, true)) return;
        }

        //处理可以滑动的组件
        List<PageNode> verticalScrollableNodes = pageNodeList.stream()
                .filter(PageNode::isScrollable)
                .filter(n -> !n.getClassName().equals("android.widget.HorizontalScrollView"))
                .collect(Collectors.toList());
        List<PageNode> horizonScrollableNodes = pageNodeList.stream()
                .filter(PageNode::isScrollable)
                .filter(n -> n.getClassName().equals("android.widget.HorizontalScrollView"))
                .collect(Collectors.toList());
        for (PageNode node : horizonScrollableNodes) {
            //指定滑动次数
            int times = 3;
            WebElement element = findElement(node);
            for (int i = 0; i < times; i++) {
                moveToRight(driver, element);
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < times; i++) {
                moveToLeft(driver, element);
            }
        }

        for (PageNode node : verticalScrollableNodes) {
            int times = 5;
            WebElement element = findElement(node);
            for (int i = 0; i < times; i++) {
                moveToDown(driver, element);
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < times * 2; i++) {
                moveToUp(driver, element);
            }
        }

        //处理低优先级
        for (PageNode clickableNode : lowPriorityClickableNodes) {
            if (clickElement(oriPage, clickableNode, true)) return;
        }

        //理更多选项点击出来的页面所有所有被访问过
        handlePageGeneratedByMoreOptions(oriPage);


        returnToLastPage(null);
    }

    private boolean clickAfterSwipe(Page oriPage, List<PageNode> pageNodeList) {
        Page page = generatePage();
        page.setPageIndex(oriPage.getPageIndex());
        List<PageNode> newClickableNodeList = page.getNodeList().stream().filter(PageNode::isClickable).collect(Collectors.toList());
        List<PageNode> oriClickableNodeList = pageNodeList.stream().filter(PageNode::isClickable).collect(Collectors.toList());
        newClickableNodeList = newClickableNodeList.stream().filter(n -> isNewClickableNode(n, oriClickableNodeList)).collect(Collectors.toList());
        for (PageNode clickableNode : newClickableNodeList) {
            if (clickElement(page, clickableNode, false)) return true;
        }
        return false;
    }

    /**
     * 判断下滑之后产生的节点是否和原来的有重复，真正的新的节点不应该包括在原来的那些里面
     *
     * @param node                 节点
     * @param oriClickableNodeList 原有的可以点击的节点列表
     * @return 是否为真的新生成的节点
     */
    private boolean isNewClickableNode(PageNode node, List<PageNode> oriClickableNodeList) {
        for (PageNode tmp : oriClickableNodeList) {
            if (tmp.equals(node) || tmp.getText().equals(node.getText())) return false;
        }
        return true;
    }

    /**
     * 不同的查找方式
     *
     * @param node 节点
     * @return webelement元素
     */
    private WebElement findElement(PageNode node) throws org.openqa.selenium.NoSuchElementException {
        List<WebElement> res = new ArrayList<>();
        if (node.getXpathText() != null && !node.getXpathText().equals("")) {
            res = driver.findElements(By.name(node.getXpathText()));
            if (res.size() == 1) return res.get(0);
        }
        if (node.getContentDesc() != null && !node.getContentDesc().equals("")) {
            res = driver.findElementsByAccessibilityId(node.getContentDesc());
            if (res.size() == 1) return res.get(0);
        }
        if (node.getResourceID() != null && !node.getResourceID().equals("")) {
            res = driver.findElementsById(node.getResourceID());
            if (res.size() == 1) return res.get(0);
        }
        return driver.findElementByXPath(node.getXpath());
    }

    /**
     * 进行点击操作
     *
     * @param oriPage       当前页面
     * @param clickableNode 页面节点列表
     * @return 是否需要结束
     */
    private boolean clickElement(Page oriPage, PageNode clickableNode, boolean needRecorded) {
        WebElement element;
        try {
            element = findElement(clickableNode);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            System.out.println("没找到按钮但是跳过了" + clickableNode.getXpath());
            return false;
        }
        boolean isMoreOptionsNode = checkMoreOptionNode(element);
        //点击按钮并判断是否为webview页面
        element.click();
        try {
            WebDriverWait explicitwait = new WebDriverWait(driver, 3);
            explicitwait.until(ExpectedConditions.visibilityOfElementLocated(By.className("android.webkit.WebView")));
        } catch (org.openqa.selenium.TimeoutException e) {
            System.out.println("不是一个webview");
        }

        //判断侧边栏
        boolean isSideBar = checkSideBar(clickableNode);
        if (!isMoreOptionsNode && !isSideBar && needRecorded) {
            pageList.get(oriPage.getPageIndex()).getNodeList().get(clickableNode.getIndex()).setVisited(true);
        }

        //检查是否产生了新的页，并且将入口的控件记录下来
        Page pageAfterClick = generatePage(clickableNode);
        //检查权限要求
        if (handlePermissionRequest(driver, pageAfterClick.getNodeList())) {
            pageAfterClick = generatePage(clickableNode);
        }

        //特殊处理更多选项这个操作
        if (isMoreOptionsNode) {
            moreOptionsNodeStack.push(clickableNode);
            pageAfterClick.setGeneratedByMoreOptionNode(true);
        }

        int pageAfterClickHashCode = pageAfterClick.getHashcode();
        boolean isNewPageGenerated = checkGenerateNewPage(pageAfterClick);
        if (isNewPageGenerated) {
            //这里要判断一下新的页是不是不该点的页，比如浏览器访问
            if (!checkInvalidPage(pageAfterClick)) {
                pageAfterClick.getNodeList().forEach(n -> n.setBelonging(pageList.size()));
                pageList.add(pageAfterClick);
                pageAfterClick.setPageIndex(pageList.size() - 1);
                pagePointer = pageList.indexOf(pageAfterClick);
                //检查是否是内部部分页面改变，借此实现已经访问过的公共组件不需要再次访问的功能
                if (pageAfterClick.getNodeList().stream().anyMatch(n -> n.equals(clickableNode))) {
                    List<String> xpathVisitedList = pageList.get(oriPage.getPageIndex()).getNodeList().stream()
                            .filter(PageNode::isVisited)
                            .map(PageNode::getXpath)
                            .collect(Collectors.toList());
                    pageAfterClick.getNodeList().stream()
                            .filter(n -> !(n.getDepth() == clickableNode.getDepth() && n.getClassName().equals(clickableNode.getClassName())))
                            .filter(n -> xpathVisitedList.contains(n.getXpath()))
                            .forEach(n -> n.setVisited(true));
                }
                return true;
            } else {
                boolean returnSuccess = returnToLastPage(oriPage);
                return !returnSuccess;
            }
        } else {
            if (pageAfterClickHashCode != oriPage.getHashcode()) {
                //到达了访问过的页面(未必执行过)
                resetPagePointer(pageAfterClickHashCode);
                return true;
            }
        }//还在当前页面,啥都不做继续循环
        return false;
    }

    /**
     * 处理更多选项点击出来的页面所有所有被访问过
     *
     * @param oriPage 现在所在的页面
     */
    private void handlePageGeneratedByMoreOptions(Page oriPage) {
        if (oriPage.isGeneratedByMoreOptionNode()) {
            if (getClickableNodeList(pageList.get(oriPage.getPageIndex()).getNodeList()).stream().allMatch(PageNode::isVisited)) {
                PageNode node = moreOptionsNodeStack.pop();
                int pageIndex = node.getBelonging();
                int nodeIndex = node.getIndex();
                pageList.get(pageIndex).getNodeList().get(nodeIndex).setVisited(true);
            }
        }
    }

    /**
     * 退回上一页
     *
     * @return 是否回到了上一页
     */
    private boolean returnToLastPage(Page oriPage) {
        driver.sendKeyEvent(4);
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Page pageAfterReturn = generatePage();
        //记录是不是在重新打开应用之后的指针
        int oriPointer = pagePointer;
        resetPagePointer(pageAfterReturn.getHashcode());
        int newPointer = pagePointer;
        if (newPointer == 0 && oriPointer != newPointer) {
            return false;
        }
        return true;
    }

    /**
     * 重置下一个要访问的页
     *
     * @param hashCode hashCode
     */
    private void resetPagePointer(int hashCode) {
        for (int i = 0; i < pageList.size(); i++) {
            if (pageList.get(i).getHashcode() == hashCode) {
                pagePointer = i;
                return;
            }
        }
    }

    /**
     * 检查是否产生了新页面
     *
     * @return 是否产生新页面
     */
    private boolean checkGenerateNewPage(Page currentPage) {
        return pageList.stream()
                .map(Page::getHashcode)
                .noneMatch(hashcode -> hashcode == currentPage.getHashcode());
    }

    /**
     * 如果有登陆的需求就处理登陆的需求并且做特殊的处理
     *
     * @return 是否有登陆的需求
     */

    private boolean handleLoginPage() {
        List<PageNode> res = nodeList.stream()
                .filter(PageNode::isClickable)
                .filter(n -> n.getClassName().equals("android.widget.Button"))
                .filter(n -> n.getText().equals("登录") || n.getText().equals("登陆") || n.getText().equals("登陸"))
                .collect(Collectors.toList());
        if (res.size() != 0) {
            login();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据包名选择特定的登陆方法
     */
    private void login() {
        WebElement userName, password, login;
        switch (packageName) {
            //哔哩哔哩
            case "com.hotbitmapgg.ohmybilibili":
                userName = driver.findElementByName("请输入您的哔哩哔哩账号");
                password = driver.findElementByName("请输入您的密码");
                userName.sendKeys("123456");
                password.sendKeys("123456");
                driver.hideKeyboard();
                login = driver.findElementById("com.hotbitmapgg.ohmybilibili:id/btn_login");
                login.click();
                break;

            //IT家园
            case "com.danmo.ithouse":
                userName = driver.findElementByAccessibilityId("com.danmo.ithouse:id/et_login_name");
                password = driver.findElementByAccessibilityId("com.danmo.ithouse:id/et_login_password");
                userName.sendKeys("18251830730");
                password.sendKeys("mima123456");
                driver.hideKeyboard();
                login = driver.findElementByName("登录");
                login.click();
                break;

            case "com.wingjay.android.jianshi":
                userName = driver.findElementByName("邮箱");
                password = driver.findElementByName("密码");
                userName.sendKeys("598251106@qq.com");
                password.sendKeys("jianshimima");
                driver.hideKeyboard();
                login = driver.findElementByName("登录");
                login.click();
                break;
            default:
                return;
        }

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Page page = generatePage();
        pageList.add(page);
        page.setPageIndex(pageList.size() - 1);
        pagePointer = pageList.size() - 1;
    }

    /**
     * 判断是否有需要调用第三方应用的入口，如果有就是非法的
     *
     * @param page 点击之后的页面
     * @return 是否非法
     */
    private boolean checkInvalidPage(Page page) {
        List<PageNode> nodeList = page.getNodeList();
        boolean judge0 = nodeList.stream().anyMatch(n -> (n.getClassName().equals("android.widget.Button") && (n.getText().equals("始终") || n.getText().equals("总是"))));
        boolean judge1 = nodeList.stream().anyMatch(n -> n.getText().equals("发送给朋友") ||
                n.getText().equals("备忘录") ||
                n.getText().equals("信息") ||
                n.getText().equals("蓝牙"));
        boolean judge2 = nodeList.stream().anyMatch(n -> n.getClassName().equals("android.webkit.WebView"));
        return judge0 || judge1 || judge2;
    }

    /**
     * 特殊的应用里面需要处理侧边栏
     * 目前有这个需要的是geeknews seaweather odyssey
     *
     * @param node 要点击的元素
     * @return 这个元素是否为侧边栏
     */
    private boolean checkSideBar(PageNode node) {
        switch (packageName) {
            case "com.codeest.geeknews":
                return (node.getContentDesc() != null) && (node.getContentDesc().equals("打开"));
            case "org.gateshipone.odyssey":
            case "com.xiecc.seeWeather":
                return (node.getContentDesc() != null) && (node.getContentDesc().equals("Open navigation drawer"));
            default:
                break;
        }
        return false;
    }

    /**
     * 判断这个节点是否是更多选项的节点
     *
     * @param element 节点
     * @return 是否为更多选项的点
     */
    private boolean checkMoreOptionNode(WebElement element) {
        String name = element.getAttribute("name");
        boolean judge0 = name.contains("更多选项");
        return judge0;
    }

    /**
     * 返回可点击的节点
     *
     * @param oriList 未经筛选的节点
     * @return 可点击的节点列表
     */
    private List<PageNode> getClickableNodeList(List<PageNode> oriList) {
        return oriList.stream()
                .filter(PageNode::isClickable)
                .filter(n -> !(n.getClassName().equals("android.widget.EditText") || n.getClassName().equals("android.widget.AutoCompleteTextView")))//这里过滤了编辑框防止弹出输入法
                .filter(n -> !n.isVisited())
                .filter(n -> !n.getText().contains("分享"))
                .filter(n -> !n.getText().contains("打开浏览器"))
                .filter(n -> !n.getText().contains("夜间模式"))
                .filter(n -> !n.getText().contains("日间模式"))
                .filter(n -> !n.getText().contains("支付宝"))
                .filter(n -> !(n.getText().contains("电影榜单") && packageName.equals("com.lhr.jiandou")))
                .filter(n -> !(n.getContentDesc() != null && n.getContentDesc().equals("Add playlist")))
                .filter(n -> !(n.getContentDesc() != null && n.getContentDesc().equals("Add Album")))
                .collect(Collectors.toList());
    }
}
