package org.opendaylight.controller.cluster.datastore.node.utils;

import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

public class QNameFactoryTest {

    @Test
    public void testBasic(){
        QName expected = TestModel.AUG_NAME_QNAME;
        QName created = QNameFactory.create(expected.toString());

        assertFalse( expected == created);

        assertEquals(expected, created);

        QName cached = QNameFactory.create(expected.toString());

        assertEquals(expected, cached);

        assertTrue( cached == created );
    }

}
