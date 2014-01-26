/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import org.junit.Test;
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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;

public class SerilizationTest {

  private static final Logger _logger = LoggerFactory.getLogger(SerilizationTest.class);

  public void fromXml() {
  }

  @Test
  public void toXml() throws FileNotFoundException {

    InputStream xmlStream = SerilizationTest.class.getResourceAsStream("/FourSimpleChildren.xml");
    StringWriter writer = new StringWriter();

    CompositeNode data = loadCompositeNode(xmlStream);
    Document domTree = NodeUtils.buildShadowDomTree(data);
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      //transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      //transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      transformer.transform(new DOMSource(domTree), new StreamResult(writer));
    } catch (TransformerException e) {
      _logger.error("Error during translation of Document to OutputStream", e);
    }

    _logger.info("Parsed xml [{}]", writer.toString());
  }

  //Note to self:  Stolen from TestUtils
  ///Users/alefan/odl/controller4/opendaylight/md-sal/sal-rest-connector/src/test/java/org/opendaylight/controller/sal/restconf/impl/test/TestUtils.java
  // Figure out how to include TestUtils through pom ...was getting errors
  private CompositeNode loadCompositeNode(InputStream xmlInputStream) throws FileNotFoundException {
    if (xmlInputStream == null) {
      throw new IllegalArgumentException();
    }
    Node<?> dataTree;
    try {
      dataTree = XmlTreeBuilder.buildDataTree(xmlInputStream);
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
