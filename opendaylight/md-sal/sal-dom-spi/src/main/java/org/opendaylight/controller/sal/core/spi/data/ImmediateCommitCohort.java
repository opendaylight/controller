/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * A simplified commit staging alternative to {@link DOMStoreThreePhaseCommitCohort}.
 * It does not support synchronized commit across multiple instances as it really has
 * two steps with no required abort cleanup.
 */
public interface ImmediateCommitCohort<T> extends Identifiable<T> {
    /**
     * Prepare for commit. Returned object is the ticket and all associated state
     * required to commit that ticket.
     *
     * @return Commit ticket.
     */
    ImmediateCommitCandidate<T> prepare();
}
