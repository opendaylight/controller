/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.slf4j.Logger;
import org.zeromq.ZMQ;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Provides a ZeroMQ Context object
 */
public class Context {
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Context.class);
  private ZMQ.Context zmqContext = ZMQ.context(1);
  private String uri;

  private static Context _instance = new Context();

  private Context() {}

  public static Context getInstance(){
    return _instance;
  }

  public ZMQ.Context getZmqContext(){
    return this.zmqContext;
  }

  public String getLocalUri(){
    uri = (uri != null) ? uri
            : new StringBuilder("tcp://").append(getIpAddress()).append(":")
              .append(getRpcPort()).toString();

    return uri;
  }

  public String getRpcPort(){
    String rpcPort = (System.getProperty("rpc.port") != null)
        ? System.getProperty("rpc.port")
        : "5554";

    return rpcPort;
  }

  private String getIpAddress(){
    String ipAddress = (System.getProperty("local.ip") != null)
        ? System.getProperty("local.ip")
        : findIpAddress();

    return ipAddress;
  }

  /**
   * Finds IPv4 address of the local VM
   * TODO: This method is non-deterministic. There may be more than one IPv4 address. Cant say which
   * address will be returned. Read IP from a property file or enhance the code to make it deterministic.
   * Should we use IP or hostname?
   *
   * @return
   */
  private String findIpAddress() {
    String hostAddress = null;
    Enumeration e = null;
    try {
      e = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e1) {
        LOG.warn("Unhanded Exception ", e);
    }
    while (e.hasMoreElements()) {

      NetworkInterface n = (NetworkInterface) e.nextElement();

      Enumeration ee = n.getInetAddresses();
      while (ee.hasMoreElements()) {
        InetAddress i = (InetAddress) ee.nextElement();
        if ((i instanceof Inet4Address) && (i.isSiteLocalAddress()))
          hostAddress = i.getHostAddress();
      }
    }
    return hostAddress;

  }
}
