/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class CommonConfig extends AbstractConfig {

    protected static final String TAG_ACTOR_SYSTEM_NAME = "actor-system-name";
    protected static final String TAG_METRIC_CAPTURE_ENABLED = "metric-capture-enabled";
    protected static final String TAG_MAILBOX_CAPACITY = "mailbox-capacity";
    protected static final String TAG_MAILBOX = "bounded-mailbox";
    protected static final String TAG_MAILBOX_PUSH_TIMEOUT = "mailbox-push-timeout-time";

    //TODO: Ideally these defaults should go to reference.conf
    // https://bugs.opendaylight.org/show_bug.cgi?id=1709
    private static final int DEFAULT_MAILBOX_CAPACITY = 1000;
    private static final int DEFAULT_MAILBOX_PUSH_TIMEOUT = 100;

    //locally cached values
    private FiniteDuration cachedMailBoxPushTimeout;
    private Integer cachedMailBoxCapacity;
    private Boolean cachedMetricCaptureEnableFlag;

    public CommonConfig(Config config) {
        super(config);
    }

    public String getActorSystemName() {
        return get().getString(TAG_ACTOR_SYSTEM_NAME);
    }

    public boolean isMetricCaptureEnabled() {
        if (cachedMetricCaptureEnableFlag != null) {
            return cachedMetricCaptureEnableFlag;
        }

        cachedMetricCaptureEnableFlag = get().hasPath(TAG_METRIC_CAPTURE_ENABLED)
                ? get().getBoolean(TAG_METRIC_CAPTURE_ENABLED)
                : false;

        return cachedMetricCaptureEnableFlag;
    }

    public String getMailBoxName() {
        return TAG_MAILBOX;
    }

    public Integer getMailBoxCapacity() {

        if (cachedMailBoxCapacity != null) {
            return cachedMailBoxCapacity;
        }

        final String PATH = TAG_MAILBOX + "." + TAG_MAILBOX_CAPACITY;
        cachedMailBoxCapacity = get().hasPath(PATH)
                ? get().getInt(PATH)
                : DEFAULT_MAILBOX_CAPACITY;

        return cachedMailBoxCapacity;
    }

    public FiniteDuration getMailBoxPushTimeout() {

        if (cachedMailBoxPushTimeout != null) {
            return cachedMailBoxPushTimeout;
        }

        final String PATH = TAG_MAILBOX + "." + TAG_MAILBOX_PUSH_TIMEOUT;

        long timeout = get().hasPath(PATH)
                ? get().getDuration(PATH, TimeUnit.NANOSECONDS)
                : DEFAULT_MAILBOX_PUSH_TIMEOUT;

        cachedMailBoxPushTimeout = new FiniteDuration(timeout, TimeUnit.NANOSECONDS);
        return cachedMailBoxPushTimeout;
    }

    public static class Builder<T extends Builder<T>> extends AbstractConfig.Builder<T> {

        public Builder(String actorSystemName) {
            super(actorSystemName);

            //actor system config
            configHolder.put(TAG_ACTOR_SYSTEM_NAME, actorSystemName);

            //config for bounded mailbox
            configHolder.put(TAG_MAILBOX, new HashMap<String, Object>());
        }

        @SuppressWarnings("unchecked")
        public T metricCaptureEnabled(boolean enabled) {
            configHolder.put(TAG_METRIC_CAPTURE_ENABLED, String.valueOf(enabled));
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T mailboxCapacity(int capacity) {
            Preconditions.checkArgument(capacity > 0, "mailbox capacity must be >0");

            Map<String, Object> boundedMailbox = (Map<String, Object>) configHolder.get(TAG_MAILBOX);
            boundedMailbox.put(TAG_MAILBOX_CAPACITY, capacity);
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T mailboxPushTimeout(String timeout) {
            Duration pushTimeout = Duration.create(timeout);
            Preconditions.checkArgument(pushTimeout.isFinite(), "invalid value for mailbox push timeout");

            Map<String, Object> boundedMailbox = (Map<String, Object>) configHolder.get(TAG_MAILBOX);
            boundedMailbox.put(TAG_MAILBOX_PUSH_TIMEOUT, timeout);
            return (T)this;
        }

        public CommonConfig build() {
            return new CommonConfig(merge());
        }
    }
}
