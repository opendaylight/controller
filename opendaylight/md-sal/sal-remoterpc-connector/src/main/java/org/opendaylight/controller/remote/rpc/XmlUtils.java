/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.activation.UnsupportedDataTypeException;
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
import java.util.Set;

public class XmlUtils {

  private static final Logger LOG = LoggerFactory.getLogger(XmlUtils.class);

  public static String inputCompositeNodeToXml(CompositeNode cNode, SchemaContext schemaContext){
    if (cNode == null) return new String();

    //Document domTree = NodeUtils.buildShadowDomTree(cNode);
    Document domTree = null;
    try {
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpc : rpcs) {
        if(rpc.getQName().equals(cNode.getNodeType())){
          domTree = XmlDocumentUtils.toDocument(cNode, rpc.getInput(), XmlDocumentUtils.defaultValueCodecProvider());
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

    //Document domTree = NodeUtils.buildShadowDomTree(cNode);
    Document domTree = null;
    try {
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpc : rpcs) {
        if(rpc.getQName().equals(cNode.getNodeType())){
          domTree = XmlDocumentUtils.toDocument(cNode, rpc.getInput(), XmlDocumentUtils.defaultValueCodecProvider());
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

    Node<?> dataTree = null;
    try {

      Document doc = XmlUtil.readXmlToDocument(xml);
      LOG.debug("xmlToCompositeNode Document is " + xml );
      Set<RpcDefinition> rpcs =  schemaContext.getOperations();
      for(RpcDefinition rpcDef : rpcs) {
        if(rpcDef.getQName().equals(rpc)){
          dataTree = XmlDocumentUtils.toDomNode(doc.getDocumentElement(), Optional.<DataSchemaNode>of(rpcDef.getInput()), Optional.of(XmlDocumentUtils.defaultValueCodecProvider()));
          break;
        }
      }
    } catch (SAXException e) {
      LOG.error("Error during building data tree from XML", e);
    } catch (IOException e) {
      LOG.error("Error during building data tree from XML", e);
    }

    LOG.debug("xmlToCompositeNode " + dataTree.toString());
    return (CompositeNode) dataTree;
  }
}
