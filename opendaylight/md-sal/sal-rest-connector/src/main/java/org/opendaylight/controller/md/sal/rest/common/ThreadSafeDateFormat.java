/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 * Thread safe DateFormater with default Date pattern format yyyy-MM-dd
 * for formating QName revision.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Apr 23, 2015
 */
public class ThreadSafeDateFormat {

    public Date parse(final String dateString) throws ParseException {
        return dateFormat.get().parse(dateString);
    }

    public String format(final Date date) {
        return dateFormat.get().format(date);
    }

    private final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {

        @Override
        public DateFormat get() {
            return super.get();
        }

        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }

        @Override
        public void remove() {
            super.remove();
        }

        @Override
        public void set(final DateFormat value) {
            super.set(value);
        }
    };
}
