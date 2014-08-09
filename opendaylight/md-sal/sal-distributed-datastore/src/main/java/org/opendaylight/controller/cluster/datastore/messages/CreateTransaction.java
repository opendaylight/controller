/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;


import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;


public class CreateTransaction implements SerializableMessage {
  public static final Class SERIALIZABLE_CLASS = ShardTransactionMessages.CreateTransaction.class;
  private final String transactionId;
  private final int transactionType;

  public CreateTransaction(String transactionId, int transactionType){

    this.transactionId = transactionId;
    this.transactionType = transactionType;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public int getTransactionType() { return transactionType;}

  @Override
  public Object toSerializable() {
    return  ShardTransactionMessages.CreateTransaction.newBuilder().setTransactionId(transactionId).setTransactionType(transactionType).build();
  }

  public static CreateTransaction fromSerializable(Object message){
    ShardTransactionMessages.CreateTransaction createTransaction = (ShardTransactionMessages.CreateTransaction)message;
    return new CreateTransaction(createTransaction.getTransactionId(),createTransaction.getTransactionType() );
  }

}
