/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands;

import org.opendaylight.yangtools.yang.common.QName;

public class CommandInvocationException extends Exception {

    public CommandInvocationException(final QName qName, final Throwable cause) {
        this("Command " + qName + " invocation failed: " + cause.getMessage(), cause);
    }

    protected CommandInvocationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static class CommandTimeoutException extends CommandInvocationException {

        public CommandTimeoutException(final QName qName, final Throwable e) {
            super("Command " + qName + " timed out: " + e.getMessage(), e);
        }
    }
}
