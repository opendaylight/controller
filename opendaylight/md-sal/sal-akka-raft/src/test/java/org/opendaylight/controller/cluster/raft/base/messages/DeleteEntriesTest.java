/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for DeleteEntries.
 *
 * @author Thomas Pantelis
 */
@Deprecated
public class DeleteEntriesTest {

    @Test
    public void testSerialization() {
        DeleteEntries deleteEntries = new DeleteEntries(11);
        org.opendaylight.controller.cluster.raft.persisted.DeleteEntries clone =
                (org.opendaylight.controller.cluster.raft.persisted.DeleteEntries) SerializationUtils.clone(deleteEntries);

        Assert.assertEquals("getFromIndex", 11, clone.getFromIndex());
        Assert.assertEquals("isMigrated", true, clone.isMigrated());
    }
}
