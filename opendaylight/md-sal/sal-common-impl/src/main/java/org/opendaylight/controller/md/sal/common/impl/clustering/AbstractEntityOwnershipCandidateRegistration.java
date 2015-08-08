/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.clustering;

import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;

/**
 * Abstract base class for an EntityOwnershipCandidateRegistration.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractEntityOwnershipCandidateRegistration
        extends AbstractEntityOwnershipListenerRegistration<EntityOwnershipCandidate>
        implements EntityOwnershipCandidateRegistration {

    protected AbstractEntityOwnershipCandidateRegistration(EntityOwnershipCandidate candidate, Entity entity) {
        super(candidate, entity);
    }
}
