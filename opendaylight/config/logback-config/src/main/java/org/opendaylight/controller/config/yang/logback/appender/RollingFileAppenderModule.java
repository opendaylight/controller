package org.opendaylight.controller.config.yang.logback.appender;

import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Text;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.yang.logback.api.HasAppenders;
import org.opendaylight.controller.config.yang.logback.api.HasAppendersImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkCondition;
import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkNotNull;

public final class RollingFileAppenderModule extends org.opendaylight.controller.config.yang.logback.appender.AbstractRollingFileAppenderModule {

    public RollingFileAppenderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RollingFileAppenderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                     RollingFileAppenderModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        JmxAttributeValidationException.checkNotNull(getRollingFileAppenderTO(), rollingFileAppenderTOJmxAttribute);
        for (RollingFileAppenderTO to : getRollingFileAppenderTO()) {

            checkCondition(to.getName() != null && to.getName().isEmpty() == false, "Name is not set", rollingFileAppenderTOJmxAttribute);
            checkCondition(to.getFile() != null && to.getFile().isEmpty() == false, "File needs to be set", rollingFileAppenderTOJmxAttribute);
            checkNotNull(to.getEncoderPattern(), "Encoder pattern is not set", rollingFileAppenderTOJmxAttribute);
            // size based triggering policy is mandatory
            checkNotNull(to.getSizeBasedTriggeringPolicy(), "Size based triggering policy is mandatory", rollingFileAppenderTOJmxAttribute);

            // either time based or fixed window rolling policy must be set, only one of them though.
            checkCondition(to.getTimeBasedRollingPolicy() != null || to.getFixedWindowRollingPolicy() != null,
                    "One of time-based-rolling-policy, fixed-window-rolling-policy must be set", rollingFileAppenderTOJmxAttribute);

            if (to.getFixedWindowRollingPolicy() == null) {
                TimeBasedRollingPolicyTO rollingPolicy = to.getTimeBasedRollingPolicy();
                checkNotNull(rollingPolicy.getFileNamePattern(), "File name pattern is null", rollingFileAppenderTOJmxAttribute);
                checkNotNull(rollingPolicy.getMaxHistory(), "Max history is null", rollingFileAppenderTOJmxAttribute);
                checkNotNull(rollingPolicy.getCleanHistoryOnStart(), "Clean history on start is null", rollingFileAppenderTOJmxAttribute);
            } else if (to.getTimeBasedRollingPolicy() == null) {
                FixedWindowRollingPolicyTO rollingPolicy = to.getFixedWindowRollingPolicy();
                checkNotNull(rollingPolicy.getFileNamePattern(), "File name pattern is null", rollingFileAppenderTOJmxAttribute);
                checkNotNull(rollingPolicy.getMinIndex(), "Min index is null", rollingFileAppenderTOJmxAttribute);
                checkNotNull(rollingPolicy.getMaxIndex(), "Max index is null", rollingFileAppenderTOJmxAttribute);

            } else {
                throw new JmxAttributeValidationException(
                        rollingFileAppenderTOJmxAttribute.getAttributeName() + " " +
                                "Only one of time-based-rolling-policy, fixed-window-rolling-policy can be set",
                        rollingFileAppenderTOJmxAttribute);
            }
        }
    }


    @Override
    public HasAppenders createInstance() {

        return new HasAppendersImpl<RollingFileAppenderTO>(getRollingFileAppenderTO()) {
            @Override
            protected Element getElement(RollingFileAppenderTO appenderTO) {
                Element appenderElement = fillElement(RollingFileAppender.class, appenderTO.getName(), appenderTO.getEncoderPattern(), appenderTO.getThresholdFilter());

                // file
                Element file = new Element("file");
                appenderElement.appendChild(file);
                file.appendChild(new Text(appenderTO.getFile()));


                {   // add triggeringPolicy/maxFileSize
                    String sizeLimit = appenderTO.getSizeBasedTriggeringPolicy().getMaxFileSize();
                    Element triggeringPolicyElement = new Element("triggeringPolicy");
                    appenderElement.appendChild(triggeringPolicyElement);
                    triggeringPolicyElement.addAttribute(new Attribute("class", SizeBasedTriggeringPolicy.class.getCanonicalName()));
                    {
                        Element maxFileSizeElement = new Element("maxFileSize");
                        triggeringPolicyElement.appendChild(maxFileSizeElement);
                        {
                            maxFileSizeElement.appendChild(new Text(sizeLimit));
                        }
                    }
                }

                Element rollingPolicyElement = new Element("rollingPolicy");
                appenderElement.appendChild(rollingPolicyElement);
                Class<?> rollingPolicyClass;
                if (appenderTO.getFixedWindowRollingPolicy() != null) {
                    rollingPolicyClass = FixedWindowRollingPolicy.class;
                    FixedWindowRollingPolicyTO rollingPolicy = appenderTO.getFixedWindowRollingPolicy();
                    // fileNamePattern
                    addChildNodeCallingGetter(rollingPolicyElement, rollingPolicy, "fileNamePattern");
                    addChildNodeCallingGetter(rollingPolicyElement, rollingPolicy, "minIndex");
                    addChildNodeCallingGetter(rollingPolicyElement, rollingPolicy, "maxIndex");
                } else {
                    rollingPolicyClass = TimeBasedRollingPolicy.class;
                    TimeBasedRollingPolicyTO rollingPolicy = appenderTO.getTimeBasedRollingPolicy();
                    addChildNodeCallingGetter(rollingPolicyElement, rollingPolicy, "fileNamePattern");
                    addChildNodeCallingGetter(rollingPolicyElement, rollingPolicy, "maxHistory");
                    addChildNodeCallingGetter(rollingPolicyElement, rollingPolicy, "cleanHistoryOnStart");
                }
                rollingPolicyElement.addAttribute(new Attribute("class", rollingPolicyClass.getCanonicalName()));
                return appenderElement;

            }

            private void addChildNodeCallingGetter(Element parent, Object object, String attributeName) {
                String getterName = "get" + capitalize(attributeName);
                Method method;
                try {
                    method = object.getClass().getMethod(getterName);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("Cannot get method with name " + getterName + " from " + object.getClass(), e) ;
                }
                String value;
                try {
                    value = String.valueOf(method.invoke(object));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Cannot get value of " + getterName + " on " + object.getClass());
                }
                Element child = new Element(attributeName);
                parent.appendChild(child);
                child.appendChild(new Text(value));
            }
        };
    }
}
