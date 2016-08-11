/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for ApplyJournalEntries.
 *
 * @author Thomas Pantelis
 */
public class ApplyJournalEntriesTest {

    @Test
    public void testSerialization() {
        ApplyJournalEntries expected = new ApplyJournalEntries(5);
        ApplyJournalEntries cloned = (ApplyJournalEntries) SerializationUtils.clone(expected);

        assertEquals("getFromIndex", expected.getToIndex(), cloned.getToIndex());
        assertEquals("isMigrated", false, cloned.isMigrated());
    }
}
