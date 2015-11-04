package org.opendaylight.controller.sal.connect.netconf.sal;

import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAnyXmlNodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by mmarsale on 5.11.2015.
 */
public class SchemalessTransformer {


    public AnyXmlNode getEditCfgRpc(final YangInstanceIdentifier path, final AnyXmlNode data, final String targetString) {

        Element dataElement = ((Document) data.getValue().getNode()).getDocumentElement();
        dataElement = createParentStructure(path, dataElement);

        final Document editCfgDocument = XmlUtil.newDocument();
        final Element editConfig = editCfgDocument.createElementNS("edit-config", "urn:ietf:params:xml:ns:netconf:base:1.0");
        final Element target = editCfgDocument.createElement("target");
        editConfig.appendChild(target);
        target.appendChild(editCfgDocument.createElement(targetString));
        final Element config = editCfgDocument.createElement("config");
        editConfig.appendChild(config);
        config.appendChild(dataElement);
        // TODO add default operation

        return new ImmutableAnyXmlNodeBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
            QName.cachedReference(QName.create("urn:ietf:params:xml:ns:netconf:base:1.0", "edit-config")))).withValue(
            new DOMSource(editConfig)).build();
    }

    private Element createParentStructure(final YangInstanceIdentifier path, final Element dataElement) {
        final Document parentStructure = XmlUtil.newDocument();


        Element lastElement = parentStructure.getDocumentElement();
        for (YangInstanceIdentifier.PathArgument o : path.getPathArguments()) {

            final QName nodeType = o.getNodeType();

            final Element elementNS = parentStructure
                .createElementNS(nodeType.getNamespace().toString(), nodeType.getLocalName());
            lastElement.appendChild(
                elementNS);

            lastElement = elementNS;

        }

        lastElement.appendChild(dataElement);
        return parentStructure.getDocumentElement();

    }

    public NormalizedNode<?, ?> getCommitInput() {

        final Document editCfgDocument = XmlUtil.newDocument();
        final Element commit = editCfgDocument.createElementNS("commit", "urn:ietf:params:xml:ns:netconf:base:1.0");

        return new ImmutableAnyXmlNodeBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
            QName.cachedReference(QName.create("urn:ietf:params:xml:ns:netconf:base:1.0", "edit-config"))))
            .withValue(new DOMSource(commit)).build();
    }

    public AnyXmlNode getGetConfigStructure() {

    }
}
