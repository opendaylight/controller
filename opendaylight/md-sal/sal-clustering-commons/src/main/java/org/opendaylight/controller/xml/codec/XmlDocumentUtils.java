/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.xml.codec;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.InstanceIdentifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class XmlDocumentUtils {
  private static class ElementWithSchemaContext {
    Element element;
    SchemaContext schemaContext;

    ElementWithSchemaContext(final Element element,final SchemaContext schemaContext) {
      this.schemaContext = schemaContext;
      this.element = element;
    }

    Element getElement() {
      return element;
    }

    SchemaContext getSchemaContext() {
      return schemaContext;
    }
  }

  public static final QName OPERATION_ATTRIBUTE_QNAME = QName.create(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), null, "operation");
  private static final Logger LOG = LoggerFactory.getLogger(XmlDocumentUtils.class);
  private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();

  /**
   * Converts Data DOM structure to XML Document for specified XML Codec Provider and corresponding
   * Data Node Container schema. The CompositeNode data parameter enters as root of Data DOM tree and will
   * be transformed to root in XML Document. Each element of Data DOM tree is compared against specified Data
   * Node Container Schema and transformed accordingly.
   *
   * @param data Data DOM root element
   * @param schema Data Node Container Schema
   * @param codecProvider XML Codec Provider
   * @return new instance of XML Document
   * @throws javax.activation.UnsupportedDataTypeException
   */
  public static Document toDocument(final CompositeNode data, final DataNodeContainer schema, final XmlCodecProvider codecProvider)
      throws UnsupportedDataTypeException {
    Preconditions.checkNotNull(data);
    Preconditions.checkNotNull(schema);

    if (!(schema instanceof ContainerSchemaNode || schema instanceof ListSchemaNode)) {
      throw new UnsupportedDataTypeException("Schema can be ContainerSchemaNode or ListSchemaNode. Other types are not supported yet.");
    }

    final DOMResult result = new DOMResult(getDocument());
    try {
      final XMLStreamWriter writer = FACTORY.createXMLStreamWriter(result);
      XmlStreamUtils.create(codecProvider).writeDocument(writer, data, (SchemaNode)schema);
      writer.close();
      return (Document)result.getNode();
    } catch (XMLStreamException e) {
      LOG.error("Failed to serialize data {}", data, e);
      return null;
    }
  }

  public static Document getDocument() {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    Document doc = null;
    try {
      DocumentBuilder bob = dbf.newDocumentBuilder();
      doc = bob.newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    return doc;
  }


  public static QName qNameFromElement(final Element xmlElement) {
    String namespace = xmlElement.getNamespaceURI();
    String localName = xmlElement.getLocalName();
    return QName.create(namespace != null ? URI.create(namespace) : null, null, localName);
  }

  private static Node<?> toNodeWithSchema(final Element xmlElement, final DataSchemaNode schema, final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
    checkQName(xmlElement, schema.getQName());
    if (schema instanceof DataNodeContainer) {
      return toCompositeNodeWithSchema(xmlElement, schema.getQName(), (DataNodeContainer) schema, schemaCtx);
    } else if (schema instanceof LeafSchemaNode) {
      return toSimpleNodeWithType(xmlElement, (LeafSchemaNode) schema, codecProvider,schemaCtx);
    } else if (schema instanceof LeafListSchemaNode) {
      return toSimpleNodeWithType(xmlElement, (LeafListSchemaNode) schema, codecProvider,schemaCtx);
    }
    return null;
  }



  private static Node<?> toSimpleNodeWithType(final Element xmlElement, final LeafSchemaNode schema,
                                              final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
    TypeDefinitionAwareCodec<? extends Object, ? extends TypeDefinition<?>> codec = codecProvider.codecFor(schema.getType());
    String text = xmlElement.getTextContent();
    Object value = null;
    if (codec != null) {
      LOG.debug("toSimpleNodeWithType: found codec, deserializing text {}", text);
      value = codec.deserialize(text);
    }

    final TypeDefinition<?> baseType = XmlUtils.resolveBaseTypeFrom(schema.getType());
    if (baseType instanceof InstanceIdentifierType) {
      LOG.debug("toSimpleNodeWithType: base type of node is instance identifier, deserializing element", xmlElement);
      value = InstanceIdentifierForXmlCodec.deserialize(xmlElement,schemaCtx);

    } else if(baseType instanceof IdentityrefTypeDefinition){
      LOG.debug("toSimpleNodeWithType: base type of node is IdentityrefTypeDefinition, deserializing element", xmlElement);
      value = InstanceIdentifierForXmlCodec.toIdentity(xmlElement.getTextContent(), xmlElement, schemaCtx);

    }

    if (value == null) {
      LOG.debug("toSimpleNodeWithType: no type found for element, returning just the text string value of element {}", xmlElement);
      value = xmlElement.getTextContent();
    }

    Optional<ModifyAction> modifyAction = getModifyOperationFromAttributes(xmlElement);
    return new SimpleNodeTOImpl<>(schema.getQName(), null, value, modifyAction.orNull());
  }

  private static Node<?> toSimpleNodeWithType(final Element xmlElement, final LeafListSchemaNode schema,
                                              final XmlCodecProvider codecProvider,final SchemaContext schemaCtx) {
    TypeDefinitionAwareCodec<? extends Object, ? extends TypeDefinition<?>> codec = codecProvider.codecFor(schema.getType());
    String text = xmlElement.getTextContent();
    Object value = null;
    if (codec != null) {
      LOG.debug("toSimpleNodeWithType: found codec, deserializing text {}", text);
      value = codec.deserialize(text);
    }

    final TypeDefinition<?> baseType = XmlUtils.resolveBaseTypeFrom(schema.getType());
    if (baseType instanceof InstanceIdentifierType) {
      LOG.debug("toSimpleNodeWithType: base type of node is instance identifier, deserializing element", xmlElement);
      value = InstanceIdentifierForXmlCodec.deserialize(xmlElement,schemaCtx);
    }

    if (value == null) {
      LOG.debug("toSimpleNodeWithType: no type found for element, returning just the text string value of element {}", xmlElement);
      value = xmlElement.getTextContent();
    }

    Optional<ModifyAction> modifyAction = getModifyOperationFromAttributes(xmlElement);
    return new SimpleNodeTOImpl<>(schema.getQName(), null, value, modifyAction.orNull());
  }

  private static Node<?> toCompositeNodeWithSchema(final Element xmlElement, final QName qName, final DataNodeContainer schema,
                                                   final SchemaContext schemaCtx) {
    List<Node<?>> values = toDomNodes(xmlElement, Optional.fromNullable(schema.getChildNodes()),schemaCtx);
    Optional<ModifyAction> modifyAction = getModifyOperationFromAttributes(xmlElement);
    return ImmutableCompositeNode.create(qName, values, modifyAction.orNull());
  }

  private static Optional<ModifyAction> getModifyOperationFromAttributes(final Element xmlElement) {
    Attr attributeNodeNS = xmlElement.getAttributeNodeNS(OPERATION_ATTRIBUTE_QNAME.getNamespace().toString(), OPERATION_ATTRIBUTE_QNAME.getLocalName());
    if(attributeNodeNS == null) {
      return Optional.absent();
    }

    ModifyAction action = ModifyAction.fromXmlValue(attributeNodeNS.getValue());
    Preconditions.checkArgument(action.isOnElementPermitted(), "Unexpected operation %s on %s", action, xmlElement);

    return Optional.of(action);
  }

  private static void checkQName(final Element xmlElement, final QName qName) {
    checkState(Objects.equal(xmlElement.getNamespaceURI(), qName.getNamespace().toString()));
    checkState(qName.getLocalName().equals(xmlElement.getLocalName()));
  }

  public static final Optional<DataSchemaNode> findFirstSchema(final QName qname, final Collection<DataSchemaNode> dataSchemaNode) {
    if (dataSchemaNode != null && !dataSchemaNode.isEmpty() && qname != null) {
      for (DataSchemaNode dsn : dataSchemaNode) {
        if (qname.isEqualWithoutRevision(dsn.getQName())) {
          return Optional.<DataSchemaNode> of(dsn);
        } else if (dsn instanceof ChoiceSchemaNode) {
          for (ChoiceCaseNode choiceCase : ((ChoiceSchemaNode) dsn).getCases()) {
            Optional<DataSchemaNode> foundDsn = findFirstSchema(qname, choiceCase.getChildNodes());
            if (foundDsn != null && foundDsn.isPresent()) {
              return foundDsn;
            }
          }
        }
      }
    }
    return Optional.absent();
  }

  private static Node<?> toDomNode(Element element) {
    QName qname = qNameFromElement(element);

    ImmutableList.Builder<Node<?>> values = ImmutableList.<Node<?>> builder();
    NodeList nodes = element.getChildNodes();
    boolean isSimpleObject = true;
    String value = null;
    for (int i = 0; i < nodes.getLength(); i++) {
      org.w3c.dom.Node child = nodes.item(i);
      if (child instanceof Element) {
        isSimpleObject = false;
        values.add(toDomNode((Element) child));
      }
      if (isSimpleObject && child instanceof org.w3c.dom.Text) {
        value = element.getTextContent();
        if (!Strings.isNullOrEmpty(value)) {
          isSimpleObject = true;
        }
      }
    }
    if (isSimpleObject) {
      return new SimpleNodeTOImpl<>(qname, null, value);
    }
    return ImmutableCompositeNode.create(qname, values.build());
  }

  public static List<Node<?>> toDomNodes(final Element element, final Optional<Collection<DataSchemaNode>> context,SchemaContext schemaCtx) {
    return forEachChild(element.getChildNodes(),schemaCtx, new Function<ElementWithSchemaContext, Optional<Node<?>>>() {

      @Override
      public Optional<Node<?>> apply(ElementWithSchemaContext input) {
        if (context.isPresent()) {
          QName partialQName = qNameFromElement(input.getElement());
          Optional<DataSchemaNode> schemaNode = XmlDocumentUtils.findFirstSchema(partialQName, context.get());
          if (schemaNode.isPresent()) {
            return Optional.<Node<?>> fromNullable(//
                toNodeWithSchema(input.getElement(), schemaNode.get(), XmlDocumentUtils.defaultValueCodecProvider(),input.getSchemaContext()));
          }
        }
        return Optional.<Node<?>> fromNullable(toDomNode(input.getElement()));
      }

    });

  }

  private static final <T> List<T> forEachChild(final NodeList nodes, final SchemaContext schemaContext, final Function<ElementWithSchemaContext, Optional<T>> forBody) {
    final int l = nodes.getLength();
    if (l == 0) {
      return ImmutableList.of();
    }

    final List<T> list = new ArrayList<>(l);
    for (int i = 0; i < l; i++) {
      org.w3c.dom.Node child = nodes.item(i);
      if (child instanceof Element) {
        Optional<T> result = forBody.apply(new ElementWithSchemaContext((Element) child,schemaContext));
        if (result.isPresent()) {
          list.add(result.get());
        }
      }
    }
    return ImmutableList.copyOf(list);
  }

  public static final XmlCodecProvider defaultValueCodecProvider() {
    return XmlUtils.DEFAULT_XML_CODEC_PROVIDER;
  }
}
