/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.utils;

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
  private static final Logger LOG = LoggerFactory.getLogger(XmlUtils.class);

  public static String inputCompositeNodeToXml(CompositeNode cNode, SchemaContext schemaContext){
    if (cNode == null) return new String();

    Document domTree = null;
    try {
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpc : rpcs) {
        if(rpc.getQName().equals(cNode.getNodeType())){
          CompositeNode inputContainer = cNode.getFirstCompositeByName(QName.create(cNode.getNodeType(), "input"));
          domTree = XmlDocumentUtils.toDocument(inputContainer, rpc.getInput(), XmlDocumentUtils.defaultValueCodecProvider());
          break;
        }
      }

    } catch (UnsupportedDataTypeException e) {
      LOG.error("Error during translation of CompositeNode to Document", e);
    }
    return domTransformer(domTree);
  }

  public static String outputCompositeNodeToXml(CompositeNode cNode, SchemaContext schemaContext){
    if (cNode == null) return new String();

    Document domTree = null;
    try {
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpc : rpcs) {
        if(rpc.getQName().equals(cNode.getNodeType())){
          domTree = XmlDocumentUtils.toDocument(cNode, rpc.getOutput(), XmlDocumentUtils.defaultValueCodecProvider());
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
    LOG.debug("compositeNodeToXml " + writer.toString());

    return writer.toString();
  }

  public static CompositeNode xmlToCompositeNode(String xml){
    if (xml==null || xml.length()==0) return null;

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

  public static CompositeNode inputXmlToCompositeNode(QName rpc, String xml,  SchemaContext schemaContext){
    if (xml==null || xml.length()==0) return null;

    CompositeNode compositeNode = null;
    try {

      Document doc = XmlUtil.readXmlToDocument(xml);
      LOG.debug("xmlToCompositeNode Document is " + xml );
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpcDef : rpcs) {
        if(rpcDef.getQName().equals(rpc)){
          QName input = rpcDef.getInput().getQName();
          Element xmlData = (Element) doc.getElementsByTagNameNS(input.getNamespace().toString(), "input").item(0);
          List<Node<?>> dataNodes = XmlDocumentUtils.toDomNodes(xmlData,
              Optional.of(rpcDef.getInput().getChildNodes()), schemaContext);
          final CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
          it.setQName(input);
          it.add(ImmutableCompositeNode.create(input, dataNodes));
          compositeNode = it.toInstance();
          break;
        }
      }
    } catch (SAXException e) {
      LOG.error("Error during building data tree from XML", e);
    } catch (IOException e) {
      LOG.error("Error during building data tree from XML", e);
    }

    LOG.debug("xmlToCompositeNode " + compositeNode.toString());
    return compositeNode;
  }

    public static TypeDefinition<?> resolveBaseTypeFrom(final @Nonnull TypeDefinition<?> type) {
        TypeDefinition<?> superType = type;
        while (superType.getBaseType() != null) {
            superType = superType.getBaseType();
        }
        return superType;
    }

    static String encodeIdentifier(final RandomPrefix prefixes, final YangInstanceIdentifier id) {
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
