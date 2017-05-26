/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Path;

/**
 * Three Phase Commit Coordinator with support of user-supplied commit cohorts
 * which participates in three-phase commit protocols
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncConfigurationCommitCoordinator<P extends Path<P>, D> {

    /**
     * Register configuration commit handler for particular subtree
     *
     * Configuration commit handler is invoked for all write transactions
     * which modifies <code>subtree</code>
     *
     * @param subtree Subtree which configuration commit handler is interested it
     * @param commitHandler Instance of user-provided commit handler
     * @return Registration object representing this registration. Invoking {@link ObjectRegistration#close()}
     *   will unregister configuration commit handler.
     */
    <C extends AsyncConfigurationCommitCohort<P, D>> ObjectRegistration<C> registerConfigurationCommitHandler(
            P subtree, C commitHandler);
}
