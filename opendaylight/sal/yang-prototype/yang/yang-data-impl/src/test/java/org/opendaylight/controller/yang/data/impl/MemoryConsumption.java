/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.yang.data.impl;

/**
 * Provides memory consumption and elapsed time between 2 points 
 * @author mirehak
 */
public class MemoryConsumption {
    
    private long memBegin;
    private long tsBegin;

    /**
     * record memory and timestamp
     */
    public void startObserving() {
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();
        memBegin = getActualMemoryConsumption();
        tsBegin = System.currentTimeMillis();
    }
    
    
    /**
     * @return memory usage and time elapsed message
     */
    public String finishObserving() {
        long memEnd = getActualMemoryConsumption();
        long tsEnd = System.currentTimeMillis();
        return String.format("Used memory: %10d B; Elapsed time: %5d ms", (memEnd - memBegin), (tsEnd - tsBegin));
    }
    
    
    /**
     * @return actual memory usage
     */
    public static long getActualMemoryConsumption() {
        Runtime runtime = Runtime.getRuntime();
        // Calculate the used memory
        long memory = runtime.totalMemory() - runtime.freeMemory();
        return memory;
    }
    
    
}
