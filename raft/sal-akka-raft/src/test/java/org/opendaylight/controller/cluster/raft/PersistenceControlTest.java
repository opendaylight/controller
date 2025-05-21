/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;

/**
 * Unit tests for RaftActorDelegatingPersistentDataProvider.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class PersistenceControlTest {
    @Mock
    private DisabledRaftStorage disabledStorage;
    @Mock
    private EnabledRaftStorage enabledStorage;
    @Mock
    private RaftCallback<Long> callback;

    private PersistenceControl provider;

    @BeforeEach
    void beforeEach() {
        provider = new PersistenceControl(disabledStorage, enabledStorage);
        // we should start off as DisabledRaftStorage
        assertSame(disabledStorage, provider.entryStore());
        assertSame(disabledStorage, provider.snapshotStore());
    }

    @Test
    void becomePersistentMeansEnabledStorage() {
        assertTrue(provider.becomePersistent());
        // we should become EnabledRaftStorage
        assertSame(enabledStorage, provider.entryStore());
        assertSame(enabledStorage, provider.snapshotStore());
    }
}
