/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.shutdown.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.shutdown.ShutdownService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownServiceImpl implements ShutdownService, AutoCloseable {
    private final ShutdownService impl;
    private final ShutdownRuntimeRegistration registration;

    public ShutdownServiceImpl(String secret, Bundle systemBundle,
                               ShutdownRuntimeRegistrator rootRuntimeBeanRegistratorWrapper) {
        if (secret == null) {
            throw new IllegalArgumentException("Secret cannot be null");
        }
        impl = new Impl(secret, systemBundle);
        registration = rootRuntimeBeanRegistratorWrapper.register(new MXBeanImpl(impl));
    }

    @Override
    public void shutdown(String inputSecret, Optional<String> reason) {
        impl.shutdown(inputSecret, reason);
    }

    @Override
    public void close() {
        registration.close();
    }
}

class Impl implements ShutdownService {
    private static final Logger logger = LoggerFactory.getLogger(Impl.class);
    private final String secret;
    private final Bundle systemBundle;

    Impl(String secret, Bundle systemBundle) {
        this.secret = secret;
        this.systemBundle = systemBundle;
    }

    @Override
    public void shutdown(String inputSecret, Optional<String> reason) {
        logger.warn("Shutdown issued with secret {} and reason {}", inputSecret, reason);
        try {
            Thread.sleep(1000); // prevent brute force attack
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Shutdown process interrupted", e);
        }
        if (this.secret.equals(inputSecret)) {
            logger.info("Server is shutting down");

            Thread stopSystemBundle = new Thread() {
                @Override
                public void run() {
                    try {
                        // wait so that JMX response is received
                        Thread.sleep(1000);
                        systemBundle.stop();
                    } catch (BundleException e) {
                        logger.warn("Can not stop OSGi server", e);
                    } catch (InterruptedException e) {
                        logger.warn("Shutdown process interrupted", e);
                    }
                }
            };
            stopSystemBundle.start();

        } else {
            logger.warn("Unauthorized attempt to shut down server");
            throw new IllegalArgumentException("Invalid secret");
        }
    }

}

class MXBeanImpl implements ShutdownRuntimeMXBean {
    private final ShutdownService impl;

    MXBeanImpl(ShutdownService impl) {
        this.impl = impl;
    }

    @Override
    public void shutdown(String inputSecret, String nullableReason) {
        Optional<String> optionalReason;
        if (nullableReason == null) {
            optionalReason = Optional.absent();
        } else {
            optionalReason = Optional.of(nullableReason);
        }
        impl.shutdown(inputSecret, optionalReason);
    }
}
