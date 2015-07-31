/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

/**
 * An EntityOwnershipCandidateRegistration records a request to register a Candidate for a given Entity. Calling
 * close on the EntityOwnershipCandidateRegistration will remove the Candidate from any future ownership considerations
 * for that Entity and will also remove it as a Listener for ownership status changes.
 */
public interface EntityOwnershipCandidateRegistration extends EntityOwnershipListenerRegistration {
}
