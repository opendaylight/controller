/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.zeromq.consumer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.Hashtable;
import java.util.concurrent.*;

import org.opendaylight.controller.sal.core.api.AbstractConsumer;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;

import javax.xml.stream.XMLStreamException;

public class ExampleConsumer extends AbstractConsumer {

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");

  private ConsumerSession session;

  private ServiceRegistration<ExampleConsumer> thisReg;
  private Logger _logger = LoggerFactory.getLogger(ExampleConsumer.class);

  @Override
  public void onSessionInitiated(ConsumerSession session) {
    this.session = session;
  }

  public RpcResult<CompositeNode> invokeRpc(QName qname, CompositeNode input) {
    _logger.info("Invoking RPC:[{}] with Input:[{}]", qname.getLocalName(), input);
    RpcResult<CompositeNode> result = null;
    Future<RpcResult<CompositeNode>> future = ExampleConsumer.this.session.rpc(qname, input);
    try {
      result = future.get();
    } catch (Exception e) {
      e.printStackTrace();
    }
    _logger.info("Returning Result:[{}]", result);
    return result;
  }

  @Override
  protected void startImpl(BundleContext context){
    thisReg = context.registerService(ExampleConsumer.class, this, new Hashtable<String,String>());
  }
  @Override
  protected void stopImpl(BundleContext context) {
    super.stopImpl(context);
    thisReg.unregister();
  }

  public CompositeNode getValidCompositeNodeWithOneSimpleChild() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/OneSimpleChild.xml");
    return loadCompositeNode(xmlStream);
  }

  public CompositeNode getValidCompositeNodeWithTwoSimpleChildren() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/TwoSimpleChildren.xml");
    return loadCompositeNode(xmlStream);
  }

  public CompositeNode getValidCompositeNodeWithFourSimpleChildren() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/FourSimpleChildren.xml");
    return loadCompositeNode(xmlStream);
  }

  public CompositeNode getValidCompositeNodeWithOneSimpleOneCompositeChild() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/OneSimpleOneCompositeChild.xml");
    return loadCompositeNode(xmlStream);
  }

  public CompositeNode getValidCompositeNodeWithTwoCompositeChildren() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/TwoCompositeChildren.xml");
    return loadCompositeNode(xmlStream);
  }

  public CompositeNode getInvalidCompositeNodeSimpleChild() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/InvalidSimpleChild.xml");
    return loadCompositeNode(xmlStream);
  }

  public CompositeNode getInvalidCompositeNodeCompositeChild() throws FileNotFoundException {
    InputStream xmlStream = ExampleConsumer.class.getResourceAsStream("/InvalidCompositeChild.xml");
    return loadCompositeNode(xmlStream);
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
