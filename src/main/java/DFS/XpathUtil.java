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
    public static String generateXpath(Element element) {
        StringBuilder builder = new StringBuilder();
        String className = element.attributeValue("class");
        if (className != null) {
            //对特殊符号特别处理
            if (!className.contains("$")) {
                builder.append(className);
            } else {
                builder.append(className.replace('$', '.'));

            }
        }
        String resourceID = element.attributeValue("resource-id");
        String contentDesc = element.attributeValue("content-desc");
        String text = element.attributeValue("text");
        String index = element.attributeValue("index");
        if (text != null && !text.equals("")) {
            builder.append("[@text='").append(text).append("']");
        } else if (index != null && !index.equals("0")) {
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
        } else if (contentDesc != null && !contentDesc.equals("")) {
            builder.append("[@content-desc='").append(contentDesc).append("']");
        }

        //resource有重复的可能性，不用

//        else if (resourceID != null && !resourceID.equals("")) {
//            builder.append("[@resource-id='").append(resourceID).append("']");
//        }
        return builder.toString();
    }

    /**
     * 根据最复杂的xpath生成简化之后的相对路径
     *
     * @return 简化后的路径
     */
    public static String simplifyXpath(String xpath) {
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
}
