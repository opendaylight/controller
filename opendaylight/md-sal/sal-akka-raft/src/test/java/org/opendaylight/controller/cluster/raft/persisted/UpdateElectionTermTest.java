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
 * Unit tests for UpdateElectionTerm.
 *
 * @author Thomas Pantelis
 */
public class UpdateElectionTermTest {

    @Test
    public void testSerialization() {
        UpdateElectionTerm expected = new UpdateElectionTerm(5, "leader");
        UpdateElectionTerm cloned = (UpdateElectionTerm) SerializationUtils.clone(expected);

        assertEquals("getCurrentTerm", expected.getCurrentTerm(), cloned.getCurrentTerm());
        assertEquals("getVotedFor", expected.getVotedFor(), cloned.getVotedFor());
        assertEquals("isMigrated", false, cloned.isMigrated());
    }
}
