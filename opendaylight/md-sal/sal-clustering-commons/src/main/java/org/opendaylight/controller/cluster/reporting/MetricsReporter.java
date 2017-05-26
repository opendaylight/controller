/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.reporting;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Maintains metrics registry that is provided to reporters.
 * At the moment only one reporter exists {@code JmxReporter}.
 * More reporters can be added.
 *
 * <p>
 * The consumers of this class will only be interested in {@code MetricsRegistry}
 * where metrics for that consumer gets stored.
 */
public class MetricsReporter implements AutoCloseable {

    private static final LoadingCache<String, MetricsReporter> METRIC_REPORTERS = CacheBuilder.newBuilder().build(
        new CacheLoader<String, MetricsReporter>() {
            @Override
            public MetricsReporter load(final String domainName) {
                return new MetricsReporter(domainName);
            }
        });

    private final String domainName;
    private final JmxReporter jmxReporter;
    private final MetricRegistry metricRegistry = new MetricRegistry();

    private MetricsReporter(final String domainName) {
        this.domainName = domainName;
        jmxReporter = JmxReporter.forRegistry(metricRegistry).inDomain(domainName).build();
        jmxReporter.start();
    }

    public static MetricsReporter getInstance(final String domainName) {
        return METRIC_REPORTERS.getUnchecked(domainName);
    }

    public MetricRegistry getMetricsRegistry() {
        return metricRegistry;
    }

    @Override
    public void close() {
        jmxReporter.close();

        METRIC_REPORTERS.invalidate(domainName);
    }
}
