/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.api.akkabased;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.base.Optional;
import com.typesafe.config.Config;
import org.opendaylight.controller.subchannel.impl.akkabased.channels.AkkaBasedActorAwareSubChannel;
import org.opendaylight.controller.subchannel.impl.akkabased.channels.AkkaBasedNonActorAwareSubChannel;
import org.opendaylight.controller.subchannel.api.SubChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/3/24.
 *
 * @author Han Jie
 */
public class AkkaBasedSubChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AkkaBasedSubChannelFactory.class);

    /**
     *  Instantiated by Actor such as {@link org.opendaylight.controller.cluster.datastore.ShardTransaction}
     *  with configuration of "sal-akka-subchannel" defined in application.conf.
     *  @param actorContext input ActorContext of the Actor when instantiated by Actor
     *  @param config the configuration of "sal-akka-subchannel"
     *  @return Service {@link SubChannel}
     */
    public static SubChannel<ActorRef> createInstance(ActorContext actorContext,Optional<Config> config) {
        LOG.info("Create AkkaBasedActorAwareSubChannel instance for Actor {} with config {}",
                actorContext.self().toString(),config);
        return new AkkaBasedActorAwareSubChannel(actorContext, config);
    }

    /**
     *  Instantiated by Actor such as {@link org.opendaylight.controller.cluster.datastore.ShardTransaction}
     *  with default configuration.
     *  @param actorContext input ActorContext of the Actor when instantiated by Actor
     *  @return Service {@link SubChannel}
     */
    public static SubChannel<ActorRef> createInstance(ActorContext actorContext) {
        LOG.info("Create AkkaBasedActorAwareSubChannel instance for Actor {} without config",
                actorContext.self().toString());
        return new AkkaBasedActorAwareSubChannel(actorContext,Optional.absent());
    }

    /**
     *  Instantiated by NonActor such as {@link org.opendaylight.controller.cluster.datastore.DistributedDataStore}
     *  with configuration of "sal-akka-subchannel" defined in application.conf.
     *  @param actorSystem input ActorSystem when instantiated by NonActor
     *  @param config the configuration of "sal-akka-subchannel"
     *  @return Service {@link SubChannel}
     */
    public static SubChannel<ActorRef> createInstance(ActorSystem actorSystem,String proxyNameSuffix,
                                                      Optional<Config> config) {
        LOG.info("Create AkkaBasedActorAwareSubChannel instance with proxyNameSuffix {} config {}",
                proxyNameSuffix,config);
        return new AkkaBasedNonActorAwareSubChannel(actorSystem, proxyNameSuffix,config);
    }

    /**
     *  Instantiated by NonActor such as {@link org.opendaylight.controller.cluster.datastore.DistributedDataStore}
     *  with default configuration
     *  @param actorSystem input ActorSystem when instantiated by NonActor
     *  @return Service {@link SubChannel}
     */
    public static SubChannel<ActorRef> createInstance(ActorSystem actorSystem,String proxyNameSuffix) {
        LOG.info("Create AkkaBasedActorAwareSubChannel instance with proxyNameSuffix {} without config",
                proxyNameSuffix);
        return new AkkaBasedNonActorAwareSubChannel(actorSystem,proxyNameSuffix,Optional.absent());
    }
}
