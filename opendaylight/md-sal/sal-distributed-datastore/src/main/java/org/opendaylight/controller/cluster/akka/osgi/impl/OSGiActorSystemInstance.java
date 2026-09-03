/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.spi.ForwardingActorSystemInstance;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(factory = OSGiActorSystemInstance.FACTORY_NAME, service = ActorSystemInstance.class)
public final class OSGiActorSystemInstance extends ForwardingActorSystemInstance {
    record PropsAndStage(Dictionary<String, ?> properties, CompletionStage<@Nullable Void> whenShutdown) {
        PropsAndStage {
            requireNonNull(properties);
            requireNonNull(whenShutdown);
        }
    }

    // OSGi DS Component Factory name
    static final String FACTORY_NAME = "org.opendaylight.controller.cluster.akka.osgi.impl.OSGiActorSystemInstance";

    private static final Logger LOG = LoggerFactory.getLogger(OSGiActorSystemInstance.class);
    private static final String DELEGATE_PROP = ".delegate";
    private static final String FUTURE_PROP = ".future";

    private final ActorSystemInstance.WithShutdown delegate;
    private final CompletableFuture<@Nullable Void> future;

    @Activate
    @SuppressWarnings("unchecked")
    public OSGiActorSystemInstance(final Map<String, ?> properties) {
        var prop = properties.get(DELEGATE_PROP);
        if (prop == null) {
            throw new VerifyException("Missing delegate");
        }
        delegate = (ActorSystemInstance.WithShutdown) prop;
        prop = properties.get(FUTURE_PROP);
        if (prop == null) {
            throw new VerifyException("Missing future");
        }
        future = (CompletableFuture<@Nullable Void>) prop;
    }

    @Deactivate
    void deactivate() {
        LOG.info("Actor System provider stopping");
        delegate.shutdown().whenComplete((unused, failure) -> {
            if (failure == null) {
                LOG.info("Actor System provider stopped");
                future.complete(null);
            } else {
                LOG.warn("Actor System provider stopped with a failure", failure);
                future.completeExceptionally(failure);
            }
        });
    }

    static PropsAndStage properties(final ActorSystemInstance.WithShutdown delegate) {
        final var future = new CompletableFuture<@Nullable Void>();
        return new PropsAndStage(
            FrameworkUtil.asDictionary(Map.of(
                DELEGATE_PROP, delegate,
                FUTURE_PROP, future)),
            future.minimalCompletionStage());
    }

    @Override
    protected ActorSystemInstance delegate() {
        return delegate;
    }
}
