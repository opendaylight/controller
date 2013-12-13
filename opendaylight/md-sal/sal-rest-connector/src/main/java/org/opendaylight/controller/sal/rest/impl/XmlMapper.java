package org.opendaylight.controller.sal.rest.impl;

import java.util.Set;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.YangNode;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Preconditions;

public class XmlMapper {
    
    private final Logger logger = LoggerFactory.getLogger(XmlMapper.class); 

    public Document write(CompositeNode data, DataNodeContainer schema) throws UnsupportedDataTypeException {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(schema);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = null;
        try {
            DocumentBuilder bob = dbf.newDocumentBuilder();
            doc = bob.newDocument();
        } catch (ParserConfigurationException e) {
            return null;
        }

        if (schema instanceof ContainerSchemaNode || schema instanceof ListSchemaNode) {
            doc.appendChild(translateToXmlAndReturnRootElement(doc, data, schema));
            return doc;
        } else {
            throw new UnsupportedDataTypeException(
                    "Schema can be ContainerSchemaNode or ListSchemaNode. Other types are not supported yet.");
        }
    }

    private Element translateToXmlAndReturnRootElement(Document doc, Node<?> data, YangNode schema)
            throws UnsupportedDataTypeException {
        QName dataType = data.getNodeType();
        Element itemEl = doc.createElementNS(dataType.getNamespace().toString(), dataType.getLocalName());
        if (data instanceof SimpleNode<?>) {
            if (schema instanceof LeafListSchemaNode) {
                writeValueOfNodeByType(itemEl, (SimpleNode<?>) data, ((LeafListSchemaNode) schema).getType());
            } else if (schema instanceof LeafSchemaNode) {
                writeValueOfNodeByType(itemEl, (SimpleNode<?>) data, ((LeafSchemaNode) schema).getType());
            } else {
                Object value = data.getValue();
                if (value != null) {
                    itemEl.setTextContent(String.valueOf(value));
                }
            }
        } else { // CompositeNode
            for (Node<?> child : ((CompositeNode) data).getChildren()) {
                DataSchemaNode childSchema = null;
                if(schema != null){
                    childSchema = findFirstSchemaForNode(child, ((DataNodeContainer) schema).getChildNodes());
                    if (logger.isDebugEnabled()) {
                        if (childSchema == null) {
                            logger.debug("Probably the data node \"" + ((child == null) ? "" : child.getNodeType().getLocalName())
                                    + "\" is not conform to schema");
                        }
                    }
                }
                itemEl.appendChild(translateToXmlAndReturnRootElement(doc, child, childSchema));
            }
        }
        return itemEl;
    }

    private void writeValueOfNodeByType(Element element, SimpleNode<?> node, TypeDefinition<?> type) {

        TypeDefinition<?> baseType = resolveBaseTypeFrom(type);

        if (baseType instanceof IdentityrefTypeDefinition) {
            if (node.getValue() instanceof QName) {
                IdentityValuesDTO valueDTO = (IdentityValuesDTO) RestCodec.from(type).serialize(node.getValue());
                IdentityValue value = valueDTO.getValuesWithNamespaces().get(0);
                String prefix = "x";
                if (value.getPrefix() != null && !value.getPrefix().isEmpty()) {
                    prefix = value.getPrefix();
                }
                element.setAttribute("xmlns:" + prefix, value.getNamespace());
                element.setTextContent(prefix + ":" + value.getValue());
            } else {
                logger.debug("Value of " + baseType.getQName().getNamespace() + ":"
                        + baseType.getQName().getLocalName() + " is not instance of " + QName.class + " but is " + node.getValue().getClass());
                element.setTextContent(String.valueOf(node.getValue()));
            }
        } else {
            Object value = node.getValue();
            if (value != null) {
                element.setTextContent(String.valueOf(value));
            }
        }
    }

    private DataSchemaNode findFirstSchemaForNode(Node<?> node, Set<DataSchemaNode> dataSchemaNode) {
        if (dataSchemaNode != null && node != null) {
            for (DataSchemaNode dsn : dataSchemaNode) {
                if (node.getNodeType().getLocalName().equals(dsn.getQName().getLocalName())) {
                    return dsn;
                } else if (dsn instanceof ChoiceNode) {
                    for (ChoiceCaseNode choiceCase : ((ChoiceNode) dsn).getCases()) {
                        DataSchemaNode foundDsn = findFirstSchemaForNode(node, choiceCase.getChildNodes());
                        if (foundDsn != null) {
                            return foundDsn;
                        }
                    }
                }
            }
        }
        return null;
    }

    private TypeDefinition<?> resolveBaseTypeFrom(TypeDefinition<?> type) {
        return type.getBaseType() != null ? resolveBaseTypeFrom(type.getBaseType()) : type;
    }

}
