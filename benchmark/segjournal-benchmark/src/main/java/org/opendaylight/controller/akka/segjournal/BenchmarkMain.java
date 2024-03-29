/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import akka.actor.ActorSystem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkMain {
    private static final String BENCHMARK = "benchmark";
    private static final Logger LOG = LoggerFactory.getLogger("benchmark");

    public static void main(String[] args) {

        final var config = BenchmarkConfigUtils.buildConfig(args);

        LOG.info("Starting");
        LOG.info("Config: {}", config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(config.workingDir())));

        final var system = ActorSystem.create(BENCHMARK);

        System.exit(0);
    }


}
