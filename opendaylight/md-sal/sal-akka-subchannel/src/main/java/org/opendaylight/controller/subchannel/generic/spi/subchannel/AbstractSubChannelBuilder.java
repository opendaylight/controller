/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.subchannel;

import java.io.Serializable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;

/**
 * Created by HanJie on 2017/2/6.
 *
 * @author Han Jie
 */

@SuppressWarnings("unchecked")
public abstract class AbstractSubChannelBuilder<T extends AbstractSubChannelBuilder<T>> implements Serializable{
    private Optional<String> parentName;
    private Optional<Config> config;
    private String proxyName;

    protected void verify() {
        Preconditions.checkNotNull(proxyName, "cluster should not be null");

    }

    protected T self(){
        return (T) this;
    }

    public String getProxyName() {
        return proxyName;
    }

    public T setProxyName(String proxyName) {
        this.proxyName = proxyName;
        return self();
    }

    public Optional<String> getParentName() {
        return parentName;
    }

    public T setParentName(Optional<String> parentName) {
        this.parentName = parentName;
        return self();
    }

    public Optional<Config> getConfig() {
        return config;
    }

    public T setConfig(Optional<Config> config) {
        this.config = config;
        return self();
    }

}
