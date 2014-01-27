/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.api;

import java.util.SortedSet;

public interface ConfigSnapshotHolder {

    /**
     * Get part of get-config document that contains just
     */
    String getConfigSnapshot();


    /**
     * Get only required capabilities referenced by the snapshot.
     */
    SortedSet<String> getCapabilities();

}
