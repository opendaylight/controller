
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   PacketResult.java
 *
 * @brief  Possible results for Data packet processing handler
 */
package org.opendaylight.controller.sal.packet;

/**
 * Possible results for Data packet processing handler
 *
 */
public enum PacketResult {
    /**
     * Packet has been processed and noone in the chain after us is
     * supposed to see it
     *
     */
    CONSUME,
    /**
     * Packet has been processed and still further processing is
     * possible
     *
     */
    KEEP_PROCESSING,
    /**
     * Packet has been ignored so further handler is present in
     * the sequence need to still look at it.
     *
     */
    IGNORED
}
