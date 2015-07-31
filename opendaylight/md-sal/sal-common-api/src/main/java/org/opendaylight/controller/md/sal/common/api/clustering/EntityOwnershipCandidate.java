/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

/**
 * <p>
 * An EntityOwnershipCandidate represents a component which would like to own a given Entity.
 * The EntityOwnershipCandidate will be notified of changes in ownership as it is also an EntityOwnershipListener.
 * </p>
 */
public interface EntityOwnershipCandidate extends EntityOwnershipListener {
}
