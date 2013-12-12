/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sample.zeromq.test.it;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.opendaylight.controller.sal.connector.remoterpc.RemoteRpcClient;

import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sample.zeromq.consumer.ExampleConsumer;
import org.opendaylight.controller.sample.zeromq.provider.ExampleProvider;

import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import javax.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;

import static org.opendaylight.controller.test.sal.binding.it.TestHelper.baseModelBundles;
import static org.opendaylight.controller.test.sal.binding.it.TestHelper.bindingAwareSalBundles;

import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
public class RouterTest {

  private Logger _logger = LoggerFactory.getLogger(RouterTest.class);

  public static final String ODL = "org.opendaylight.controller";
  public static final String YANG = "org.opendaylight.yangtools";
  public static final String SAMPLE = "org.opendaylight.controller.tests";
  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");


  @Inject
  org.osgi.framework.BundleContext ctx;

  @Inject
  @Filter(timeout=60*1000)
  Broker broker;
  
  private ZMQ.Context zmqCtx = ZMQ.context(1);
  //private Server router;
  //private ExampleProvider provider;

  //@Test
  public void testInvokeRpc() throws Exception{
    //Thread.sleep(1000);
    //Send announcement
    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);

    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);

    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);


    _logger.debug("Provider sends announcement [{}]", "heartbeat");
    provider.announce(QNAME);
    ServiceReference routerRef = ctx.getServiceReference(RemoteRpcClient.class);
    RemoteRpcClient router = (RemoteRpcClient) ctx.getService(routerRef);
    _logger.debug("Found router[{}]", router);
    _logger.debug("Invoking RPC [{}]", QNAME);
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = router.invokeRpc(QNAME, consumer.getValidCompositeNodeWithOneSimpleChild());
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testInvokeRpcWithValidSimpleNode() throws Exception{
    //Thread.sleep(1500);

    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);
    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);

    // Provider sends announcement
    _logger.debug("Provider sends announcement [{}]", "heartbeat");
    provider.announce(QNAME);
    // Consumer invokes RPC
    _logger.debug("Invoking RPC [{}]", QNAME);
    CompositeNode input = consumer.getValidCompositeNodeWithOneSimpleChild();
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, input);
      Assert.assertNotNull(result);
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertTrue(result.isSuccessful());
      Assert.assertNotNull(result.getResult());
      Assert.assertEquals(0, result.getErrors().size());
      Assert.assertEquals(input.getChildren().size()+1, result.getResult().getChildren().size());
    }
  }

  @Test
  public void testInvokeRpcWithValidSimpleNodes() throws Exception{
    //Thread.sleep(1500);

    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);
    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);

    // Provider sends announcement
    _logger.debug("Provider sends announcement [{}]", "heartbeat");
    provider.announce(QNAME);
    // Consumer invokes RPC
    _logger.debug("Invoking RPC [{}]", QNAME);
    CompositeNode input = consumer.getValidCompositeNodeWithFourSimpleChildren();
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, input);
      Assert.assertNotNull(result);
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertTrue(result.isSuccessful());
      Assert.assertNotNull(result.getResult());
      Assert.assertEquals(0, result.getErrors().size());
      Assert.assertEquals(input.getChildren().size()+1, result.getResult().getChildren().size());
    }
  }

  @Test
  public void testInvokeRpcWithValidCompositeNode() throws Exception{
    //Thread.sleep(1500);

    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);
    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);

    // Provider sends announcement
    _logger.debug("Provider sends announcement [{}]", "heartbeat");
    provider.announce(QNAME);
    // Consumer invokes RPC
    _logger.debug("Invoking RPC [{}]", QNAME);
    CompositeNode input = consumer.getValidCompositeNodeWithTwoCompositeChildren();
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, input);
      Assert.assertNotNull(result);
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertTrue(result.isSuccessful());
      Assert.assertNotNull(result.getResult());
      Assert.assertEquals(0, result.getErrors().size());
      Assert.assertEquals(input.getChildren().size()+1, result.getResult().getChildren().size());
    }
  }

  @Test
  public void testInvokeRpcWithNullInput() throws Exception{
    //Thread.sleep(1500);

    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);
    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);

    // Provider sends announcement
    _logger.debug("Provider sends announcement [{}]", QNAME.getLocalName());
    provider.announce(QNAME);
    // Consumer invokes RPC
    _logger.debug("Invoking RPC [{}]", QNAME);
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, null);
      Assert.assertNotNull(result);
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertFalse(result.isSuccessful());
      Assert.assertNull(result.getResult());
      Assert.assertEquals(1, result.getErrors().size());
      Assert.assertEquals(RpcError.ErrorSeverity.WARNING, ((RpcError)result.getErrors().toArray()[0]).getSeverity());
    }
  }

  @Test
  public void testInvokeRpcWithInvalidSimpleNode() throws Exception{
    //Thread.sleep(1500);

    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);
    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);

    // Provider sends announcement
    _logger.debug("Provider sends announcement [{}]", QNAME.getLocalName());
    provider.announce(QNAME);
    // Consumer invokes RPC
    _logger.debug("Invoking RPC [{}]", QNAME);
    CompositeNode input = consumer.getInvalidCompositeNodeSimpleChild();
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, input);
      Assert.assertNotNull(result);
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertFalse(result.isSuccessful());
      Assert.assertNull(result.getResult());
      Assert.assertEquals(1, result.getErrors().size());
      Assert.assertEquals(RpcError.ErrorSeverity.ERROR, ((RpcError)result.getErrors().toArray()[0]).getSeverity());
    }
  }

  @Test
  public void testInvokeRpcWithInvalidCompositeNode() throws Exception{
    //Thread.sleep(1500);

    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    Assert.assertNotNull(providerRef);
    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
    Assert.assertNotNull(provider);
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    Assert.assertNotNull(consumerRef);
    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
    Assert.assertNotNull(consumer);

    // Provider sends announcement
    _logger.debug("Provider sends announcement [{}]", QNAME.getLocalName());
    provider.announce(QNAME);
    // Consumer invokes RPC
    _logger.debug("Invoking RPC [{}]", QNAME);
    CompositeNode input = consumer.getInvalidCompositeNodeCompositeChild();
    for (int i = 0; i < 3; i++) {
      RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, input);
      Assert.assertNotNull(result);
      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
      Assert.assertFalse(result.isSuccessful());
      Assert.assertNull(result.getResult());
      Assert.assertEquals(1, result.getErrors().size());
      Assert.assertEquals(RpcError.ErrorSeverity.ERROR, ((RpcError)result.getErrors().toArray()[0]).getSeverity());
    }
  }

  //@Test
  // This method is UNTESTED -- need to get around the bundling issues before I know if this even work
