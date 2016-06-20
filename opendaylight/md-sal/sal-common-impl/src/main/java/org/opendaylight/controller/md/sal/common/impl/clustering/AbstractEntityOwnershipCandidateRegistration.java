/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.clustering;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Abstract base class for an EntityOwnershipCandidateRegistration.
 *
 * @author Thomas Pantelis
 * @deprecated use insted
 *             org.opendaylight.mdsal.common.api.clustering.AbstractGenericEntityOwnershipCandidateRegistration
 */
@Deprecated
public abstract class AbstractEntityOwnershipCandidateRegistration extends AbstractObjectRegistration<Entity>
        implements EntityOwnershipCandidateRegistration {

    protected AbstractEntityOwnershipCandidateRegistration(@Nonnull final Entity entity) {
        super(Preconditions.checkNotNull(entity, "entity cannot be null"));
    }
}
