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
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * {@link XtendBeanGenerator} customized for YANG beans stored in MD SAL
 * DataBroker.
 *
 * This is required: (a) because YANG model DataObject beans (when read from a
 * DataBroker) are funky java.lang.reflect.Proxy instances, and XtendBeanGenerator
 * cannot find the Builder or the property getters for them without a bit of help,
 * which this class provides;
 *
 * (b) to integrate it with the {@link XtendBuilderExtensions}
 * (for ">>" instead of "->" and no build() method calls);
 *
 * (c) to integrate it with the {@link AugmentableExtension}.
 *
 * @see XtendBeanGenerator
 *
 * @author Michael Vorburger
 */
// package-local: no need to expose this, consider it an implementation detail; public API is the AssertDataObjects
class XtendYangBeanGenerator extends XtendBeanGenerator {

    private boolean useBuilderExtensions(Object bean) {
        return bean instanceof DataObject;
    }

    @Override
    public String getExpression(Object bean) {
        final String beanText = super.getExpression(bean);
        if (useBuilderExtensions(bean)) {
            return new StringBuilder("import static extension ").append(XtendBuilderExtensions.class.getName())
                    .append(".operator_doubleGreaterThan\n\n").append(beanText).toString();
        } else {
            return beanText;
        }
    }

    @Override
    protected boolean isUsingBuilder(Object bean, Class<?> builderClass) {
        if (useBuilderExtensions(bean)) {
            return false;
        } else {
            return super.isUsingBuilder(bean, builderClass);
        }
    }

    @Override
    protected String getOperator(Object bean, Class<?> builderClass) {
        if (useBuilderExtensions(bean)) {
            return ">>";
        } else {
            return super.getOperator(bean, builderClass);
        }
    }

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