//  public void testInvokeRpcWithValidCompositeNode() throws Exception{
//    Thread.sleep(10000);
//    //Send announcement
//    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
//    Assert.assertNotNull(providerRef);
//
//    ExampleProvider provider = (ExampleProvider)ctx.getService(providerRef);
//    Assert.assertNotNull(provider);
//
//    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
//    Assert.assertNotNull(consumerRef);
//
//    ExampleConsumer consumer = (ExampleConsumer)ctx.getService(consumerRef);
//    Assert.assertNotNull(consumer);
//
//    _logger.debug("Provider sends announcement [{}]", "heartbeat");
//    provider.announce(QNAME);
//    ServiceReference routerRef = ctx.getServiceReference(Client.class);
//    Client router = (Client) ctx.getService(routerRef);
//    _logger.debug("Found router[{}]", router);
//    _logger.debug("Invoking RPC [{}]", QNAME);
//    for (int i = 0; i < 3; i++) {
//      RpcResult<CompositeNode> result = router.getInstance().invokeRpc(QNAME, consumer.getValidCompositeNodeWithOneSimpleChild());
//      _logger.debug("{}-> Result is: Successful:[{}], Payload:[{}], Errors: [{}]", i, result.isSuccessful(), result.getResult(), result.getErrors());
//      Assert.assertNotNull(result);
//    }
//  }

  private Message send(Message msg) throws IOException {
    ZMQ.Socket reqSocket = zmqCtx.socket(ZMQ.REQ);
    reqSocket.connect("tcp://localhost:5555");
    reqSocket.send(Message.serialize(msg));
    Message response = parseMessage(reqSocket);

    return response;
  }

  /**
   * @param socket
   * @return
   */
  private Message parseMessage(ZMQ.Socket socket) {

    Message msg = null;
    try {
      byte[] bytes = socket.recv();
      _logger.debug("Received bytes:[{}]", bytes.length);
      msg = (Message) Message.deserialize(bytes);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return msg;
  }

  
  private void printState(){
    Bundle[] b = ctx.getBundles();
    _logger.debug("\n\nNumber of bundles [{}]\n\n]", b.length);
    for (int i=0;i<b.length;i++){
      _logger.debug("Bundle States {}-{} ",b[i].getSymbolicName(), stateToString(b[i].getState()));

      if ( Bundle.INSTALLED == b[i].getState() || (Bundle.RESOLVED == b[i].getState())){
        try {
          b[i].start();
        } catch (BundleException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private String stateToString(int state) {
    switch (state) {
      case Bundle.ACTIVE:
        return "ACTIVE";
      case Bundle.INSTALLED:
        return "INSTALLED";
      case Bundle.RESOLVED:
        return "RESOLVED";
      case Bundle.UNINSTALLED:
        return "UNINSTALLED";
      default:
        return "Not CONVERTED";
    }
  }

  @Configuration
  public Option[] config() {
    return options(systemProperty("osgi.console").value("2401"),
            systemProperty("rpc.port").value("5555"),
            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), //
            mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(), //
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(), //
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(), //

            //mavenBundle(ODL, "sal-binding-broker-impl").versionAsInProject().update(), //
            mavenBundle(ODL, "sal-common").versionAsInProject(), //
            mavenBundle(ODL, "sal-common-api").versionAsInProject(),//
            mavenBundle(ODL, "sal-common-impl").versionAsInProject(), //
            mavenBundle(ODL, "sal-common-util").versionAsInProject(), //
            mavenBundle(ODL, "sal-core-api").versionAsInProject().update(), //
            mavenBundle(ODL, "sal-broker-impl").versionAsInProject(), //
            mavenBundle(ODL, "sal-core-spi").versionAsInProject().update(), //
            mavenBundle(ODL, "sal-connector-api").versionAsInProject(), //


            baseModelBundles(),
            bindingAwareSalBundles(),
            TestHelper.bindingIndependentSalBundles(),
            TestHelper.configMinumumBundles(),
            TestHelper.mdSalCoreBundles(),

            //Added the consumer
            mavenBundle(SAMPLE, "sal-remoterpc-connector-test-consumer").versionAsInProject(), //
            //**** These two bundles below are NOT successfully resolved -- some of their dependencies must be missing
            //**** This causes the "Message" error to occur, the class cannot be found
            mavenBundle(SAMPLE, "sal-remoterpc-connector-test-provider").versionAsInProject(), //
            mavenBundle(ODL, "sal-remoterpc-connector").versionAsInProject(), //

            mavenBundle(ODL, "zeromq-routingtable.implementation").versionAsInProject(),
            mavenBundle(YANG, "concepts").versionAsInProject(),
            mavenBundle(YANG, "yang-binding").versionAsInProject(), //
            mavenBundle(YANG, "yang-common").versionAsInProject(), //
            mavenBundle(YANG, "yang-data-api").versionAsInProject(), //
            mavenBundle(YANG, "yang-data-impl").versionAsInProject(), //
            mavenBundle(YANG, "yang-model-api").versionAsInProject(), //
            mavenBundle(YANG, "yang-parser-api").versionAsInProject(), //
            mavenBundle(YANG, "yang-parser-impl").versionAsInProject(), //
            mavenBundle(YANG, "yang-model-util").versionAsInProject(), //
            mavenBundle(YANG + ".thirdparty", "xtend-lib-osgi").versionAsInProject(), //
            mavenBundle(YANG + ".thirdparty", "antlr4-runtime-osgi-nohead").versionAsInProject(), //
            mavenBundle("com.google.guava", "guava").versionAsInProject(), //
            mavenBundle("org.zeromq", "jeromq").versionAsInProject(),
            mavenBundle("org.codehaus.jackson", "jackson-mapper-asl").versionAsInProject(),
            mavenBundle("org.codehaus.jackson", "jackson-core-asl").versionAsInProject(),
            //routingtable dependencies
            systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
            // List framework bundles
            mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
            mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
            mavenBundle("equinoxSDK381", "org.eclipse.osgi.services").versionAsInProject(),
            mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
            mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
            mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
            mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
            // List logger bundles

            mavenBundle("org.opendaylight.controller", "clustering.services")
                    .versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "clustering.stub")
                    .versionAsInProject(),


            // List all the bundles on which the test case depends
            mavenBundle("org.opendaylight.controller", "sal")
                    .versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "sal.implementation")
                    .versionAsInProject(),
            mavenBundle("org.jboss.spec.javax.transaction",
                    "jboss-transaction-api_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.commons", "commons-lang3")
                    .versionAsInProject(),
            mavenBundle("org.apache.felix",
                    "org.apache.felix.dependencymanager")
                    .versionAsInProject(),

            junitBundles()
    );
  }

}
