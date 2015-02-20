package org.opendaylight.controller.config.util;

import javax.management.ObjectName;

/**
 * Created by mmarsale on 20.2.2015.
 */
public interface BeanReader {
    Object getAttributeCurrentValue(ObjectName on, String attributeName);
}
