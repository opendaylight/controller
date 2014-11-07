/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.xml.codec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class XmlUtilsTest {

  private static final DocumentBuilderFactory BUILDERFACTORY;

  static {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setCoalescing(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setIgnoringComments(true);
    BUILDERFACTORY = factory;
  }

  private SchemaContext schema;
  private RpcDefinition testRpc;

  public static final String XML_CONTENT = "<add-flow xmlns=\"urn:opendaylight:controller:rpc:test\"><input xmlns=\"urn:opendaylight:controller:rpc:test\">" +
      "<id>flowid</id>" +
      "<flow xmlns:ltha=\"urn:opendaylight:controller:rpc:test\">/ltha:node/ltha:node1[ltha:id='3@java.lang.Short']</flow>" +
      "</input></add-flow>";

  @Before
  public void setUp() throws Exception {
    final ByteSource byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return XmlUtilsTest.this.getClass().getResourceAsStream("rpcTest.yang");
      }
    };
    schema = new YangParserImpl().parseSources(Lists.newArrayList(byteSource));
    final Module rpcTestModule = schema.getModules().iterator().next();
    testRpc = rpcTestModule.getRpcs().iterator().next();
  }

  @Test
  public void testNullInputXmlToComposite() {
    CompositeNode node = XmlUtils.inputXmlToCompositeNode(testRpc.getQName(), null, schema);
    Assert.assertNull(node);
  }

  @Test
  public void testNullRpcXmlToComposite() {
    CompositeNode node = XmlUtils.inputXmlToCompositeNode(null, XML_CONTENT, schema);
    Assert.assertNull(node);
  }

  @Test
  public void testInputXmlToCompositeNode() {
    CompositeNode node = XmlUtils.inputXmlToCompositeNode(testRpc.getQName(), XML_CONTENT, schema);
    ImmutableList<SimpleNode<?>> input = (ImmutableList<SimpleNode<?>>)node.getValue().get(0).getValue();
    SimpleNode<?> firstNode = input.get(0);

    Assert.assertEquals("id", firstNode.getNodeType().getLocalName());
    Assert.assertEquals("flowid", firstNode.getValue());

    SimpleNode<?> secondNode = input.get(1);
    Assert.assertEquals("flow", secondNode.getNodeType().getLocalName());

    YangInstanceIdentifier instance = (YangInstanceIdentifier) secondNode.getValue();
    Iterable<YangInstanceIdentifier.PathArgument> iterable = instance.getPathArguments();
    Iterator<YangInstanceIdentifier.PathArgument> it = iterable.iterator();
    YangInstanceIdentifier.NodeIdentifier firstPath = (YangInstanceIdentifier.NodeIdentifier) it.next();
    Assert.assertEquals("node", firstPath.getNodeType().getLocalName());
    YangInstanceIdentifier.NodeIdentifierWithPredicates secondPath = (YangInstanceIdentifier.NodeIdentifierWithPredicates)it.next();
    Short value = (Short)secondPath.getKeyValues().values().iterator().next();
    Short expected = 3;
    Assert.assertEquals(expected, value);
  }

  @Test
  public void testInputCompositeNodeToXML() {
    CompositeNode input = XmlUtils.inputXmlToCompositeNode(testRpc.getQName(), XML_CONTENT, schema);
    List<Node<?>> childNodes = new ArrayList<>();
    childNodes.add(input);
    QName rpcQName = schema.getOperations().iterator().next().getQName();
    CompositeNode node = new ImmutableCompositeNode(rpcQName, input.getValue(), ModifyAction.REPLACE);
    String xml = XmlUtils.inputCompositeNodeToXml(node, schema);
    Assert.assertNotNull(xml);
    Assert.assertTrue(xml.contains("3@java.lang.Short"));
  }

  @Test
  public void testNullCompositeNodeToXml(){
    String xml = XmlUtils.inputCompositeNodeToXml(null, schema);
    Assert.assertTrue(xml.isEmpty());
  }

  @Test
  public void testNullSchemaCompositeNodeToXml(){
    String xml = XmlUtils.inputCompositeNodeToXml(new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "child1"), new ArrayList<Node<?>>(), ModifyAction.REPLACE), null);
    Assert.assertTrue(xml.isEmpty());
  }


}
