/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for MessageSliceIdentifier.
 *
 * @author Thomas Pantelis
 */
public class MessageSliceIdentifierTest {

    @Test
    public void testSerialization() {
        MessageSliceIdentifier expected = new MessageSliceIdentifier(new StringIdentifier("test"));
        MessageSliceIdentifier cloned = (MessageSliceIdentifier) SerializationUtils.clone(expected);
        assertEquals("cloned", expected, cloned);
    }
}
