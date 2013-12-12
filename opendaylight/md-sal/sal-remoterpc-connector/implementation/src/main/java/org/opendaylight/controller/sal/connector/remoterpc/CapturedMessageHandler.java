/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class CapturedMessageHandler implements Runnable {

  private Logger _logger = LoggerFactory.getLogger(CapturedMessageHandler.class);

  private ZMQ.Socket socket;

  public CapturedMessageHandler(ZMQ.Socket socket){
    this.socket = socket;
  }

  @Override
  public void run(){

    try {
      while (!Thread.currentThread().isInterrupted()){
        String message = socket.recvStr();
        _logger.debug("Captured [{}]", message);
      }
    } catch (Exception e) {
      _logger.error("Exception raised [{}]", e.getMessage());
    }
  }
}
