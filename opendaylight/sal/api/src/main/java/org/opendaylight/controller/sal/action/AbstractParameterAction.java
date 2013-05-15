package org.opendaylight.controller.sal.action;

public abstract class AbstractParameterAction<T> extends Action {

    private final T value;

    public AbstractParameterAction(T value) {
        if (value == null)
            throw new IllegalArgumentException("value should not be null.");
        if (checkValue(value)) {
            this.value = value;
        } else
            throw new IllegalArgumentException("Supplied value is invalid.");
    }

    public final T getValue() {
        return value;
    }

    protected boolean checkValue(T value) throws IllegalArgumentException {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        AbstractParameterAction<?> other = (AbstractParameterAction<?>) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }


}
