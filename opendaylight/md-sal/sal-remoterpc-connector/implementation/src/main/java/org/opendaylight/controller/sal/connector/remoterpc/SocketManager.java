/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Manages creation of {@link RpcSocket} and their registration with {@link ZMQ.Poller}
 */
public class SocketManager implements AutoCloseable{
  private static final Logger log = LoggerFactory.getLogger(SocketManager.class);

  /*
   * RpcSockets mapped by network address its connected to
   */
  private ConcurrentHashMap<String, RpcSocket> managedSockets = new ConcurrentHashMap<String, RpcSocket>();

  private ZMQ.Poller _poller = new ZMQ.Poller(2); //randomly selected size. Poller grows automatically

  /**
   * Returns a {@link RpcSocket} for the given address
   * @param address network address with port eg: 10.199.199.20:5554
   * @return
   */
  public RpcSocket getManagedSocket(String address) throws IllegalArgumentException {
    //Precondition
    if (!address.matches("(tcp://)(.*)(:)(\\d*)")) {
      throw new IllegalArgumentException("Address must of format 'tcp://<ip address>:<port>' but is " + address);
    }

    if (!managedSockets.containsKey(address)) {
      log.debug("{} Creating new socket for {}", Thread.currentThread().getName());
      RpcSocket socket = new RpcSocket(address, _poller);
      managedSockets.put(address, socket);
    }

    return managedSockets.get(address);
  }

  /**
   * Returns a {@link RpcSocket} for the given {@link ZMQ.Socket}
   * @param socket
   * @return
   */
  public Optional<RpcSocket> getManagedSocketFor(ZMQ.Socket socket) {
    for (RpcSocket rpcSocket : managedSockets.values()) {
      if (rpcSocket.getSocket().equals(socket)) {
        return Optional.of(rpcSocket);
      }
    }
    return Optional.absent();
  }

  /**
   * Return a collection of all managed sockets
   * @return
   */
  public Collection<RpcSocket> getManagedSockets() {
    return managedSockets.values();
  }

  /**
   * Returns the {@link ZMQ.Poller}
   * @return
   */
  public ZMQ.Poller getPoller() {
    return _poller;
  }

  /**
   * This should be called when stopping the server to close all the sockets
   * @return
   */
  @Override
  public void close() throws Exception {
    log.debug("Stopping...");
    for (RpcSocket socket : managedSockets.values()) {
      socket.close();
    }
    managedSockets.clear();
    log.debug("Stopped");
  }
}
