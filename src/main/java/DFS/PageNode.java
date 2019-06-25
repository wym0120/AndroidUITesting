package DFS;

public class PageNode {
    //所属的页面编号
    private int belonging;
    //编号
    private int index;
    //当前节点深度
    private int depth;
    //包括了子组件的文本内容
    private String text;
    private String resourceID;
    private String contentDesc;
    //用于xpath的text
    private String xpathText;
    //类名,后续的行为基本按照这个类名来进行
    private String className;
    //可点击
    private boolean clickable;
    //可滑动
    private boolean scrollable;
    //xpath
    private String xpath;
    //是否已经被访问过
    private boolean visited;

    public boolean equals(PageNode node) {
        return this.xpath.equals(node.getXpath());
    }

    public String getResourceID() {
        return resourceID;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public String getContentDesc() {
        return contentDesc;
    }

    public void setContentDesc(String contentDesc) {
        this.contentDesc = contentDesc;
    }

    public String getXpathText() {
        return xpathText;
    }

    public void setXpathText(String xpathText) {
        this.xpathText = xpathText;
    }

    public int getBelonging() {
        return belonging;
    }

    public void setBelonging(int belonging) {
        this.belonging = belonging;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }
}
