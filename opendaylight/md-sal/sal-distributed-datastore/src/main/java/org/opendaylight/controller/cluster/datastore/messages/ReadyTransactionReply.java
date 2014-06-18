/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;

public class ReadyTransactionReply {
  private final ActorPath path;

  public ReadyTransactionReply(ActorPath path) {

    this.path = path;
  }

  public ActorPath getPath() {
    return path;
  }
}
