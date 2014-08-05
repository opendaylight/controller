/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages.datachange.notification;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the DataChangeListener.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */

public class DataChangeListenerMessagesTest extends AbstractMessagesTest {

  private final String namespace = "urn:protobuff", revision = "2014-07-31",
      localName = "test";

  @Override
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

    NormalizedNodeMessages.InstanceIdentifier expectedOne =
        instanceIdentifierBuilder.build();
    DataChangeListenerMessages.DataChanged.Builder builder =
        DataChangeListenerMessages.DataChanged.newBuilder();
    builder.addRemovedPaths(expectedOne);

    writeToFile((com.google.protobuf.GeneratedMessage.Builder<?>) builder);

    DataChangeListenerMessages.DataChanged dataChangedNew =
        (DataChangeListenerMessages.DataChanged) readFromFile(DataChangeListenerMessages.DataChanged.PARSER);
    Assert.assertEquals(expectedOne.getArgumentsCount(), dataChangedNew
        .getRemovedPaths(0).getArgumentsCount());
    Assert.assertEquals(expectedOne.getArguments(0).getType(), dataChangedNew
        .getRemovedPaths(0).getArguments(0).getType());

    DataChangeListenerMessages.DataChanged dataChangedOriginal =
        (DataChangeListenerMessages.DataChanged) readFromTestDataFile(DataChangeListenerMessages.DataChanged.PARSER);
    Assert.assertEquals(dataChangedNew.getRemovedPathsCount(),
        dataChangedOriginal.getRemovedPathsCount());
    Assert.assertEquals(dataChangedNew.getRemovedPaths(0).getArguments(0)
        .getValue(), dataChangedOriginal.getRemovedPaths(0).getArguments(0)
        .getValue());

  }

  @Override
  public String getTestFileName() {
    return DataChangeListenerMessages.class.getSimpleName();
  }
}
