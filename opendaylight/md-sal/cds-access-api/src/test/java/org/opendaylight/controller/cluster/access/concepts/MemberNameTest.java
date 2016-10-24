/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MemberNameTest extends AbstractIdentifierTest<MemberName> {
    private static final MemberName OBJECT = MemberName.forName("test1");
    private static final MemberName DIFFERENT_OBJECT = MemberName.forName("test2");
    private static final MemberName EQUAL_OBJECT = MemberName.forName("test1");

    @Override
    MemberName object() {
        return OBJECT;
    }

    @Override
    MemberName differentObject() {
        return DIFFERENT_OBJECT;
    }

    @Override
    MemberName equalObject() {
        return EQUAL_OBJECT;
    }


    @Test
    public void testCompareTo() {
        assertEquals(0, object().compareTo(object()));
        assertEquals(0, object().compareTo(equalObject()));
        assertTrue(object().compareTo(differentObject()) < 0);
        assertTrue(differentObject().compareTo(object()) > 0);
    }

    @Test
    public void testGetName() {
        assertEquals("test1", OBJECT.getName());
    }
}
