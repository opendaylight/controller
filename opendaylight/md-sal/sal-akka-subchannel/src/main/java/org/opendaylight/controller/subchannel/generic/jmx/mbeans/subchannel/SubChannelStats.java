/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.jmx.mbeans.subchannel;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import org.opendaylight.controller.subchannel.generic.spi.proxy.AbstractSubChannelProxy;
import org.opendaylight.controller.subchannel.generic.spi.proxy.ProxyContext;

/**
 * Created by HanJie on 2017/3/23.
 *
 * @author Han Jie
 */
public class SubChannelStats<T,C>  extends AbstractMXBean implements SubChannelMXBean {
    public static String JMX_CATEGORY_SUBCHANNEL = "SubChannelProxy";
    private AbstractSubChannelProxy<T,C> subChannelProxy;

    public SubChannelStats(@Nonnull  String proxyId, @Nonnull String mBeanType) {
        super(proxyId,mBeanType,JMX_CATEGORY_SUBCHANNEL);
    }

    public void setSubChannelProxy(AbstractSubChannelProxy<T, C> subChannelProxy) {
        this.subChannelProxy = subChannelProxy;
    }

    @Override
    public int getChunkSize() {
        return subChannelProxy.getConfigParams().getChunkSize();
    }

    @Override
    public long getMessageTimeoutInSeconds() {
        return subChannelProxy.getConfigParams().getMessageTimeoutInSeconds();
    }

    @Override
    public long getSerializerTimeoutInSeconds() {
        return subChannelProxy.getConfigParams().getSerializerTimeoutInSeconds();
    }

    @Override
    public String getLocalProxy() {
        return subChannelProxy.getSelf().toString();
    }

    @Override
    public List<String> getRemoteProxys() {
        List<String> remoteProxys = new ArrayList<>();

        for (ProxyContext<T> s : subChannelProxy.getProxyContexts()) {
            remoteProxys.add(s.getRemoteProxy().toString());
        }

        return remoteProxys;
    }
}
