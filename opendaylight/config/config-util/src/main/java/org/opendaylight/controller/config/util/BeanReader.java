package org.opendaylight.controller.config.util;

import javax.management.ObjectName;

public interface BeanReader {
    Object getAttributeCurrentValue(ObjectName on, String attributeName);
}
