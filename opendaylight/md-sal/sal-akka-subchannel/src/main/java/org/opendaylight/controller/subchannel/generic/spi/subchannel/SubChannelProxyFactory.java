/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subchannel.generic.spi.subchannel;

/**
 * Created by HanJie on 2017/3/24.
 *
 * @author Han Jie
 */
public interface SubChannelProxyFactory<T> {
    T createSubChannelProxy(AbstractSubChannelBuilder<?> builder);
}
