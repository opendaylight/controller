/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.tests.zmqrouter.rest;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.impl.RoutingTableImpl;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.controller.sample.zeromq.consumer.ExampleConsumer;
import org.opendaylight.controller.sample.zeromq.provider.ExampleProvider;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.net.URI;
import java.util.Set;

@Path("router")
public class Router {
  private Logger _logger = LoggerFactory.getLogger(Router.class);
  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "heartbeat");


  @GET
  @Path("/hello")
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    return "Hello";
  }

  @GET
  @Path("/announce")
  @Produces(MediaType.TEXT_PLAIN)
  public String announce() {
    _logger.info("Announce request received");

    BundleContext ctx = getBundleContext();
    ServiceReference providerRef = ctx.getServiceReference(ExampleProvider.class);
    if (providerRef == null) {
      _logger.debug("Could not get provider reference");
      return "Could not get provider reference";
    }

    ExampleProvider provider = (ExampleProvider) ctx.getService(providerRef);
    if (provider == null) {
      _logger.info("Could not get provider service");
      return "Could not get provider service";
    }

    provider.announce(QNAME);
    return "Announcement sent ";

  }

  @GET
  @Path("/rpc")
  @Produces(MediaType.TEXT_PLAIN)
  public String invokeRpc() throws Exception {
    _logger.info("Invoking RPC");

    ExampleConsumer consumer = getConsumer();
    RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, consumer.getValidCompositeNodeWithOneSimpleChild());
    _logger.info("Result [{}]", result.isSuccessful());

    return stringify(result);
  }

  @GET
  @Path("/rpc-success")
  @Produces(MediaType.TEXT_PLAIN)
  public String invokeRpcSuccess() throws Exception {
    ExampleConsumer consumer = getConsumer();
    RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, consumer.getValidCompositeNodeWithFourSimpleChildren()); //TODO: Change this
    _logger.info("Result [{}]", result.isSuccessful());

    return stringify(result);
  }

  @GET
  @Path("/rpc-failure")
  @Produces(MediaType.TEXT_PLAIN)
  public String invokeRpcFailure() throws Exception {
    ExampleConsumer consumer = getConsumer();
    //RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, consumer.getInvalidCompositeNodeCompositeChild()); //TODO: Change this
    RpcResult<CompositeNode> result = consumer.invokeRpc(QNAME, null); //TODO: Change this
    _logger.info("Result [{}]", result.isSuccessful());

    return stringify(result);
  }

  @GET
  @Path("/routingtable")
  @Produces(MediaType.TEXT_PLAIN)
  public String invokeRoutingTable() {
    _logger.info("Invoking adding an entry in routing table");

    BundleContext ctx = getBundleContext();
    ServiceReference routingTableServiceReference = ctx.getServiceReference(RoutingTable.class);
    if (routingTableServiceReference == null) {
      _logger.debug("Could not get routing table impl reference");
      return "Could not get routingtable referen ";
    }
    RoutingTableImpl routingTable = (RoutingTableImpl) ctx.getService(routingTableServiceReference);
    if (routingTable == null) {
      _logger.info("Could not get routing table service");
      return "Could not get routing table service";
    }


    RoutingIdentifierImpl rii = new RoutingIdentifierImpl();
    try {
      routingTable.addGlobalRoute(rii.toString(), "172.27.12.1:5000");
    } catch (RoutingTableException e) {
      _logger.error("error in adding routing identifier" + e.getMessage());

    } catch (SystemException e) {
      _logger.error("error in adding routing identifier" + e.getMessage());
    }

    String result = routingTable.dumpRoutingTableCache();




    _logger.info("Result [{}] routes added for route" + rii + result);

    return result;
  }

  @GET
  @Path("/routingtabledelete")
  @Produces(MediaType.TEXT_PLAIN)
  public String invokeDeleteRoutingTable() {
    _logger.info("Invoking adding an entry in routing table");

    BundleContext ctx = getBundleContext();
    ServiceReference routingTableServiceReference = ctx.getServiceReference(RoutingTable.class);
    if (routingTableServiceReference == null) {
      _logger.debug("Could not get routing table impl reference");
      return "Could not get routingtable referen ";
    }
    RoutingTable routingTable = (RoutingTableImpl) ctx.getService(routingTableServiceReference);
    if (routingTable == null) {
      _logger.info("Could not get routing table service");
      return "Could not get routing table service";
    }


    RoutingIdentifierImpl rii = new RoutingIdentifierImpl();
    try {
      routingTable.removeGlobalRoute(rii.toString());
    } catch (RoutingTableException e) {
      _logger.error("error in adding routing identifier" + e.getMessage());

    } catch (SystemException e) {
      _logger.error("error in adding routing identifier" + e.getMessage());
    }

    Set<String> routes = routingTable.getRoutes(rii.toString());

    StringBuilder stringBuilder = new StringBuilder();
    if (routes != null) {
      for (String route : routes) {
        stringBuilder.append(route);
      }
    } else {
      stringBuilder.append(" successfully");
    }

    _logger.info("Result [{}] routes removed for route" + rii + stringBuilder.toString());

    return stringBuilder.toString();
  }

  private String stringify(RpcResult<CompositeNode> result) {
    CompositeNode node = result.getResult();
    StringBuilder builder = new StringBuilder("result:").append(XmlUtils.compositeNodeToXml(node)).append("\n")
        .append("error:").append(result.getErrors()).append("\n");

    return builder.toString();
  }

  private BundleContext getBundleContext() {
    ClassLoader tlcl = Thread.currentThread().getContextClassLoader();
    Bundle bundle = null;

    if (tlcl instanceof BundleReference) {
      bundle = ((BundleReference) tlcl).getBundle();
    } else {
      _logger.info("Unable to determine the bundle context based on " +
          "thread context classloader.");
      bundle = FrameworkUtil.getBundle(this.getClass());
    }
    return (bundle == null ? null : bundle.getBundleContext());
  }

  private ExampleConsumer getConsumer() {
    BundleContext ctx = getBundleContext();
    ServiceReference consumerRef = ctx.getServiceReference(ExampleConsumer.class);
    if (consumerRef == null) {
      _logger.debug("Could not get consumer reference");
      throw new NullPointerException("Could not get consumer reference");
    }
    ExampleConsumer consumer = (ExampleConsumer) ctx.getService(consumerRef);
    if (consumer == null) {
      _logger.info("Could not get consumer service");
      throw new NullPointerException("Could not get consumer service");
    }
    return consumer;
  }

  class RoutingIdentifierImpl implements RpcRouter.RouteIdentifier, Serializable {

    private final URI namespace = URI.create("http://cisco.com/example");
    private final QName QNAME = new QName(namespace, "global");
    private final QName instance = new QName(URI.create("127.0.0.1"), "local");

    @Override
    public QName getContext() {
      return QNAME;
    }

    @Override
    public QName getType() {
      return QNAME;
    }

    @Override
    public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier getRoute() {
      return InstanceIdentifier.of(instance);
    }

      @Override
      public boolean equals(Object o) {
          if (this == o) return true;
          if (o == null || getClass() != o.getClass()) return false;

          RoutingIdentifierImpl that = (RoutingIdentifierImpl) o;

          if (!QNAME.equals(that.QNAME)) return false;
          if (!instance.equals(that.instance)) return false;
          if (!namespace.equals(that.namespace)) return false;

          return true;
      }

      @Override
      public int hashCode() {
          int result = namespace.hashCode();
          result = 31 * result + QNAME.hashCode();
          result = 31 * result + instance.hashCode();
          return result;
      }
  }
}
