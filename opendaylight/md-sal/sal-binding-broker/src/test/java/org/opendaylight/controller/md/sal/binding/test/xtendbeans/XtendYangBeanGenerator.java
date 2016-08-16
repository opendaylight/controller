/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * {@link XtendBeanGenerator} customized for YANG beans stored in MD SAL
 * DataBroker.
 *
 * This is required because YANG model DataObject beans (when read from a
 * DataBroker) are funky java.lang.reflect.Proxy instances, and
 * XtendBeanGenerator cannot find the Builder for them without a bit of help,
 * which this class provides.
 *
 * @see XtendBeanGenerator
 *
 * @author Michael Vorburger
 */
public class XtendYangBeanGenerator extends XtendBeanGenerator {

    @Override
    protected CharSequence getNewBeanExpression(Object bean) {
        if (bean instanceof DataContainer) {
            DataContainer dataContainerBean = (DataContainer) bean;
            Class<?> builderClass = getBuilderClassByAppendingBuilderToClassName(
                    dataContainerBean.getImplementedInterface());
            return super.getNewBeanExpression(dataContainerBean, builderClass);
        } else {
            return super.getNewBeanExpression(bean);
        }
    }

}
