/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Marker interface for grouping session termination cause.
 */
@Deprecated
public interface TerminationReason {

    /**
     * Get cause of session termination.
     * @return human-readable cause.
     */
    String getErrorMessage();
}

