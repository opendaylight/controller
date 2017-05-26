/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;

public class QNameFactoryTest {

    @Test
    public void testBasic() {
        QName expected = TestModel.AUG_NAME_QNAME;
        QName created = QNameFactory.create(expected.toString());

        assertFalse(expected == created);

        assertEquals(expected, created);

        QName cached = QNameFactory.create(expected.toString());

        assertEquals(expected, cached);

        assertTrue(cached == created);
    }
}
