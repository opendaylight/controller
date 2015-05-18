/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

/**
 * This exception could occur if shard leader re-election is in progress
 */

public class DataStoreUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    private String shard;

    private long waitMilliseconds;

    public DataStoreUnavailableException(String shard, long waitMilliseconds, String message){
        super(message);
        this.shard = shard;
        this.waitMilliseconds = waitMilliseconds;
    }

    public long getWaitMilliseconds() {
        return waitMilliseconds;
    }

    public String getShard() {
        return shard;
    }
}
