package org.opendaylight.controller.md.sal.common.api;

import java.util.EventListener;

import org.opendaylight.yangtools.concepts.Registration;

public interface RegistrationListener<T extends Registration<?>> extends EventListener {

    void onRegister(T registration);
    
    void onUnregister(T registration);
}
