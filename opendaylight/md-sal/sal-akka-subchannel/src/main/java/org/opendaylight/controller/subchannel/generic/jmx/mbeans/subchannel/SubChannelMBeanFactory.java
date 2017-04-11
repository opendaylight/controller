/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.jmx.mbeans.subchannel;

import javax.annotation.Nullable;

/**
 * Created by HanJie on 2017/3/23.
 *
 * @author Han Jie
 */
public class SubChannelMBeanFactory {

    public static SubChannelStats getSubChannelMBean(final String proxyName, @Nullable final String mxBeanType) {
        String finalMXBeanType = mxBeanType != null ? mxBeanType : "SubChannel";
        SubChannelStats subchannelStatsMBeanImpl = new SubChannelStats<>(proxyName, finalMXBeanType);
        subchannelStatsMBeanImpl.registerMBean();
        return subchannelStatsMBeanImpl;
    }
}
