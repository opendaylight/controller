/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for FollowerIdentifier.
 *
 * @author Thomas Pantelis
 */
public class FollowerIdentifierTest {

    @Test
    public void testSerialization() throws FileNotFoundException, IOException {
        FollowerIdentifier expected = new FollowerIdentifier("follower1");
        FollowerIdentifier cloned = (FollowerIdentifier) SerializationUtils.clone(expected);
        assertEquals("cloned", expected, cloned);
    }
}
