/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.akka.eos.service;

import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

public class NativeEntityOwnershipCandidateRegistration extends AbstractObjectRegistration<DOMEntity>
        implements DOMEntityOwnershipCandidateRegistration {
    private final NativeEntityOwnershipService service;

    protected NativeEntityOwnershipCandidateRegistration(final DOMEntity instance,
                                                         final NativeEntityOwnershipService service) {
        super(instance);
        this.service = service;
    }

    @Override
    protected void removeRegistration() {
        service.unregisterCandidate(getInstance());
    }
}
