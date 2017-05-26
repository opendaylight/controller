/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.shutdown.impl;

import com.google.common.base.Optional;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
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
    public void shutdown(String inputSecret, Long maxWaitTime, Optional<String> reason) {
        impl.shutdown(inputSecret, maxWaitTime, reason);
    }

    @Override
    public void close() {
        registration.close();
    }
}

class Impl implements ShutdownService {
    private static final Logger LOG = LoggerFactory.getLogger(Impl.class);
    private final String secret;
    private final Bundle systemBundle;

    Impl(String secret, Bundle systemBundle) {
        this.secret = secret;
        this.systemBundle = systemBundle;
    }

    @Override
    public void shutdown(String inputSecret, Long maxWaitTime, Optional<String> reason) {
        LOG.warn("Shutdown issued with secret {} and reason {}", inputSecret, reason);
        try {
            Thread.sleep(1000); // prevent brute force attack
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Shutdown process interrupted", e);
        }
        if (this.secret.equals(inputSecret)) {
            LOG.info("Server is shutting down");

            // actual work:
            Thread stopSystemBundleThread = new StopSystemBundleThread(systemBundle);
            stopSystemBundleThread.start();
            if (maxWaitTime != null && maxWaitTime > 0) {
                Thread systemExitThread = new CallSystemExitThread(maxWaitTime);
                LOG.debug("Scheduling {}", systemExitThread);
                systemExitThread.start();
            }
            // end
        } else {
            LOG.warn("Unauthorized attempt to shut down server");
            throw new IllegalArgumentException("Invalid secret");
        }
    }

}

class StopSystemBundleThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(StopSystemBundleThread.class);
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
            LOG.debug("Stopping system bundle");
            systemBundle.stop();
        } catch (BundleException e) {
            LOG.warn("Can not stop OSGi server", e);
        } catch (InterruptedException e) {
            LOG.warn("Shutdown process interrupted", e);
        }
    }
}

class CallSystemExitThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(CallSystemExitThread.class);
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
            LOG.error("Since some threads are still running, server is going to shut down via System.exit(1) !");
            // do a thread dump
            ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
            StringBuffer sb = new StringBuffer();
            for(ThreadInfo info : threads) {
                sb.append(info);
                sb.append("\n");
            }
            LOG.warn("Thread dump:{}", sb);
            System.exit(1);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted, not going to call System.exit(1)");
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
