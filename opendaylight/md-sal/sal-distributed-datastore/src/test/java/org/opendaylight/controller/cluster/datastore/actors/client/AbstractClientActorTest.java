/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import static org.mockito.Mockito.doReturn;
import akka.actor.ActorRef;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.common.actor.TestTicker;

/**
 * Abstract base class for client actors and their components.
 *
 * @author Robert Varga
 */
public abstract class AbstractClientActorTest {
    @Mock
    private ClientActorContext mockActorContext;
    @Mock
    private ClientIdentifier mockIdentifier;
    @Mock
    private ActorRef mockSelf;

    protected final TestTicker ticker = new TestTicker();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        doReturn(ticker).when(mockActorContext).ticker();
        doReturn(mockIdentifier).when(mockActorContext).getIdentifier();
        doReturn(getClass().getSimpleName()).when(mockActorContext).persistenceId();
        doReturn(mockSelf).when(mockActorContext).self();
    }

    protected final ClientActorContext actorContext() {
        return mockActorContext;
    }

    protected final ClientIdentifier identifier() {
        return mockIdentifier;
    }

    protected final ActorRef self() {
        return mockSelf;
    }
}
