/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.api.statistics;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Helper class for creating and populating instances of classes generated from the
 * <code>thread-executor-stats</code> yang grouping via reflection.
 *
 * @author Thomas Pantelis
 */
public final class ThreadExecutorStatsHelper {

    private static LoadingCache<Class<?>, Map<String, PropertyDescriptor>> propDescCache =
            CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<?>,
                                                               Map<String, PropertyDescriptor>>() {
                @Override
                public Map<String, PropertyDescriptor> load(Class<?> clazz) throws Exception {
                    BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                    Builder<String, PropertyDescriptor> mapBuilder =
                            ImmutableMap.<String, PropertyDescriptor>builder();
                    for(PropertyDescriptor desc: beanInfo.getPropertyDescriptors()) {
                        mapBuilder.put(desc.getName(), desc);
                    }

                    return mapBuilder.build();
                }
            });

    private ThreadExecutorStatsHelper() {
    }

    /**
     * Creates a new instance of the given class and populates it with stats data from the given
     * {@ling ExecutorService}. This method uses reflection to create the instance and introspection
     * to find and set the appropriate property methods. It is assumed the given class was generated
     * from the <code>thread-executor-stats</code> yang grouping.
     *
     * @param executor the ExecutorService
     * @param statsClass the stats class
     * @return an instance of <code>statsClass</code>
     * @throws IllegalArgumentException if an error occurs creating or populating the instance
     */
    public static <T> T newStatsInstance(ExecutorService executor, Class<T> statsClass)
            throws IllegalArgumentException {

        try {
            T stats = statsClass.newInstance();
            if(!(executor instanceof ThreadPoolExecutor)) {
                return stats;
            }

            ThreadPoolExecutor tpExecutor = (ThreadPoolExecutor)executor;
            Long queueSize = Long.valueOf(tpExecutor.getQueue().size());
            Long maxQueueSize = Long.valueOf(tpExecutor.getQueue().remainingCapacity()
                                                                          + queueSize.longValue());

            Map<String, PropertyDescriptor> propDescMap = propDescCache.get(statsClass);
            setProperty("activeThreadCount", stats,
                    Long.valueOf(tpExecutor.getActiveCount()), propDescMap);
            setProperty("completedTaskCount", stats,
                    Long.valueOf(tpExecutor.getCompletedTaskCount()), propDescMap);
            setProperty("currentQueueSize", stats, queueSize, propDescMap);
            setProperty("currentThreadPoolSize", stats,
                    Long.valueOf(tpExecutor.getPoolSize()), propDescMap);
            setProperty("largestThreadPoolSize", stats,
                    Long.valueOf(tpExecutor.getLargestPoolSize()), propDescMap);
            setProperty("maxQueueSize", stats, maxQueueSize, propDescMap);
            setProperty("maxThreadPoolSize", stats,
                    Long.valueOf(tpExecutor.getMaximumPoolSize()), propDescMap);
            setProperty("totalTaskCount", stats,
                    Long.valueOf(tpExecutor.getTaskCount()), propDescMap);

            return stats;
        } catch(IllegalArgumentException e) {
            throw e;
        } catch(Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error creating instance of %s", statsClass), e);
        }
    }

    private static void setProperty(String propName, Object obj, Object value,
                                    Map<String, PropertyDescriptor> propDescMap )
              throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        PropertyDescriptor propDesc = propDescMap.get(propName);
        if(propDesc == null) {
            throw new IllegalArgumentException(
                    String.format("No property for %s on class ", propName, obj.getClass()));
        }

        Method writeMethod = propDesc.getWriteMethod();
        if(writeMethod == null) {
            throw new IllegalArgumentException(
                    String.format("No write method for property for %s on class ",
                            propName, obj.getClass()));
        }

        writeMethod.invoke(obj, value);
    }
}
