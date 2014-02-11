/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.logback.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class AppenderFactoryUtil {
    private static final Logger logger = LoggerFactory.getLogger(AppenderFactoryUtil.class);

    public static boolean isEncoderSupported(OutputStreamAppender outputStreamAppender) {
        return outputStreamAppender.getEncoder() != null && outputStreamAppender.getEncoder() instanceof PatternLayoutEncoderBase;
    }

    public static <TO> TO setName_EncoderPattern_ThresholdFilter(OutputStreamAppender outputStreamAppender, TO to) {
        AppenderFactoryUtil.setName(to, outputStreamAppender.getName());
        AppenderFactoryUtil.setEncoderPattern(to, ((PatternLayoutEncoderBase) outputStreamAppender.getEncoder()).getPattern());
        boolean thresholdSet = false;
        for (Filter filter : (List<Filter>) outputStreamAppender.getCopyOfAttachedFiltersList()) {
            if (filter instanceof ThresholdFilter && thresholdSet == false) {
                String safeLevel = AppenderFactoryUtil.getThresholdLevel(filter);
                AppenderFactoryUtil.setThresholdFilter(to, safeLevel);
                thresholdSet = true;
            } else {
                logger.warn("Filter in appender " + outputStreamAppender.getName() + " was not ignored: " + filter);
            }
        }
        return to;
    }

    // get Threshold level by reflection
    public static String getThresholdLevel(Filter filter) {
        // get level property
        Field levelField;
        Level defaultLevel = Level.ALL;
        try {
            levelField = filter.getClass().getDeclaredField("level");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Cannot find 'level' in " + filter, e);
        }
        levelField.setAccessible(true);
        Object levelValue;
        try {
            levelValue = levelField.get(filter);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot get value 'level' from " + filter, e);
        }
        String levelString = levelValue.toString();
        // sanity check converts null and undefined to default level
        return Level.toLevel(levelString, defaultLevel).toString();
    }


    // TO reflection utils, TODO generate an interface for TOs instead
    private static void setThresholdFilter(Object to, String value) {
        setSomeString(to, "setThresholdFilter", value);
    }

    private static void setEncoderPattern(Object to, String value) {
        setSomeString(to, "setEncoderPattern", value);
    }

    private static void setName(Object to, String value) {
        setSomeString(to, "setName", value);
    }

    private static void setSomeString(Object to, String methodName, String value) {
        Method setNameMethod;
        try {
            setNameMethod = to.getClass().getMethod(methodName, new Class<?>[]{String.class});
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot find method '" + methodName +
                    "' in " + to, e);
        }
        try {
            setNameMethod.invoke(to, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot call '" + methodName +
                    "' on " + to, e);
        }
    }
}