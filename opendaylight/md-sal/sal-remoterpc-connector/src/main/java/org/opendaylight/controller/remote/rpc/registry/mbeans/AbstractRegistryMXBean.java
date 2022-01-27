/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import static java.util.Objects.requireNonNull;

import akka.actor.Address;
import akka.util.Timeout;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.remote.rpc.registry.AbstractRoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

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
            final @NonNull BucketStoreAccess bucketAccess, final @NonNull Timeout timeout) {
        super(beanName, beanType, null);
        this.bucketAccess = requireNonNull(bucketAccess);
        this.timeout = timeout.duration();
        registerMBean();
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    final T localData() {
        try {
            return (T) Await.result((Future) bucketAccess.getLocalData(), timeout);
        } catch (Exception e) {
            throw new RuntimeException("getLocalData failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    final Map<Address, Bucket<T>> remoteBuckets() {
        try {
            return (Map<Address, Bucket<T>>) Await.result((Future)bucketAccess.getRemoteBuckets(), timeout);
        } catch (Exception e) {
            throw new RuntimeException("getRemoteBuckets failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    final String bucketVersions() {
        try {
            return Await.result((Future)bucketAccess.getBucketVersions(), timeout).toString();
        } catch (Exception e) {
            throw new RuntimeException("getVersions failed", e);
        }
    }
}
