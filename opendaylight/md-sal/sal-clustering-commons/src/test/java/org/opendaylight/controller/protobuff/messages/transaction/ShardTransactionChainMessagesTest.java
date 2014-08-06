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
 * for the ShardTransactionChain.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */


public class ShardTransactionChainMessagesTest extends AbstractMessagesTest {

  @Override
  @Test
  public void verifySerialization() throws Exception {
    String testTransactionChainPath =
        "/actor/path";

    ShardTransactionChainMessages.CreateTransactionChainReply.Builder builder =
        ShardTransactionChainMessages.CreateTransactionChainReply.newBuilder();
    builder.setTransactionChainPath(testTransactionChainPath);

    writeToFile((com.google.protobuf.GeneratedMessage.Builder<?>) builder);

    // Here we will read the same and check we got back what we had saved
    ShardTransactionChainMessages.CreateTransactionChainReply replyNew =
        (ShardTransactionChainMessages.CreateTransactionChainReply) readFromFile(ShardTransactionChainMessages.CreateTransactionChainReply.PARSER);

    Assert.assertEquals(replyNew.getTransactionChainPath(),testTransactionChainPath);

    // the following will compare with the version we had shipped
    ShardTransactionChainMessages.CreateTransactionChainReply replyOriginal =
        (ShardTransactionChainMessages.CreateTransactionChainReply) readFromTestDataFile(ShardTransactionChainMessages.CreateTransactionChainReply.PARSER);


    Assert.assertEquals(replyOriginal.getTransactionChainPath(),
        replyNew.getTransactionChainPath());

  }

  @Override
  public String getTestFileName() {
    return ShardTransactionChainMessagesTest.class.getSimpleName();
  }



}
