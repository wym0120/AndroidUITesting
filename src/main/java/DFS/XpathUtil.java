package DFS;

import org.dom4j.Element;

import java.util.List;
import java.util.stream.Collectors;


public class XpathUtil {
    /**
     * 获取某一个结点的xpath
     *
     * @param element 节点
     * @return xpath
     */
    public static String generateXpath(Element element, PageNode currentNode) {
        StringBuilder builder = new StringBuilder();
        String className = element.attributeValue("class") == null ? "" : element.attributeValue("class").replace('$', '.');
        builder.append(className);
        currentNode.setClassName(className);
        String resourceID = element.attributeValue("resource-id");
        String contentDesc = element.attributeValue("content-desc");
        String text = element.attributeValue("text");
        String index = element.attributeValue("index");
        if (index != null && !index.equals("0")) {
            //获得相对的Index
            List<Element> nodeList = element.getParent().elements();
            nodeList = nodeList.stream().filter(n -> n.attributeValue("class").equals(className)).collect(Collectors.toList());
            int trueIndex = Integer.parseInt(index);
            for (int i = 0; i < nodeList.size(); i++) {
                if (nodeList.get(i).attributeValue("index").equals(index)) {
                    //xpath的规则这里要+1
                    trueIndex = i + 1;
                    break;
                }
            }
            builder.append("[").append(trueIndex).append("]");
        }
        //顺便把节点的属性给设置了
        if (text != null && !text.equals("")) {
            currentNode.setXpathText(text);
        }
        if (contentDesc != null && !contentDesc.equals("")) {
            currentNode.setContentDesc(contentDesc);
        }
        if (resourceID != null && !resourceID.equals("")) {
            currentNode.setResourceID(resourceID);
        }
        return builder.toString();
    }
}
