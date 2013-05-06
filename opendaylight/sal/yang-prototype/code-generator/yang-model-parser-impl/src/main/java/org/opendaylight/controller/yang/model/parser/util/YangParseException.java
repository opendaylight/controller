/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/eplv10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

public class YangParseException extends RuntimeException {

    private static final long serialVersionUID = 1239548963471793178L;

    public YangParseException(final String errorMsg) {
        super(errorMsg);
    }

    public YangParseException(final String errorMsg, final Exception exception) {
        super(errorMsg, exception);
    }

    public YangParseException(final int line, final String errorMsg) {
        super("Error on line " + line + ": " + errorMsg);
    }

    public YangParseException(final int line, final String errorMsg,
            final Exception exception) {
        super("Error on line " + line + ": " + errorMsg, exception);
    }

    public YangParseException(final String moduleName, final int line,
            final String errorMsg) {
        super("Error in module '" + moduleName + "' on line " + line + ": "
                + errorMsg);
    }

    public YangParseException(final String moduleName, final int line,
            final String errorMsg, final Exception exception) {
        super("Error in module '" + moduleName + "' on line " + line + ": "
                + errorMsg, exception);
    }

}
