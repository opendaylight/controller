/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertSame;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for EmptyState.
 *
 * @author Thomas Pantelis
 *
 */
public class EmptyStateTest {

    @Test
    public void testSerialization() {
        EmptyState cloned = (EmptyState) SerializationUtils.clone(EmptyState.INSTANCE);
        assertSame("cloned", EmptyState.INSTANCE, cloned);
    }
}
