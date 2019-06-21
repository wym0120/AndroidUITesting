package DFS;

import java.util.List;

public class Page {
    //标识这个页是哪一个页
    private int pageIndex;
    //判断是否结束访问了
    private boolean finished;
    //是否为登陆页面，如果监测到了相应字段，那么就调用特定的注册登陆函数(这里只能特殊处理每一个应用
    private boolean isLoginPage;
    //页面内的所有用的节点
    private List<PageNode> nodeList;
    //页面唯一标识，用来判断是否需要建立新的页
    private int hashcode;
    //用于处理深度优先里面的同级的button相互跳转的现象
    private PageNode specialNode;


    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public PageNode getSpecialNode() {
        return specialNode;
    }

    public void setSpecialNode(PageNode specialNode) {
        this.specialNode = specialNode;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

//    public int getPointer() {
//        return pointer;
//    }
//
//    public void setPointer(int pointer) {
//        this.pointer = pointer;
//    }

    public boolean isLoginPage() {
        return isLoginPage;
    }

    public void setLoginPage(boolean loginPage) {
        isLoginPage = loginPage;
    }

    public List<PageNode> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<PageNode> nodeList) {
        this.nodeList = nodeList;
    }

    public int getHashcode() {
        return hashcode;
    }

    public void generateHashCode(){
        String str = this.nodeList.stream()
                .map(n->n.getClassName()+ n.getDepth())
                .reduce("", (a, b) -> a + b);
        System.out.println(str);
        this.hashcode = str.hashCode();
    }
}
