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
 * Unit tests for AbortSlicing.
 *
 * @author Thomas Pantelis
 */
public class AbortSlicingTest {

    @Test
    public void testSerialization() {
        AbortSlicing expected = new AbortSlicing(new StringIdentifier("test"));
        AbortSlicing cloned = (AbortSlicing) SerializationUtils.clone(expected);
        assertEquals("getIdentifier", expected.getIdentifier(), cloned.getIdentifier());
    }
}
