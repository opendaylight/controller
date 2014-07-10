/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class CanCommitTransactionReply implements SerializableMessage {
  public static Class SERIALIZABLE_CLASS = ThreePhaseCommitCohortMessages.CanCommitTransactionReply.class;
  private final Boolean canCommit;

  public CanCommitTransactionReply(Boolean canCommit) {
    this.canCommit = canCommit;
  }

  public Boolean getCanCommit() {
    return canCommit;
  }

  @Override
  public Object toSerializable() {
    return  ThreePhaseCommitCohortMessages.CanCommitTransactionReply.newBuilder().setCanCommit(canCommit).build();
  }


  public static CanCommitTransactionReply fromSerializable(Object message) {
    return  new CanCommitTransactionReply(((ThreePhaseCommitCohortMessages.CanCommitTransactionReply)message).getCanCommit());
  }
}
