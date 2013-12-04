/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.MessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the busy state of a {@link RpcSocket}
 */
public class BusySocketState implements SocketState {

  private static Logger log = LoggerFactory.getLogger(BusySocketState.class);

  @Override
  public void process(RpcSocket socket) {
    if (socket.hasTimedOut()) {
      if (socket.getRetriesLeft() > 0) {
        log.debug("process : Request timed out, retrying now...");
        socket.sendMessage();
        socket.setRetriesLeft(socket.getRetriesLeft() - 1);
      }
      else {
        // No more retries for current request, so stop processing the current request
        MessageWrapper message = socket.removeCurrentRequest();
        if (message != null) {
          log.error("Unable to process rpc request [{}]", message);
          socket.setState(new IdleSocketState());
          socket.setRetriesLeft(RpcSocket.NUM_RETRIES);
        }
      }
    }
    // Else no timeout, so allow processing to continue
  }
}
