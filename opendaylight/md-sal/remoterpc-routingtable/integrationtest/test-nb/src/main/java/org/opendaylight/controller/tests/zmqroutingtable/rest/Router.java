/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.tests.zmqroutingtable.rest;

import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.impl.RoutingTableImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.net.URI;

@Path("router")
public class Router implements Serializable {
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
    @Path("/rtadd")
    @Produces(MediaType.TEXT_PLAIN)
    public String addToRoutingTable(@QueryParam("nsp") String namespace,@QueryParam("inst") String instance,@QueryParam("port") String port) {
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


        RouteIdentifierImpl rii = new RouteIdentifierImpl(namespace,instance);
        try {
            routingTable.addGlobalRoute(rii, instance+":"+ port);
        } catch (RoutingTableException e) {
            _logger.error("error in adding routing identifier" + e.getMessage());

        } catch (SystemException e) {
            _logger.error("error in adding routing identifier" + e.getMessage());
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Result of adding route:").append("\n")
                     .append(routingTable.dumpRoutingTableCache());
        return stringBuilder.toString();
    }

  @GET
  @Path("/rtdelete")
  @Produces(MediaType.TEXT_PLAIN)
  public String invokeDeleteRoutingTable(@QueryParam("nsp") String namespace,@QueryParam("inst") String instance) {
    _logger.info("Invoking delete an entry in routing table");

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


    RouteIdentifierImpl rii = new RouteIdentifierImpl(namespace,instance);
    try {
      routingTable.removeGlobalRoute(rii);
    } catch (RoutingTableException e) {
      _logger.error("error in adding routing identifier" + e.getMessage());

    } catch (SystemException e) {
      _logger.error("error in adding routing identifier" + e.getMessage());
    }


    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Result of deleting route:").append("\n")
              .append(routingTable.dumpRoutingTableCache());

    return stringBuilder.toString();
  }

    @GET
    @Path("/routingtable")
    @Produces(MediaType.TEXT_PLAIN)
    public String invokeGetRoutingTable() {
        _logger.info("Invoking getting of routing table");

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


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Result of getting routetable:").append("\n")
                .append(routingTable.dumpRoutingTableCache());

        return stringBuilder.toString();
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



}
