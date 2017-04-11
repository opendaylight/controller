/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subchannel.generic.config;

/**
 * Created by HanJie on 2017/3/22.
 *
 * @author Han Jie
 */
public interface ProcedureConfigParams<T extends ProcedureConfigParams<T>> {

    /**
     * The size (in bytes) of the chunk sent between subchannel proxys
     */
    int getChunkSize();

    T setChunkSize(int chunkSize);
    /**
     * The amount of time to wait for every chunkmessage or its reply.
     */
    long getMessageTimeoutInSeconds();

    T setMessageTimeoutInSeconds(long timeoutInSeconds);
}
