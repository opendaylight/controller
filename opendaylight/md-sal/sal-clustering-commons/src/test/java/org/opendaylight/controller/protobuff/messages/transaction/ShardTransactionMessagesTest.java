/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages.transaction;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the ShardTransaction.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */


public class ShardTransactionMessagesTest extends AbstractMessagesTest {

  private final String namespace = "urn:protobuff", revision = "2014-07-31",
      localName = "test";

  @Test
  public void verifySerialization() throws Exception {
    NormalizedNodeMessages.InstanceIdentifier.Builder instanceIdentifierBuilder =
        NormalizedNodeMessages.InstanceIdentifier.newBuilder();
    NormalizedNodeMessages.PathArgument.Builder pathArgument =
        NormalizedNodeMessages.PathArgument.newBuilder();
    pathArgument.setNodeType(NormalizedNodeMessages.QName.newBuilder()
        .setValue(QName.create(namespace, revision, localName).toString())
        .build());
    pathArgument.setValue("test");
    instanceIdentifierBuilder.addArguments(pathArgument.build());
    ShardTransactionMessages.ReadData.Builder builder =
        ShardTransactionMessages.ReadData.newBuilder();
    NormalizedNodeMessages.InstanceIdentifier expectedOne =
        instanceIdentifierBuilder.build();
    builder.setInstanceIdentifierPathArguments(expectedOne);

    writeToFile(builder);

    // Here we will read the same and check we got back what we had saved
    ShardTransactionMessages.ReadData readDataNew =
        (ShardTransactionMessages.ReadData) readFromFile(ShardTransactionMessages.ReadData.PARSER);


    Assert.assertEquals(expectedOne.getArgumentsCount(), readDataNew
        .getInstanceIdentifierPathArguments().getArgumentsCount());
    Assert.assertEquals(expectedOne.getArguments(0), readDataNew
        .getInstanceIdentifierPathArguments().getArguments(0));


    // the following will compare with the version we had shipped
    ShardTransactionMessages.ReadData readDataOriginal =
        (ShardTransactionMessages.ReadData) readFromTestDataFile(ShardTransactionMessages.ReadData.PARSER);


    Assert.assertEquals(readDataNew.getInstanceIdentifierPathArguments()
        .getArguments(0), readDataOriginal.getInstanceIdentifierPathArguments()
        .getArguments(0));

  }

  @Override
  public String getTestFileName() {
    return ShardTransactionMessagesTest.class.getSimpleName();
  }



}
