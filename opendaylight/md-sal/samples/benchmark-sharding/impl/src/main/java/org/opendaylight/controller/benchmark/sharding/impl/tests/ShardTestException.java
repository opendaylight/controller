/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

/**
 * Thrown when something goes wrong with the ShardTest.
 *
 * @author jmedved
 */
public class ShardTestException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Include a message.
     *
     * @param message Exception message.
     */
    public ShardTestException(final String message) {
        super(message);
    }

    /**
     * Include only cause.
     *
     * @param cause Exception cause.
     */
    public ShardTestException(final Throwable cause) {
        super(cause);
    }

    /**
     * Include both message and a cause.
     *
     * @param message Exception message.
     * @param cause Exception cause.
     */
    public ShardTestException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
