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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

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
    public void shutdown(String inputSecret, Long maxWaitTime, Optional<String> reason) {
        impl.shutdown(inputSecret, maxWaitTime, reason);
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
    public void shutdown(String inputSecret, Long maxWaitTime, Optional<String> reason) {
        logger.warn("Shutdown issued with secret {} and reason {}", inputSecret, reason);
        try {
            Thread.sleep(1000); // prevent brute force attack
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Shutdown process interrupted", e);
        }
        if (this.secret.equals(inputSecret)) {
            logger.info("Server is shutting down");

            // actual work:
            Thread stopSystemBundleThread = new StopSystemBundleThread(systemBundle);
            stopSystemBundleThread.start();
            if (maxWaitTime != null && maxWaitTime > 0) {
                Thread systemExitThread = new CallSystemExitThread(maxWaitTime);
                logger.debug("Scheduling {}", systemExitThread);
                systemExitThread.start();
            }
            // end
        } else {
            logger.warn("Unauthorized attempt to shut down server");
            throw new IllegalArgumentException("Invalid secret");
        }
    }

}

class StopSystemBundleThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(StopSystemBundleThread.class);
    public static final String CONFIG_MANAGER_SYMBOLIC_NAME = "org.opendaylight.controller.config-manager";
    private final Bundle systemBundle;

    StopSystemBundleThread(Bundle systemBundle) {
        super("stop-system-bundle");
        this.systemBundle = systemBundle;
    }

    @Override
    public void run() {
        try {
            // wait so that JMX response is received
            Thread.sleep(1000);
            // first try to stop config-manager
            Bundle configManager = findConfigManager();
            if (configManager != null){
                logger.debug("Stopping config-manager");
                configManager.stop();
                Thread.sleep(1000);
            }
            logger.debug("Stopping system bundle");
            systemBundle.stop();
        } catch (BundleException e) {
            logger.warn("Can not stop OSGi server", e);
        } catch (InterruptedException e) {
            logger.warn("Shutdown process interrupted", e);
        }
    }

    private Bundle findConfigManager() {
        for(Bundle bundle: systemBundle.getBundleContext().getBundles()){
            if (CONFIG_MANAGER_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }

}

class CallSystemExitThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(CallSystemExitThread.class);
    private final long maxWaitTime;
    CallSystemExitThread(long maxWaitTime) {
        super("call-system-exit-daemon");
        setDaemon(true);
        if (maxWaitTime <= 0){
            throw new IllegalArgumentException("Cannot schedule to zero or negative time:" + maxWaitTime);
        }
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public String toString() {
        return "CallSystemExitThread{" +
                "maxWaitTime=" + maxWaitTime +
                '}';
    }

    @Override
    public void run() {
        try {
            // wait specified time
            Thread.sleep(maxWaitTime);
            logger.error("Since some threads are still running, server is going to shut down via System.exit(1) !");
            // do a thread dump
            ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
            StringBuffer sb = new StringBuffer();
            for(ThreadInfo info : threads) {
                sb.append(info);
                sb.append("\n");
            }
            logger.warn("Thread dump:{}", sb);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.warn("Interrupted, not going to call System.exit(1)");
        }
    }
}


class MXBeanImpl implements ShutdownRuntimeMXBean {
    private final ShutdownService impl;

    MXBeanImpl(ShutdownService impl) {
        this.impl = impl;
    }

    @Override
    public void shutdown(String inputSecret, Long maxWaitTime, String nullableReason) {
        Optional<String> optionalReason;
        if (nullableReason == null) {
            optionalReason = Optional.absent();
        } else {
            optionalReason = Optional.of(nullableReason);
        }
        impl.shutdown(inputSecret, maxWaitTime, optionalReason);
    }
}
