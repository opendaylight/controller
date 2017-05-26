/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ModuleIdentifierTest {
    String fact = new String("factory");
    String inst = new String("instance");

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor() throws Exception {
        ModuleIdentifier m = new ModuleIdentifier(null, "instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2() throws Exception {
        ModuleIdentifier m = new ModuleIdentifier("name", null);
    }

    @Test
    public void testEquals() throws Exception {

        ModuleIdentifier m1 = new ModuleIdentifier(fact, inst);
        assertEquals(m1, new ModuleIdentifier(fact, inst));
    }

    @Test
    public void testEquals2() throws Exception {
        assertNotEquals(new ModuleIdentifier(fact, inst), null);
    }

    @Test
    public void testEquals3() throws Exception {
        assertNotEquals(new ModuleIdentifier(fact, inst), new ModuleIdentifier(fact, "i"));
    }

    @Test
    public void testEquals4() throws Exception {
        assertNotEquals(new ModuleIdentifier(fact, inst), new ModuleIdentifier("f", inst));
    }

    @Test
    public void testEquals5() throws Exception {
        ModuleIdentifier m1 = new ModuleIdentifier(fact, inst);
        assertEquals(m1, m1);
    }

    @Test
    public void testHashCode() throws Exception {
        int hash = new ModuleIdentifier(fact, inst).hashCode();
        assertEquals(hash, new ModuleIdentifier("factory", "instance").hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals( new ModuleIdentifier("factory", "instance").toString(),
                new ModuleIdentifier("factory", "instance").toString());
    }
}
