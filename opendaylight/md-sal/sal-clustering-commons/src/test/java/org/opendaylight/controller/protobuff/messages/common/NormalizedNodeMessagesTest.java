/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages.common;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the common.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */

public class NormalizedNodeMessagesTest extends AbstractMessagesTest {

  @Override
  @Test
  public void verifySerialization() throws Exception {
    NormalizedNodeMessages.Attribute.Builder builder =
        NormalizedNodeMessages.Attribute.newBuilder();
    builder.setName("test");
    builder.setType("fake");
    builder.setValue("testValue");
    writeToFile(builder);

    NormalizedNodeMessages.Attribute attributeNew =
        (NormalizedNodeMessages.Attribute) readFromFile(NormalizedNodeMessages.Attribute.PARSER);
    Assert.assertEquals("test", attributeNew.getName());
    Assert.assertEquals("fake", attributeNew.getType());
    Assert.assertEquals("testValue", attributeNew.getValue());

    NormalizedNodeMessages.Attribute attributeOriginal =
        (NormalizedNodeMessages.Attribute) readFromTestDataFile(NormalizedNodeMessages.Attribute.PARSER);
    Assert.assertEquals(attributeNew.getName(), attributeOriginal.getName());
  }

  @Override
  public String getTestFileName() {
    return NormalizedNodeMessagesTest.class.getSimpleName();
  }


}
