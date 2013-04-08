/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

public final class BaseConstraints {

    private BaseConstraints() {
    }

    public static LengthConstraint lengthConstraint(final Number min,
            final Number max, final String description, final String reference) {
        return new LengthConstraintImpl(min, max, description, reference);
    }

    public static RangeConstraint rangeConstraint(final Number min,
            final Number max, final String description, final String reference) {
        return new RangeConstraintImpl(min, max, description, reference);
    }

    public static PatternConstraint patternConstraint(final String pattern,
            final String description, final String reference) {
        return new PatternConstraintImpl(pattern, description, reference);
    }

    private static final class LengthConstraintImpl implements LengthConstraint {

        private final Number min;
        private final Number max;

        private final String description;
        private final String reference;

        private final String errorAppTag;
        private final String errorMessage;

        public LengthConstraintImpl(Number min, Number max,
                final String description, final String reference) {
            super();
            this.min = min;
            this.max = max;
            this.description = description;
            this.reference = reference;

            this.errorAppTag = "length-out-of-specified-bounds";
            this.errorMessage = "The argument is out of bounds <" + min + ", "
                    + max + ">";
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getErrorAppTag() {
            return errorAppTag;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public Number getMin() {
            return min;
        }

        @Override
        public Number getMax() {
            return max;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((errorAppTag == null) ? 0 : errorAppTag.hashCode());
            result = prime * result
                    + ((errorMessage == null) ? 0 : errorMessage.hashCode());
            result = prime * result + ((max == null) ? 0 : max.hashCode());
            result = prime * result + ((min == null) ? 0 : min.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LengthConstraintImpl other = (LengthConstraintImpl) obj;
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (errorAppTag == null) {
                if (other.errorAppTag != null) {
                    return false;
                }
            } else if (!errorAppTag.equals(other.errorAppTag)) {
                return false;
            }
            if (errorMessage == null) {
                if (other.errorMessage != null) {
                    return false;
                }
            } else if (!errorMessage.equals(other.errorMessage)) {
                return false;
            }
            if (max != other.max) {
                return false;
            }
            if (min != other.min) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LengthConstraintImpl [min=");
            builder.append(min);
            builder.append(", max=");
            builder.append(max);
            builder.append(", description=");
            builder.append(description);
            builder.append(", errorAppTag=");
            builder.append(errorAppTag);
            builder.append(", reference=");
            builder.append(reference);
            builder.append(", errorMessage=");
            builder.append(errorMessage);
            builder.append("]");
            return builder.toString();
        }
    }

    private final static class RangeConstraintImpl implements RangeConstraint {
        private final Number min;
        private final Number max;

        private final String description;
        private final String reference;

        private final String errorAppTag;
        private final String errorMessage;

        public RangeConstraintImpl(Number min, Number max, String description,
                String reference) {
            super();
            this.min = min;
            this.max = max;
            this.description = description;
            this.reference = reference;

            this.errorAppTag = "range-out-of-specified-bounds";
            this.errorMessage = "The argument is out of bounds <" + min + ", "
                    + max + ">";
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getErrorAppTag() {
            return errorAppTag;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public Number getMin() {
            return min;
        }

        @Override
        public Number getMax() {
            return max;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((errorAppTag == null) ? 0 : errorAppTag.hashCode());
            result = prime * result
                    + ((errorMessage == null) ? 0 : errorMessage.hashCode());
            result = prime * result + ((max == null) ? 0 : max.hashCode());
            result = prime * result + ((min == null) ? 0 : min.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RangeConstraintImpl other = (RangeConstraintImpl) obj;
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (errorAppTag == null) {
                if (other.errorAppTag != null) {
                    return false;
                }
            } else if (!errorAppTag.equals(other.errorAppTag)) {
                return false;
            }
            if (errorMessage == null) {
                if (other.errorMessage != null) {
                    return false;
                }
            } else if (!errorMessage.equals(other.errorMessage)) {
                return false;
            }
            if (max == null) {
                if (other.max != null) {
                    return false;
                }
            } else if (!max.equals(other.max)) {
                return false;
            }
            if (min == null) {
                if (other.min != null) {
                    return false;
                }
            } else if (!min.equals(other.min)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("RangeConstraintImpl [min=");
            builder.append(min);
            builder.append(", max=");
            builder.append(max);
            builder.append(", description=");
            builder.append(description);
            builder.append(", reference=");
            builder.append(reference);
            builder.append(", errorAppTag=");
            builder.append(errorAppTag);
            builder.append(", errorMessage=");
            builder.append(errorMessage);
            builder.append("]");
            return builder.toString();
        }
    }

    private final static class PatternConstraintImpl implements
            PatternConstraint {

        private final String regex;
        private final String description;
        private final String reference;

        private final String errorAppTag;
        private final String errorMessage;

        public PatternConstraintImpl(final String regex,
                final String description, final String reference) {
            super();
            this.regex = regex;
            this.description = description;
            this.reference = reference;

            errorAppTag = "invalid-regular-expression";
            // TODO: add erro message
            errorMessage = "";
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getErrorAppTag() {
            return errorAppTag;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public String getRegularExpression() {
            return regex;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((errorAppTag == null) ? 0 : errorAppTag.hashCode());
            result = prime * result
                    + ((errorMessage == null) ? 0 : errorMessage.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result + ((regex == null) ? 0 : regex.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PatternConstraintImpl other = (PatternConstraintImpl) obj;
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (errorAppTag == null) {
                if (other.errorAppTag != null) {
                    return false;
                }
            } else if (!errorAppTag.equals(other.errorAppTag)) {
                return false;
            }
            if (errorMessage == null) {
                if (other.errorMessage != null) {
                    return false;
                }
            } else if (!errorMessage.equals(other.errorMessage)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (regex == null) {
                if (other.regex != null) {
                    return false;
                }
            } else if (!regex.equals(other.regex)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("PatternConstraintImpl [regex=");
            builder.append(regex);
            builder.append(", description=");
            builder.append(description);
            builder.append(", reference=");
            builder.append(reference);
            builder.append(", errorAppTag=");
            builder.append(errorAppTag);
            builder.append(", errorMessage=");
            builder.append(errorMessage);
            builder.append("]");
            return builder.toString();
        }
    }
}
