package org.opendaylight.controller.sal.binding.dom.serializer.api;

import java.util.Map.Entry;

import org.opendaylight.yangtools.yang.common.QName;

public class ValueWithQName<V> implements Entry<QName, V>{
    
    final QName qname;
    final V value;
    
    public ValueWithQName(QName qname, V value) {
        super();
        this.qname = qname;
        this.value = value;
    }

    public QName getQname() {
        return qname;
    }

    public V getValue() {
        return value;
    }
    
    @Override
    public QName getKey() {
        return qname;
    }
    
    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((qname == null) ? 0 : qname.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        ValueWithQName other = (ValueWithQName) obj;
        if (qname == null) {
            if (other.qname != null)
                return false;
        } else if (!qname.equals(other.qname))
            return false;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
