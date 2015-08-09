/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.clustering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Unit tests for Entity.
 *
 * @author Thomas Pantelis
 */
public class EntityTest {
    static String ENTITY_TYPE1 = "type1";
    static String ENTITY_TYPE2 = "type2";
    static final QName QNAME1 = QName.create("test", "2015-08-14", "1");
    static final QName QNAME2 = QName.create("test", "2015-08-14", "2");
    static final YangInstanceIdentifier YANGID1 = YangInstanceIdentifier.of(QNAME1);
    static final YangInstanceIdentifier YANGID2 = YangInstanceIdentifier.of(QNAME2);

    @Test
    public void testHashCode() {
        Entity entity1 = new Entity(ENTITY_TYPE1, YANGID1);

        assertEquals("hashCode", entity1.hashCode(), new Entity(ENTITY_TYPE1, YANGID1).hashCode());
        assertNotEquals("hashCode", entity1.hashCode(), new Entity(ENTITY_TYPE2, YANGID2).hashCode());
    }

    @Test
    public void testEquals() {
        Entity entity1 = new Entity(ENTITY_TYPE1, YANGID1);

        assertEquals("Same", true, entity1.equals(entity1));
        assertEquals("Same", true, entity1.equals(new Entity(ENTITY_TYPE1, YANGID1)));
        assertEquals("Different entity type", false, entity1.equals(new Entity(ENTITY_TYPE2, YANGID1)));
        assertEquals("Different yang ID", false, entity1.equals(new Entity(ENTITY_TYPE1, YANGID2)));
        assertEquals("Different Object", false, entity1.equals(new Object()));
        assertEquals("Equals null", false, entity1.equals(null));
    }

    @Test
    public void testSerialization() {
        Entity entity = new Entity(ENTITY_TYPE1, YANGID1);

        Entity clone = SerializationUtils.clone(entity);

        assertEquals("getType", entity.getType(), clone.getType());
        assertEquals("getId", entity.getId(), clone.getId());
    }
}
