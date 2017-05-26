/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.dummy.datastore;

public class Configuration {
    private final int maxDelayInMillis;
    private final boolean dropReplies;
    private final boolean causeTrouble;

    public Configuration(int maxDelayInMillis, boolean dropReplies, boolean causeTrouble) {
        this.maxDelayInMillis = maxDelayInMillis;
        this.dropReplies = dropReplies;
        this.causeTrouble = causeTrouble;
    }

    public int getMaxDelayInMillis() {
        return maxDelayInMillis;
    }

    public boolean shouldDropReplies() {
        return dropReplies;
    }

    public boolean shouldCauseTrouble() {
        return causeTrouble;
    }
}
