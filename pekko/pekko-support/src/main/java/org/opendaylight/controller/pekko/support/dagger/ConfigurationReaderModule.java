/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.dagger;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.impl.FileAkkaConfigurationReader;
import org.opendaylight.controller.pekko.support.spi.ConfigurationReader;

/**
 * Module providing reference {@link ConfigurationReader} instances.
 */
@Module
@NonNullByDefault
@SuppressWarnings("exports")
public interface ConfigurationReaderModule {
    /**
     * Return a {@link ConfigurationReader} backed by specified file.
     *
     * @param file the file
     * @return a {@link ConfigurationReader}
     */
    @Provides
    @Singleton
    static ConfigurationReader provideConfigurationReader(final Path file) {
        return new FileAkkaConfigurationReader();
    }
}
