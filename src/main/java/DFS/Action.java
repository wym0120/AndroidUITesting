package DFS;

public class Action {
    private int from;
    private int to;
    private ActionType actionType;
    private String xpath;
    //这个只针对滑动操作
    private SwipeDirection direction;

    /**
     * 点击事件
     *
     * @param actionType
     * @param xpath
     */
    public Action(ActionType actionType, String xpath) {
        this.actionType = actionType;
        this.xpath = xpath;
    }

    /**
     * 滑动事件
     *
     * @param actionType
     * @param xpath
     * @param direction
     */
    public Action(ActionType actionType, String xpath, SwipeDirection direction) {
        this.actionType = actionType;
        this.xpath = xpath;
        this.direction = direction;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getXpath() {
        return xpath;
    }

    public SwipeDirection getDirection() {
        return direction;
    }
}
