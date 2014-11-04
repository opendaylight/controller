/*
 * Copyright (c) 2014 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;

import org.opendaylight.controller.sal.utils.GlobalConstants;

/**
 * An instance of this class keeps a protocol plugin service handler.
 *
 * @param <T>  Type of protocol plugin service.
 */
public final class ProtocolService<T> {
    /**
     * Default priority value.
     */
    private static final int  DEFAULT_PRIORITY = 0;

    /**
     * A protocol plugin service handler.
     */
    private final T  service;

    /**
     * A priority value assigned to this protocol plugin.
     */
    private final int  priority;

    /**
     * Set protocol plugin service.
     *
     * @param map     A map that keeps protocol plugin services.
     * @param props   Service properties.
     * @param s       Protocol plugin service.
     * @param logger  A logger instance.
     * @param <S>     Type of protocol plugin service.
     */
    public static <S> void set(ConcurrentMap<String, ProtocolService<S>> map,
                               Map<?, ?> props, S s, Logger logger) {
        if (map == null) {
            logger.error("Protocol plugin service store is null.");
            return;
        }
        if (s == null) {
            logger.error("Protocol plugin service is null.");
            return;
        }
        if (props == null) {
            logger.error("Service property is null.");
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Received set service request: {}", s);
            for (Map.Entry<?, ?> entry: props.entrySet()) {
                logger.trace("Prop key:({}) value:({})", entry.getKey(),
                             entry.getValue());
            }
        }

        Object value = props.get(GlobalConstants.PROTOCOLPLUGINTYPE.toString());
        if (!(value instanceof String)) {
            logger.error("Unexpected protocol type: {}", value);
            return;
        }

        String type = (String)value;
        ProtocolService<S> service = new ProtocolService<S>(props, s);
        ProtocolService<S> old = map.putIfAbsent(type, service);
        while (old != null) {
            // Compare priority value.
            if (old.getPriority() >= service.getPriority()) {
                logger.trace("Protocol plugin service for {} is already set: " +
                             "current={}, requested={}", type, old, service);
                return;
            }

            if (map.replace(type, old, service)) {
                break;
            }
            old = map.putIfAbsent(type, service);
        }

        logger.debug("Stored protocol plugin service for {}: {}",
                     type, service);
    }

    /**
     * Unset protocol plugin service.
     *
     * @param map     A map that keeps protocol plugin services.
     * @param props   Service properties.
     * @param s       Protocol plugin service.
     * @param logger  A logger instance.
     * @param <S>     Type of protocol plugin service.
     */
    public static <S> void unset(ConcurrentMap<String, ProtocolService<S>> map,
                                 Map<?, ?> props, S s, Logger logger) {
        if (map == null) {
            logger.error("Protocol plugin service store is null.");
            return;
        }
        if (s == null) {
            logger.error("Protocol plugin service is null.");
            return;
        }
        if (props == null) {
            logger.error("Service property is null.");
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Received unset service request: {}", s);
            for (Map.Entry<?, ?> entry: props.entrySet()) {
                logger.trace("Prop key:({}) value:({})",
                             entry.getKey(), entry.getValue());
            }
        }

        Object value = props.get(GlobalConstants.PROTOCOLPLUGINTYPE.toString());
        if (!(value instanceof String)) {
            logger.error("Unexpected protocol type {}: service={}", value, s);
            return;
        }

        String type = (String)value;
        ProtocolService<S> plugin = new ProtocolService<S>(props, s);
        if (map.remove(type, plugin)) {
            logger.debug("Removed protocol plugin service for {}: {}",
                         type, plugin);
        } else {
            logger.trace("Ignore unset service request for {}: {}",
                         type, plugin);
        }
    }

    /**
     * Constructor.
     *
     * @param props  Protocol plugin service properties.
     * @param s      A protocol plugin service handler.
     */
    public ProtocolService(Map<?, ?> props, T s) {
        service = s;

        String key = GlobalConstants.PROTOCOLPLUGINPRIORITY.toString();
        Object value = props.get(key);
        if (value instanceof Integer) {
            priority = ((Integer)value).intValue();
        } else {
            priority = DEFAULT_PRIORITY;
        }
    }

    /**
     * Return a protocol plugin service handler.
     *
     * @return  A protocol plugin service handler.
     */
    public T getService() {
        return service;
    }

    /**
     * Return a priority value assigned to this protocol plugin.
     *
     * @return  A priority value.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Determine whether the given object is identical to this object.
     *
     * @param o  An object to be compared.
     * @return   {@code true} if identical. Otherwise {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        ProtocolService plugin = (ProtocolService)o;
        return (service.equals(plugin.service) && priority == plugin.priority);
    }

    /**
     * Return the hash code of this object.
     *
     * @return  The hash code.
     */
    @Override
    public int hashCode() {
        return service.hashCode() + (priority * 31);
    }

    /**
     * Return a string representation of this instance.
     *
     * @return  A string representation of this instance.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[service=");
        return builder.append(service).append(", priority=").append(priority).
            append(']').toString();
    }
}
