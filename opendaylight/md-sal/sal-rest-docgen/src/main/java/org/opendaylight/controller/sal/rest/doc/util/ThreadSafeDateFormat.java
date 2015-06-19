/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.doc.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * sal-rest-docgen
 * org.opendaylight.controller.sal.rest.doc.util
 *
 * Thread safe DateFormater with default Date pattern format yyyy-MM-dd for formating QName revision.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Jun 19, 2015
 */
public class ThreadSafeDateFormat {

    /**
     * Parses text from the beginning of the given string to produce a date. The method may not use the entire text of
     * the given string.
     *
     * @param dateString
     * @return date
     * @throws ParseException
     */
    public Date parse(final String dateString) throws ParseException {
        return dateFormat.get().parse(dateString);
    }

    /**
     * Formats a Date into a date/time string in "yyyy-MM-dd" format.
     * @param date
     * @return dateString
     */
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
