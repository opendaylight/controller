/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.message;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for MessageSliceReply.
 *
 * @author Thomas Pantelis
 */
public class MessageSliceReplyTest {

    @Test
    public void testSerialization() {
        MessageSliceReply expected = new MessageSliceReply(new StringIdentifier("test"), 3, true);
        MessageSliceReply cloned = (MessageSliceReply) SerializationUtils.clone(expected);

        assertEquals("getIdentifier", expected.getIdentifier(), cloned.getIdentifier());
        assertEquals("getSliceIndex", expected.getSliceIndex(), cloned.getSliceIndex());
        assertEquals("isSuccess", expected.isSuccess(), cloned.isSuccess());
    }
}
