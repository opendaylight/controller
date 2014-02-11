package org.opendaylight.controller.config.yang.logback.appender;

import ch.qos.logback.core.FileAppender;
import nu.xom.Element;
import nu.xom.Text;
import org.opendaylight.controller.config.yang.logback.api.HasAppenders;
import org.opendaylight.controller.config.yang.logback.api.HasAppendersImpl;

import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkCondition;
import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkNotNull;

public final class FileAppenderModule extends org.opendaylight.controller.config.yang.logback.appender.AbstractFileAppenderModule {

    public FileAppenderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FileAppenderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                              FileAppenderModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        checkNotNull(getFileAppenderTO(), fileAppenderTOJmxAttribute);
        for (FileAppenderTO to : getFileAppenderTO()) {
            checkCondition(to.getName() != null && to.getName().isEmpty() == false, "Name is not set",
                    fileAppenderTOJmxAttribute);
            checkNotNull(to.getEncoderPattern(), "Encoder pattern is not set", fileAppenderTOJmxAttribute);
            checkNotNull(to.getAppend(), "Append is null", fileAppenderTOJmxAttribute);
            checkCondition(to.getFile() != null && to.getFile().isEmpty() == false, "File needs to be set", fileAppenderTOJmxAttribute);
        }
    }

    @Override
    public HasAppenders createInstance() {

        return new HasAppendersImpl<FileAppenderTO>(getFileAppenderTO()) {
            @Override
            protected Element getElement(FileAppenderTO appenderTO) {
                Element appenderElement = fillElement(FileAppender.class, appenderTO.getName(), appenderTO.getEncoderPattern(), appenderTO.getThresholdFilter());
                // append
                Element append = new Element("append");
                appenderElement.appendChild(append);
                append.appendChild(new Text(Boolean.toString(appenderTO.getAppend())));
                // file
                Element file = new Element("file");
                appenderElement.appendChild(file);
                file.appendChild(new Text(appenderTO.getFile()));
                return appenderElement;

            }
        };
    }
}
