/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Util class to access package private members in cds-access-client for test purposes.
 */
public class AccessClientMockFactory {

    public static ClientActorContext createClientActorContextMock(final String persistenceId) throws Exception {
        final ClientActorContext mock = mock(ClientActorContext.class);
        when(mock.persistenceId()).thenReturn(persistenceId);
        return mock;
    }

}
