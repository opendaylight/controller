/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages.registration;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the ListenerRegistration.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */

public class ListenerRegistrationMessagesTest extends AbstractMessagesTest {

  @Override
  @Test
  public void verifySerialization() throws Exception {
    String testListenerRegistrationPath =
        "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test?revision=2014-04-15)family";
    ListenerRegistrationMessages.RegisterChangeListenerReply.Builder builder =
        ListenerRegistrationMessages.RegisterChangeListenerReply.newBuilder();
    builder.setListenerRegistrationPath(testListenerRegistrationPath);

    writeToFile((com.google.protobuf.GeneratedMessage.Builder<?>) builder);

    ListenerRegistrationMessages.RegisterChangeListenerReply rclrNew =
        (ListenerRegistrationMessages.RegisterChangeListenerReply) readFromFile(ListenerRegistrationMessages.RegisterChangeListenerReply.PARSER);
    Assert.assertEquals(testListenerRegistrationPath,
        rclrNew.getListenerRegistrationPath());

    ListenerRegistrationMessages.RegisterChangeListenerReply rclrOriginal =
        (ListenerRegistrationMessages.RegisterChangeListenerReply) readFromTestDataFile(ListenerRegistrationMessages.RegisterChangeListenerReply.PARSER);
    Assert.assertEquals(rclrOriginal.getListenerRegistrationPath(),
        rclrNew.getListenerRegistrationPath());

  }

  @Override
  public String getTestFileName() {
    return ListenerRegistrationMessages.class.getSimpleName();
  }
}
