/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.util;

import static org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil.getRevisionFormat;

import com.google.common.base.Preconditions;
import java.text.ParseException;
import java.util.Date;

public final class Util {

    public static String writeDate(final Date date) {
        return getRevisionFormat().format(date);
    }

    public static Date readDate(final String s) throws ParseException {
        return getRevisionFormat().parse(s);
    }

    public static void checkType(final Object value, final Class<?> clazz) {
        Preconditions.checkArgument(clazz.isAssignableFrom(value.getClass()), "Unexpected type " + value.getClass()
                + " should be " + clazz + " of " + value);
    }

}
