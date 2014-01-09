/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.util;

import com.google.common.base.Preconditions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Util {

    /**
     * Used for date <-> xml serialization
     */
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static String writeDate(final Date date) {
        return dateFormat.format(date);
    }

    public static Date readDate(final String s) throws ParseException {
        return dateFormat.parse(s);
    }

    public static void checkType(final Object value, final Class<?> clazz) {
        Preconditions.checkArgument(clazz.isAssignableFrom(value.getClass()), "Unexpected type " + value.getClass()
                + " should be " + clazz + " of " + value);
    }

}
