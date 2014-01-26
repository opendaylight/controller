/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import org.zeromq.ZMQ;

import java.util.UUID;

/**
 *
 */
public class SocketPair implements AutoCloseable{
  private ZMQ.Socket sender;
  private ZMQ.Socket receiver;

  private static final String INPROC_PREFIX = "inproc://";

  public SocketPair(){
    String address = new StringBuilder(INPROC_PREFIX)
                         .append(UUID.randomUUID())
                         .toString();

    receiver = Context.getInstance().getZmqContext().socket(ZMQ.PAIR);
    receiver.bind(address);

    sender = Context.getInstance().getZmqContext().socket(ZMQ.PAIR);
    sender.connect(address);
  }

  public ZMQ.Socket getSender(){
    return this.sender;
  }

  public ZMQ.Socket getReceiver(){
    return this.receiver;
  }

  @Override
  public void close() throws Exception {
    sender.close();
    receiver.close();
  }
}
