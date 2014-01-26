/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test.structures;

import static org.junit.Assert.*;

import org.junit.Test;

public class CompareLf {

    @Test
    public void test() {
        Lf lf1 = new Lf("name", "value");
        Lf lf2 = new Lf("name", "value");
        Lf lf3 = new Lf("name1", "value");
        Lf lf4 = new Lf("name", "value1");
        
        assertTrue(lf1.equals(lf2));
        assertFalse(lf1.equals(lf3));
        assertFalse(lf1.equals(lf4));
    }

}
