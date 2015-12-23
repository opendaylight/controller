/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages.persistent;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;


/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the peristent.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */

public class PersistentMessagesTest extends AbstractMessagesTest {

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

    PersistentMessages.Modification.Builder builder =
        PersistentMessages.Modification.newBuilder();
    builder.setType("test");
    builder.setPath(expectedOne);

    writeToFile(builder);

    PersistentMessages.Modification modificationNew =
        (PersistentMessages.Modification) readFromFile(PersistentMessages.Modification.PARSER);
    Assert.assertEquals("test", modificationNew.getType());
    Assert.assertEquals(expectedOne.getArguments(0).getValue(), modificationNew
        .getPath().getArguments(0).getValue());
    Assert.assertEquals(expectedOne.getArguments(0).getType(), modificationNew
        .getPath().getArguments(0).getType());

    // we will compare with the serialized data that we had shipped
    PersistentMessages.Modification modificationOriginal =
        (PersistentMessages.Modification) readFromTestDataFile(PersistentMessages.Modification.PARSER);
    Assert.assertEquals(modificationOriginal.getPath().getArguments(0)
        .getValue(), modificationNew.getPath().getArguments(0).getValue());
    Assert.assertEquals(modificationOriginal.getPath().getArguments(0)
        .getType(), modificationNew.getPath().getArguments(0).getType());


  }

  @Override
  public String getTestFileName() {
    return PersistentMessagesTest.class.getSimpleName();
  }
}
