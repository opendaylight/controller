/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.policy;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.osgi.service.component.annotations.Component;

@Singleton
@Component
@MetaInfServices
public final class DefaultRaftPolicy implements RaftPolicy {
    @Inject
    public DefaultRaftPolicy() {
        // Nothing here
    }

    @Override
    public boolean automaticElectionsEnabled() {
        return true;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return false;
    }
}
