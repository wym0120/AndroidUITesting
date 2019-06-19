package DFS;

import org.dom4j.Element;

public class XpathUtil {
    /**
     * 获取某一个结点的xpath
     *
     * @param element
     * @return
     */
    public static String generateXpath(Element element) {
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
