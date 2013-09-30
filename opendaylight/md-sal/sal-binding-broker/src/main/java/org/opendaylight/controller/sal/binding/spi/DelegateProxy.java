package org.opendaylight.controller.sal.binding.spi;

public interface DelegateProxy<T> {
    
    void setDelegate(T delegate);
    T getDelegate();
}
