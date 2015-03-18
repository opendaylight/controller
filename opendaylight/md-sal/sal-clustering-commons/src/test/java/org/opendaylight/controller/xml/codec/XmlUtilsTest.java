/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.xml.codec;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Before;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

// FIXME : CompositeNode is not avaliable anymore so fix the test to use NormalizedNodeContainer ASAP
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

}
