/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.threadgroup;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(pid = "org.opendaylight.netty.threadgroup")
public @interface Configuration {
    @AttributeDefinition(name = "global-boss-group-thread-count")
    int bossThreadCount() default 0;

    @AttributeDefinition(name = "global-worker-group-thread-count")
    int workerThreadCount() default 0;
}
