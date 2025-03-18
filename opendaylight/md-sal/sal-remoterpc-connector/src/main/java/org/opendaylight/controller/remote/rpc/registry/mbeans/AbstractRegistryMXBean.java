/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.Address;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.remote.rpc.registry.AbstractRoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;
import scala.jdk.javaapi.DurationConverters;
import scala.jdk.javaapi.FutureConverters;

abstract class AbstractRegistryMXBean<T extends AbstractRoutingTable<T, I>, I> extends AbstractMXBean {
    static final String LOCAL_CONSTANT = "local";
    static final String ROUTE_CONSTANT = "route:";
    static final String NAME_CONSTANT = " | name:";

    @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final BucketStoreAccess bucketAccess;
    private final FiniteDuration timeout;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
        justification = "registerMBean() is expected to be stateless")
    AbstractRegistryMXBean(final @NonNull String beanName, final @NonNull String beanType,
            final @NonNull BucketStoreAccess bucketAccess, final @NonNull Duration timeout) {
        super(beanName, beanType, null);
        this.bucketAccess = requireNonNull(bucketAccess);
        this.timeout = DurationConverters.toScala(timeout);
        registerMBean();
    }

    final T localData() {
        try {
            return Await.result(FutureConverters.asScala(bucketAccess.<T>getLocalData()), timeout);
        } catch (InterruptedException | TimeoutException e) {
            throw new IllegalStateException("getLocalData failed", e);
        }
    }

    final Map<Address, Bucket<T>> remoteBuckets() {
        try {
            return Await.result(FutureConverters.asScala(bucketAccess.getRemoteBuckets()), timeout);
        } catch (InterruptedException | TimeoutException e) {
            throw new IllegalStateException("getRemoteBuckets failed", e);
        }
    }

    final String bucketVersions() {
        try {
            return Await.result(FutureConverters.asScala(bucketAccess.getBucketVersions()), timeout).toString();
        } catch (InterruptedException | TimeoutException e) {
            throw new IllegalStateException("getVersions failed", e);
        }
    }
}
