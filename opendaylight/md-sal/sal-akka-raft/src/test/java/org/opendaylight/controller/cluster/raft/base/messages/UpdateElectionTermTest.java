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
 * Unit tests for UpdateElectionTerm.
 *
 * @author Thomas Pantelis
 */
public class UpdateElectionTermTest {

    @Test
    public void testSerialization() {

        UpdateElectionTerm deleteEntries = new UpdateElectionTerm(5, "member1");

        UpdateElectionTerm clone = (UpdateElectionTerm) SerializationUtils.clone(deleteEntries);

        Assert.assertEquals("getCurrentTerm", 5, clone.getCurrentTerm());
        Assert.assertEquals("getVotedFor", "member1", clone.getVotedFor());
    }
}
