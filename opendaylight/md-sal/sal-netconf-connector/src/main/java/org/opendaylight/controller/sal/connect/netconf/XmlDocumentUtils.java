package org.opendaylight.controller.sal.connect.netconf;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlDocumentUtils {

    public static CompositeNode toCompositeNode(Document doc) {
        return (CompositeNode) toCompositeNode(doc.getDocumentElement());
    }

    private static Node<?> toCompositeNode(Element element) {
        String orgNamespace = element.getNamespaceURI();
        URI biNamespace = null;
        if (orgNamespace != null) {
            biNamespace = URI.create(orgNamespace);
        }
        QName qname = new QName(biNamespace, element.getLocalName());

        List<Node<?>> values = new ArrayList<>();
        NodeList nodes = element.getChildNodes();
        boolean isSimpleObject = false;
        String value = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node child = nodes.item(i);
            if (child instanceof Element) {
                isSimpleObject = false;
                values.add(toCompositeNode((Element) child));
            }
            if (!isSimpleObject && child instanceof org.w3c.dom.Text) {
                value = element.getTextContent();
                if (value.matches(".*\\w.*")) {
                    isSimpleObject = true;
                    break;
                }
            }
        }

        if (isSimpleObject) {
            return new SimpleNodeTOImpl<>(qname, null, value);
        }
        return new CompositeNodeTOImpl(qname, null, values);
    }
}
