/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.clustering.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;

/**
 * Unit tests for PreBoronEntityOwnershipServiceAdapter.
 *
 * @author Thomas Pantelis
 */
public class LegacyEntityOwnershipServiceAdapterTest {
    static Entity LEGACY_ENTITY = new Entity("foo", "bar");
    static DOMEntity DOM_ENTITY = new DOMEntity("foo", LEGACY_ENTITY.getId());

    @Mock
    private DOMEntityOwnershipService mockDOMService;

    private LegacyEntityOwnershipServiceAdapter adapter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        adapter = new LegacyEntityOwnershipServiceAdapter(mockDOMService);
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        DOMEntityOwnershipCandidateRegistration mockDOMReg = mock(DOMEntityOwnershipCandidateRegistration.class);
        doNothing().when(mockDOMReg).close();
        doReturn(mockDOMReg).when(mockDOMService).registerCandidate(DOM_ENTITY);

        EntityOwnershipCandidateRegistration reg = adapter.registerCandidate(LEGACY_ENTITY);

        assertNotNull("registerCandidate returned null", reg);
        assertEquals("getInstance", LEGACY_ENTITY, reg.getInstance());

        reg.close();
        verify(mockDOMReg).close();
    }

    @Test(expected=CandidateAlreadyRegisteredException.class)
    public void testAlreadyRegisteredCandidate() throws Exception {
        doThrow(new org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException(DOM_ENTITY)).
                when(mockDOMService).registerCandidate(DOM_ENTITY);

        adapter.registerCandidate(LEGACY_ENTITY);
    }

    @Test
    public void testRegisterListener() {
        DOMEntityOwnershipListenerRegistration mockDOMReg = mock(DOMEntityOwnershipListenerRegistration.class);
        doNothing().when(mockDOMReg).close();
        doReturn(mockDOMReg).when(mockDOMService).registerListener(eq(DOM_ENTITY.getType()),
                any(DOMEntityOwnershipListener.class));
        EntityOwnershipListener mockListener = mock(EntityOwnershipListener.class);
        doNothing().when(mockListener).ownershipChanged(any(EntityOwnershipChange.class));

        EntityOwnershipListenerRegistration reg = adapter.registerListener(LEGACY_ENTITY.getType(), mockListener);

        assertNotNull("registerListener returned null", reg);
        assertEquals("getInstance", mockListener, reg.getInstance());
        assertEquals("getEntityType", LEGACY_ENTITY.getType(), reg.getEntityType());

        ArgumentCaptor<DOMEntityOwnershipListener> domListenerCaptor = ArgumentCaptor.forClass(DOMEntityOwnershipListener.class);
        verify(mockDOMService).registerListener(eq(DOM_ENTITY.getType()),  domListenerCaptor.capture());

        DOMEntityOwnershipChange domOwnershipChange = new DOMEntityOwnershipChange(DOM_ENTITY,
                EntityOwnershipChangeState.LOCAL_OWNERSHIP_GRANTED);
        domListenerCaptor.getValue().ownershipChanged(domOwnershipChange );

        ArgumentCaptor<EntityOwnershipChange> ownershipChangeCaptor = ArgumentCaptor.forClass(EntityOwnershipChange.class);
        verify(mockListener).ownershipChanged(ownershipChangeCaptor.capture());

        EntityOwnershipChange change = ownershipChangeCaptor.getValue();
        assertEquals("getEntity", LEGACY_ENTITY, change.getEntity());
        assertEquals("wasOwner", false, change.wasOwner());
        assertEquals("isOwner", true, change.isOwner());
        assertEquals("hasOwner", true, change.hasOwner());

        reg.close();
        verify(mockDOMReg).close();
    }

    @Test
    public void testGetOwnershipState() {
        testGetOwnershipState(EntityOwnershipState.IS_OWNER, true, true);
        testGetOwnershipState(EntityOwnershipState.OWNED_BY_OTHER, false, true);
        testGetOwnershipState(EntityOwnershipState.NO_OWNER, false, false);

        doReturn(Optional.absent()).when(mockDOMService).getOwnershipState(DOM_ENTITY);
        assertEquals("isPresent", false, adapter.getOwnershipState(LEGACY_ENTITY).isPresent());
    }

    @Test
    public void testIsCandidateRegistered() {
        doReturn(true).when(mockDOMService).isCandidateRegistered(DOM_ENTITY);
        assertEquals("isCandidateRegistered", true, adapter.isCandidateRegistered(LEGACY_ENTITY));
    }

    private void testGetOwnershipState(EntityOwnershipState state, boolean expIsOwner, boolean expHasOwner) {
        doReturn(Optional.of(state)).when(mockDOMService).getOwnershipState(DOM_ENTITY);

        Optional<org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState> actualState =
                adapter.getOwnershipState(LEGACY_ENTITY);

        assertEquals("isPresent", true, actualState.isPresent());
        assertEquals("isOwner", expIsOwner, actualState.get().isOwner());
        assertEquals("hasOwner", expHasOwner, actualState.get().hasOwner());

    }

}
