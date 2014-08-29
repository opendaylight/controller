/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.md.sal.xpath;

/**
 * Defines an exception is thrown from the sal-xpath engine when a runtime error occurs.
 * @author Devin Avery
 *
 */
public class SalXPathRuntimeException extends RuntimeException {

    public SalXPathRuntimeException() {
        super();
    }

    public SalXPathRuntimeException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SalXPathRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SalXPathRuntimeException(String message) {
        super(message);
    }

    public SalXPathRuntimeException(Throwable cause) {
        super(cause);
    }


}
