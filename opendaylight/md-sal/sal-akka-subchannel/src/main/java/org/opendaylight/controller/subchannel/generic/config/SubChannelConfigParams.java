/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
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
public interface SubChannelConfigParams extends ProcedureConfigParams<SubChannelConfigParams> {

    /**
     * The maximum amount of time a serializer can be idle without
     * receiving the next messages before it closes itself
     */
    long getSerializerTimeoutInSeconds();

    SubChannelConfigParams setSerializerTimeoutInSeconds(long timeoutInSeconds);
}
