/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.xml.codec;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.activation.UnsupportedDataTypeException;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common XML-related utility methods, which are not specific to a particular
 * JAXP API.
 */
public class XmlUtils {

  public static final XmlCodecProvider DEFAULT_XML_CODEC_PROVIDER = new XmlCodecProvider() {
    @Override
    public TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codecFor(final TypeDefinition<?> baseType) {
      return TypeDefinitionAwareCodec.from(baseType);
    }
  };

  private XmlUtils() {
  }

  private static final String BLANK = "";
  private static final Logger LOG = LoggerFactory.getLogger(XmlUtils.class);

  /**
   * Converts the composite node to xml using rpc input schema node
   * @param cNode
   * @param schemaContext
   * @return xml String
   */
  public static String inputCompositeNodeToXml(CompositeNode cNode, SchemaContext schemaContext){
    if(LOG.isDebugEnabled()) {
        LOG.debug("Converting input composite node to xml {}", cNode);
    }
    if (cNode == null) {
        return BLANK;
    }

    if(schemaContext == null) {
        return BLANK;
    }

    Document domTree = null;
    try {
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpc : rpcs) {
        if(rpc.getQName().equals(cNode.getNodeType())){
          if(LOG.isDebugEnabled()) {
              LOG.debug("Found the rpc definition from schema context matching with input composite node  {}", rpc.getQName());
          }
          CompositeNode inputContainer = cNode.getFirstCompositeByName(QName.create(cNode.getNodeType(), "input"));
          domTree = XmlDocumentUtils.toDocument(inputContainer, rpc.getInput(), XmlDocumentUtils.defaultValueCodecProvider());
          if(LOG.isDebugEnabled()) {
              LOG.debug("input composite node to document conversion complete, document is   {}", domTree);
          }
          break;
        }
      }

    } catch (UnsupportedDataTypeException e) {
      LOG.error("Error during translation of CompositeNode to Document", e);
    }
    return domTransformer(domTree);
  }

  /**
   * Converts the composite node to xml String using rpc output schema node
   * @param cNode
   * @param schemaContext
   * @return xml string
   */
  public static String outputCompositeNodeToXml(CompositeNode cNode, SchemaContext schemaContext){
    if(LOG.isDebugEnabled()) {
        LOG.debug("Converting output composite node to xml {}", cNode);
    }
    if (cNode == null) {
        return BLANK;
    }

    if(schemaContext == null) {
        return BLANK;
    }

    Document domTree = null;
    try {
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpc : rpcs) {
        if(rpc.getQName().equals(cNode.getNodeType())){
          if(LOG.isDebugEnabled()) {
              LOG.debug("Found the rpc definition from schema context matching with output composite node  {}", rpc.getQName());
          }
          CompositeNode outputContainer = cNode.getFirstCompositeByName(QName.create(cNode.getNodeType(), "output"));
          domTree = XmlDocumentUtils.toDocument(outputContainer, rpc.getOutput(), XmlDocumentUtils.defaultValueCodecProvider());
          if(LOG.isDebugEnabled()) {
              LOG.debug("output composite node to document conversion complete, document is   {}", domTree);
          }
          break;
        }
      }

    } catch (UnsupportedDataTypeException e) {
      LOG.error("Error during translation of CompositeNode to Document", e);
    }
    return domTransformer(domTree);
  }

  private static String domTransformer(Document domTree) {
    StringWriter writer = new StringWriter();
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(domTree), new StreamResult(writer));
    } catch (TransformerException e) {

      LOG.error("Error during translation of Document to OutputStream", e);
    }
    if(LOG.isDebugEnabled()) {
        LOG.debug("Document to string conversion complete, xml string is  {} ", writer.toString());
    }
    return writer.toString();
  }

  public static CompositeNode xmlToCompositeNode(String xml){
    if (xml==null || xml.length()==0) {
        return null;
    }

    Node<?> dataTree;
    try {
      dataTree = XmlTreeBuilder.buildDataTree(new ByteArrayInputStream(xml.getBytes()));
    } catch (XMLStreamException e) {
      LOG.error("Error during building data tree from XML", e);
      return null;
    }
    if (dataTree == null) {
      LOG.error("data tree is null");
      return null;
    }
    if (dataTree instanceof SimpleNode) {
      LOG.error("RPC XML was resolved as SimpleNode");
      return null;
    }
    return (CompositeNode) dataTree;
  }

  /**
   * Converts the xml to composite node using rpc input schema node
   * @param rpc
   * @param xml
   * @param schemaContext
   * @return CompositeNode object based on the input, if any of the input parameter is null, a null object is returned
   */
  public static CompositeNode inputXmlToCompositeNode(QName rpc, String xml,  SchemaContext schemaContext){
    if(LOG.isDebugEnabled()) {
        LOG.debug("Converting input xml to composite node {}", xml);
    }
    if (xml==null || xml.length()==0) {
        return null;
    }

    if(rpc == null) {
        return null;
    }

    if(schemaContext == null) {
        return null;
    }

    CompositeNode compositeNode = null;
    try {

      Document doc = XmlUtil.readXmlToDocument(xml);
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpcDef : rpcs) {
        if(rpcDef.getQName().equals(rpc)){
          if(LOG.isDebugEnabled()) {
              LOG.debug("found the rpc definition from schema context matching rpc  {}", rpc);
          }
          if(rpcDef.getInput() == null) {
            LOG.warn("found rpc definition's input is null");
            return null;
          }

          QName input = rpcDef.getInput().getQName();
          NodeList nodeList = doc.getElementsByTagNameNS(input.getNamespace().toString(), "input");
          if(nodeList == null || nodeList.getLength() < 1) {
            LOG.warn("xml does not have input entry. {}", xml);
            return null;
          }
          Element xmlData = (Element)nodeList.item(0);

          List<Node<?>> dataNodes = XmlDocumentUtils.toDomNodes(xmlData,
              Optional.of(rpcDef.getInput().getChildNodes()), schemaContext);
          if(LOG.isDebugEnabled()) {
              LOG.debug("Converted xml input to list of nodes  {}", dataNodes);
          }
          final CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
          it.setQName(rpc);
          it.add(ImmutableCompositeNode.create(input, dataNodes));
          compositeNode = it.build();
          break;
        }
      }
    } catch (SAXException e) {
      LOG.error("Error during building data tree from XML", e);
    } catch (IOException e) {
      LOG.error("Error during building data tree from XML", e);
    }
    if(LOG.isDebugEnabled()) {
        LOG.debug("Xml to composite node conversion complete {} ", compositeNode);
    }
    return compositeNode;
  }

  public static TypeDefinition<?> resolveBaseTypeFrom(final @Nonnull TypeDefinition<?> type) {
    TypeDefinition<?> superType = type;
    while (superType.getBaseType() != null) {
      superType = superType.getBaseType();
    }
    return superType;
  }

  /**
   * This code is picked from yangtools and modified to add type of instance identifier
   * output of instance identifier something like below for a flow ref composite node of type instance identifier,
   * which has path arguments with predicates, whose value is of type java.lang.short
   * <flow-ref xmlns:bgkj="urn:opendaylight:flow:inventory" xmlns:jdlk="urn:opendaylight:inventory">
   *   /jdlk:nodes/jdlk:node[jdlk:id='openflow:205558455098190@java.lang.String']
   *   /bgkj:table[bgkj:id='3@java.lang.Short']
   *   /bgkj:flow[bgkj:id='156@java.lang.String']
   * </flow-ref>
   *
   */

  public static String encodeIdentifier(final RandomPrefix prefixes, final YangInstanceIdentifier id) {
    StringBuilder textContent = new StringBuilder();
    for (PathArgument pathArgument : id.getPathArguments()) {
      textContent.append('/');
      textContent.append(prefixes.encodeQName(pathArgument.getNodeType()));
      if (pathArgument instanceof NodeIdentifierWithPredicates) {
        Map<QName, Object> predicates = ((NodeIdentifierWithPredicates) pathArgument).getKeyValues();

        for (QName keyValue : predicates.keySet()) {
          Object value = predicates.get(keyValue);
          String type = value.getClass().getName();
          String predicateValue = String.valueOf(value);
          textContent.append('[');
          textContent.append(prefixes.encodeQName(keyValue));
          textContent.append("='");
          textContent.append(predicateValue);
          textContent.append("@");
          textContent.append(type);
          textContent.append("']");
        }
      } else if (pathArgument instanceof NodeWithValue) {
        textContent.append("[.='");
        textContent.append(((NodeWithValue) pathArgument).getValue());
        textContent.append("']");
      }
    }

    return textContent.toString();
  }
}
