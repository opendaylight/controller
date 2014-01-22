/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.util;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeUtils;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

public class XmlUtils {

  private static final Logger _logger = LoggerFactory.getLogger(XmlUtils.class);

  public static String compositeNodeToXml(CompositeNode cNode){
    if (cNode == null) return new String();

    Document domTree = NodeUtils.buildShadowDomTree(cNode);
    StringWriter writer = new StringWriter();
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(domTree), new StreamResult(writer));
    } catch (TransformerException e) {
      _logger.error("Error during translation of Document to OutputStream", e);
    }

    return writer.toString();
  }

  public static CompositeNode xmlToCompositeNode(String xml){
    if (xml==null || xml.length()==0) return null;

    Node<?> dataTree;
    try {
      dataTree = XmlTreeBuilder.buildDataTree(new ByteArrayInputStream(xml.getBytes()));
    } catch (XMLStreamException e) {
      _logger.error("Error during building data tree from XML", e);
      return null;
    }
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
