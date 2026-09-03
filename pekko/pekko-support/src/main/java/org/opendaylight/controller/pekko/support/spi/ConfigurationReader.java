/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import com.google.common.annotations.Beta;
import com.typesafe.config.Config;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An intermediate interface providing Pekko configuration.
 *
 * @since 14.0.0
 */
@Beta
@NonNullByDefault
public interface ConfigurationReader {
    /**
     * Read the configuration.
     *
     * @return the configuration
     */
    // FIXME: document exceptions
    Config read();
}
