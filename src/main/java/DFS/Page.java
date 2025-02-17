package DFS;

import java.util.List;

public class Page {
    //标识这个页是哪一个页
    private int pageIndex;
    //页面内的所有用的节点
    private List<PageNode> nodeList;
    //页面唯一标识，用来判断是否需要建立新的页
    private int hashcode;
    //用于处理深度优先里面的同级的button相互跳转的现象
    private PageNode specialNode;
    //用来标识这个页面是否为点击更多选项按钮之后产生的页
    private boolean generatedByMoreOptionNode;

    public boolean isGeneratedByMoreOptionNode() {
        return generatedByMoreOptionNode;
    }

    public void setGeneratedByMoreOptionNode(boolean generatedByMoreOptionNode) {
        this.generatedByMoreOptionNode = generatedByMoreOptionNode;
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

    public List<PageNode> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<PageNode> nodeList) {
        this.nodeList = nodeList;
    }

    public int getHashcode() {
        return hashcode;
    }

    public void generateHashCode() {
        String str = this.nodeList.stream()
                .map(n -> n.getClassName() + n.getDepth())
//                .map(n -> n.getClassName() + n.getDepth() + n.getText())
                .reduce("", (a, b) -> a + b);
        this.hashcode = str.hashCode();
    }
}
