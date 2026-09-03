/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.spi.ConfigurationReader;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * OSGi bootstrap for {@link ActorSystemInstance}. It activates {@link OSGiActorSystemInstance}, ensuring the underlying
 * actor system is shut down before attempting re-activation.
 */
@Component
public final class OSGiActorSystemBootstrap {

    @Activate
    public OSGiActorSystemBootstrap(@Reference final ActorSystemInstance.Creator instanceCreator,
            @Reference final ConfigurationReader configReader,
            @Reference(target = "(component.factory=" + OSGiActorSystemInstance.FACTORY_NAME + ")")
            final ComponentFactory<OSGiActorSystemInstance> datastoreFactory) {

    }
}
