/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.util;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeUtils;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Stack;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XmlUtils {

  private static final Logger _logger = LoggerFactory.getLogger(XmlUtils.class);

  public static String compositeNodeToXml(CompositeNode cNode/*, SchemaContext schemaContext*/){
    if (cNode == null) return new String();
    StringWriter writer = new StringWriter();

    // **** use XMLDocumentUtils method ****
    try {
//      DataNodeContainer dataNodeContainer = null;// ** check for NPE
//      Stack<DataSchemaNode> jobQueue = new Stack<>();
//      jobQueue.addAll(schemaContext.getDataDefinitions());
//
//      while (!jobQueue.isEmpty()) {
//        DataSchemaNode dataSchema = jobQueue.pop();
//
//        if (dataSchema instanceof DataNodeContainer) {
//          dataNodeContainer = (DataNodeContainer) dataSchema;
//          //jobQueue.addAll(((DataNodeContainer) dataSchema).getChildNodes());
//        }
//      }
      Document domTree = XmlDocumentUtils.toDocument(cNode,/*schemaContext,*/XmlDocumentUtils.defaultValueCodecProvider());
      try {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(domTree), new StreamResult(writer));
      } catch (TransformerException e) {
        _logger.error("Error during translation of Document to OutputStream", e);
      }

    } catch (UnsupportedDataTypeException ex) {
      _logger.error("Error during translation of Document to OutputStream", ex);

    }


    // *******************
    // ** commenting for now Document domTree = NodeUtils.buildShadowDomTree(cNode);



    return writer.toString();
  }

  public static CompositeNode xmlToCompositeNode(String xml/*, SchemaContext schemaContext*/){
    if (xml==null || xml.length()==0) return null;

    Node<?> dataTree =null;
    //**************

    try {
      java.io.InputStream sbis = new java.io.StringBufferInputStream(xml);

      javax.xml.parsers.DocumentBuilderFactory b = javax.xml.parsers.DocumentBuilderFactory.newInstance();
      b.setNamespaceAware(false);
      Document doc = null;
      javax.xml.parsers.DocumentBuilder db = null;

      db = b.newDocumentBuilder();
      doc = db.parse(sbis);
      org.w3c.dom.Element element = doc.getDocumentElement();
      //QName qname = XmlDocumentUtils.qNameFromElement(element); //** or use schemaContext.getQName()
      dataTree = XmlDocumentUtils.toDomNode(doc);
    } catch (ParserConfigurationException e) {
      _logger.error("Error during building data tree from XML", e);
    } catch (SAXException e) {
      _logger.error("Error during building data tree from XML", e);
    } catch (IOException e) {
      _logger.error("Error during building data tree from XML", e);
    }

    //**************


//    try {
//      dataTree = XmlTreeBuilder.buildDataTree(new ByteArrayInputStream(xml.getBytes()));
//    } catch (XMLStreamException e) {
//      _logger.error("Error during building data tree from XML", e);
//      return null;
//    }
    if (dataTree == null) {
      _logger.error("data tree is null");
      return null;
    }
    if (dataTree instanceof SimpleNode) {
      _logger.error("RPC XML was resolved as SimpleNode");
      return null;
    }
    return (CompositeNode) dataTree;
  }
}
