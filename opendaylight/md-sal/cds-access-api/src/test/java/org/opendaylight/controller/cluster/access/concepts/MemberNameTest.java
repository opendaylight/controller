/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemberNameTest extends AbstractIdentifierTest<MemberName> {
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

    @Override
    int expectedSize() {
        return 87;
    }

    @Test
    void testCompareTo() {
        assertEquals(0, object().compareTo(object()));
        assertEquals(0, object().compareTo(equalObject()));
        assertThat(object().compareTo(differentObject())).isLessThan(0);
        assertThat(differentObject().compareTo(object())).isGreaterThan(0);
    }

    @Test
    void testGetName() {
        assertEquals("test1", OBJECT.getName());
    }
}
