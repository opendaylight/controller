/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.proxy;


/**
 * Created by HanJie on 2017/2/9.
 *
 * @author Han Jie
 */
abstract public class AbstractSubChannelProxyPathPolicy<T> {
     abstract public String getSubChannelProxyPath(T local);
}
