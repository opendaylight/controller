/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.threadgroup;

import io.netty.channel.EventLoopGroup;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component(immediate = true, service = EventLoopGroup.class, property = "type=global-worker-group")
@Designate(ocd = Configuration.class)
public final class GlobalWorkerGroup extends AbstractGlobalGroup {
    @Activate
    public GlobalWorkerGroup(final Configuration configuration) {
        super(configuration.workerThreadCount());
    }
}
