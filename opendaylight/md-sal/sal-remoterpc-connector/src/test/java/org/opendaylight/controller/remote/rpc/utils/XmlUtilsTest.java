/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.utils;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


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

  public static final String XML_CONTENT = "<input xmlns=\"urn:opendaylight:controller:rpc:test\">" +
      "<id>flowid</id>" +
      "<flow xmlns:ltha=\"urn:opendaylight:controller:rpc:test\">/ltha:node/ltha:node1[ltha:id='3@java.lang.Short']</flow>" +
      "</input>";

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
  public void testInputXmlToCompositeNode() {
    CompositeNode node = XmlUtils.inputXmlToCompositeNode(testRpc.getQName(), XML_CONTENT, schema);
    ImmutableList<SimpleNode> input = (ImmutableList)node.getValue().get(0).getValue();
    SimpleNode firstNode = input.get(0);

    Assert.assertEquals("id", firstNode.getNodeType().getLocalName());
    Assert.assertEquals("flowid", firstNode.getValue());

    SimpleNode secondNode = input.get(1);
    Assert.assertEquals("flow", secondNode.getNodeType().getLocalName());

    YangInstanceIdentifier instance = (YangInstanceIdentifier) secondNode.getValue();
    Iterable<YangInstanceIdentifier.PathArgument> iterable = instance.getPathArguments();
    Iterator it = iterable.iterator();
    YangInstanceIdentifier.NodeIdentifier firstPath = (YangInstanceIdentifier.NodeIdentifier) it.next();
    Assert.assertEquals("node", firstPath.getNodeType().getLocalName());
    YangInstanceIdentifier.NodeIdentifierWithPredicates secondPath = (YangInstanceIdentifier.NodeIdentifierWithPredicates)it.next();
    Short value = (Short)secondPath.getKeyValues().values().iterator().next();
    Short expected = 3;
    Assert.assertEquals(expected, value);
  }


}
