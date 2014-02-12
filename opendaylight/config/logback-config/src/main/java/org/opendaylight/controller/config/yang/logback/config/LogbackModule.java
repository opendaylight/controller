/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import nu.xom.Element;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.yang.logback.api.HasAppenders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkCondition;
import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkNotNull;


public final class LogbackModule extends org.opendaylight.controller.config.yang.logback.config.AbstractLogbackModule {
    private final DependencyResolver dependencyResolver;

    public LogbackModule(org.opendaylight.controller.config.api.ModuleIdentifier name,
                         org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(name, dependencyResolver);
        this.dependencyResolver = dependencyResolver;
    }

    public LogbackModule(org.opendaylight.controller.config.api.ModuleIdentifier name,
                         org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                         org.opendaylight.controller.config.yang.logback.config.AbstractLogbackModule oldModule,
                         java.lang.AutoCloseable oldInstance) {
        super(name, dependencyResolver, oldModule, oldInstance);
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    protected void customValidation() {

        dependencyResolver.validateDependencies(HasAppendersServiceInterface.class);
        Set<String> notFoundAppenders = LogbackModuleFactory.findAppendersNotPresentInTransaction(dependencyResolver);
        checkCondition(notFoundAppenders.isEmpty(), "Some appenders in logback API are not present in transaction:" +
                notFoundAppenders, loggersJmxAttribute);

        validateLoggersObjects();

        // TODO validate name clashes
    }

    private void validateLoggersObjects() {

        checkNotNull(getLoggerTO(), loggersJmxAttribute);

        for (LoggerTO loggerToValidate : getLoggerTO()) {
            checkNotNull(loggerToValidate.getLoggerName(), "LoggerName is null",
                    loggersJmxAttribute);
            checkNotNull(loggerToValidate.getLevel(), "Level is null",
                    loggersJmxAttribute);
            checkCondition(!loggerToValidate.getLoggerName().isEmpty(),
                    "LoggerName needs to be set", loggersJmxAttribute);
            checkCondition(!loggerToValidate.getLevel().isEmpty(),
                    "Level needs to be set", loggersJmxAttribute);


        }
    }

    @Override
    public boolean isSame(AbstractLogbackModule other) {
        return false; // short circuit for now, TODO detect changes in appenders
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Map<ModuleIdentifier, HasAppenders> moduleIdentifierHasAppendersMap = dependencyResolver.resolveInstances(
                HasAppendersServiceInterface.class, HasAppenders.class);
        List<Element> appenderElements = new ArrayList<>();
        for (HasAppenders hasAppenders : moduleIdentifierHasAppendersMap.values()) {
            appenderElements.addAll(hasAppenders.getXmlRepresentationOfAppenders().values());
        }
        return new LogbackReconfigurator(appenderElements, getLoggerTO(), new LogbackStatusListener(getRootRuntimeBeanRegistratorWrapper()));
    }
}
