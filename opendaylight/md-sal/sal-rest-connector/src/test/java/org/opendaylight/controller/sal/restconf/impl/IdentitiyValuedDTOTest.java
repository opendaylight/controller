/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.Predicate;

public class IdentitiyValuedDTOTest {

    /**
     * tests toString method for IdentityValue and Predicate classes
     */
    @Test
    public void testToStringMethod() {
        IdentityValue identityValue = new IdentityValue("dummy:namespace", "dummy value", "dummyprefix");
        IdentityValue identityValueListEntry1 = new IdentityValue("dummy:list:entry1", "dummy list entry1",
                "dummyprefix");
        IdentityValue identityValueListEntry2 = new IdentityValue("dummy:list:entry2", "dummy list entry2",
                "dummyprefix");
        List<Predicate> predicates = new ArrayList<Predicate>();

        predicates.add(new Predicate(identityValueListEntry1, "entry1"));
        predicates.add(new Predicate(identityValueListEntry2, "entry2"));
        identityValue.setPredicates(predicates);

        assertEquals(
                "dummy:namespace(dummyprefix) - dummy value[dummy:list:entry1(dummyprefix) - dummy list entry1=entry1][dummy:list:entry2(dummyprefix) - dummy list entry2=entry2]",
                identityValue.toString());
    }

}
