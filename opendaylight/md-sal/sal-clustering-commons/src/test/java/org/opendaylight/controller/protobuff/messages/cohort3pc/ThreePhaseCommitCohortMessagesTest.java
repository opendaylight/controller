/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages.cohort3pc;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the cohort.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */


public class ThreePhaseCommitCohortMessagesTest extends AbstractMessagesTest {


  @Test
  public void verifySerialization() throws Exception {



    ThreePhaseCommitCohortMessages.CanCommitTransactionReply.Builder builder =
        ThreePhaseCommitCohortMessages.CanCommitTransactionReply.newBuilder();
    builder.setCanCommit(true);

    writeToFile(builder);

    // Here we will read from the just serialized data

    ThreePhaseCommitCohortMessages.CanCommitTransactionReply newCanCommitTransactionReply =
        (ThreePhaseCommitCohortMessages.CanCommitTransactionReply) readFromFile(ThreePhaseCommitCohortMessages.CanCommitTransactionReply.PARSER);

    Assert.assertTrue(newCanCommitTransactionReply.getCanCommit());


    // Here we will read the same from our test-data file and check we got back what we had saved
    // earlier
    ThreePhaseCommitCohortMessages.CanCommitTransactionReply originalCanCommitTransactionReply =
        (ThreePhaseCommitCohortMessages.CanCommitTransactionReply) readFromTestDataFile(ThreePhaseCommitCohortMessages.CanCommitTransactionReply.PARSER);

    Assert.assertEquals(newCanCommitTransactionReply.getCanCommit(),
        originalCanCommitTransactionReply.getCanCommit());

  }

  @Override
  public String getTestFileName() {
    return ThreePhaseCommitCohortMessagesTest.class.getSimpleName();
  }


}
