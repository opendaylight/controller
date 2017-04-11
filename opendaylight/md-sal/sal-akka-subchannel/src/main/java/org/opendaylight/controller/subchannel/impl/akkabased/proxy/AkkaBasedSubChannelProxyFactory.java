/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.proxy;

import akka.actor.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/3/23.
 *
 * @author Han Jie
 */
public class AkkaBasedSubChannelProxyFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AkkaBasedSubChannelProxyFactory.class);

    public static AkkaBasedSubChannelProxy createInstance(ActorContext actorContext, SubChannelProxyActor.AbstractBuilder<?> builder) {
        LOG.info("Create AkkaBasedSubChannelProxy instance");
        return new AkkaBasedSubChannelProxy(actorContext, builder);
    }
}
