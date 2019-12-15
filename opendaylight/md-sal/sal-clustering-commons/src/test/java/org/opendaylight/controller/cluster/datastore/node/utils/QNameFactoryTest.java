/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;

@Deprecated(forRemoval = true)
public class QNameFactoryTest {

    @Test
    @Deprecated
    public void testBasicString() {
        QName expected = TestModel.AUG_NAME_QNAME;
        QName created = QNameFactory.create(expected.toString());
        assertNotSame(expected, created);
        assertEquals(expected, created);

        QName cached = QNameFactory.create(expected.toString());
        assertSame(created, cached);
    }

    @Test
    public void testBasic() {
        QName expected = TestModel.AUG_NAME_QNAME;
        QName created = lookup(expected);
        assertNotSame(expected, created);
        assertEquals(expected, created);

        QName cached = lookup(expected);
        assertSame(created, cached);
    }

    private static QName lookup(final QName qname) {
        return QNameFactory.create(qname.getLocalName(), qname.getNamespace().toString(),
            qname.getRevision().map(Revision::toString).orElse(null));
    }
}
