/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.proxy;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/3/18.
 *
 * @author Han Jie
 */
public class ProxyLink<T> implements Serializable{
    private T localProxy;
    private T remoteProxy;

    ProxyLink(T localProxy, T remoteProxy) {
        this.localProxy = localProxy;
        this.remoteProxy = remoteProxy;
    }

    public T getLocalProxy() {
        return localProxy;
    }

    public T getRemoteProxy() {
        return remoteProxy;
    }

    @Override
    public int hashCode() {
        return remoteProxy.hashCode()*31 + localProxy.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProxyLink)) {
            return false;
        }

        final ProxyLink<?> other = (ProxyLink<?>) o;
        return remoteProxy.equals(other.getRemoteProxy()) &&  localProxy.equals(other.getLocalProxy());
    }
}
