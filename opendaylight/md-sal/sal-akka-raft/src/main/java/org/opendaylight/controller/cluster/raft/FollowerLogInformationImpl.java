/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.util.concurrent.atomic.AtomicLong;

public class FollowerLogInformationImpl implements FollowerLogInformation{

    private final String id;

    private final AtomicLong nextIndex;

    private final AtomicLong matchIndex;

    public FollowerLogInformationImpl(String id, AtomicLong nextIndex,
        AtomicLong matchIndex) {
        this.id = id;
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
    }

    public long incrNextIndex(){
        return nextIndex.incrementAndGet();
    }

    public long incrMatchIndex(){
        return matchIndex.incrementAndGet();
    }

    public String getId() {
        return id;
    }

    public AtomicLong getNextIndex() {
        return nextIndex;
    }

    public AtomicLong getMatchIndex() {
        return matchIndex;
    }

}
