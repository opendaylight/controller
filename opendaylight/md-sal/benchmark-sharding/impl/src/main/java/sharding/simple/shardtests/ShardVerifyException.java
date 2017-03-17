/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

/**
 * Exception used when producer verification fails.
 * @author jmedved
 *
 */
public class ShardVerifyException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Include a message.
     * @param message: Exception message
     */
    public ShardVerifyException(String message) {
        super(message);
    }

    /**
     * Include a cause.
     * @param cause: Exception cause
     */
    public ShardVerifyException(Throwable cause) {
        super(cause);
    }

    /**
     * Include both message and a cause.
     * @param message: Exception message
     * @param cause: Exception cause
     */
    public ShardVerifyException(String message, Throwable cause) {
        super(message, cause);
    }

}
