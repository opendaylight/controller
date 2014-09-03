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

/**
 * Maintains metrics registry that is provided to reporters.
 * At the moment only one reporter exists {@code JmxReporter}.
 * More reporters can be added.
 * <p/>
 * The consumers of this class will only be interested in {@code MetricsRegistry}
 * where metrics for that consumer gets stored.
 */
public class MetricsReporter implements AutoCloseable{

    private final MetricRegistry METRICS_REGISTRY = new MetricRegistry();
    private final String DOMAIN = "org.opendaylight.controller.actor.metric";

    public final JmxReporter jmxReporter = JmxReporter.forRegistry(METRICS_REGISTRY).inDomain(DOMAIN).build();

    private static MetricsReporter inst = new MetricsReporter();

    private MetricsReporter(){
        jmxReporter.start();
    }

    public static MetricsReporter getInstance(){
        return inst;
    }

    public MetricRegistry getMetricsRegistry(){
        return METRICS_REGISTRY;
    }

    @Override
    public void close() throws Exception {
        jmxReporter.close();
    }
}
