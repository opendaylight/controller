/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.util.BaseConstraints;

/**
 * Holder object for holding YANG type constraints.
 */
public final class TypeConstraints {
    private final List<List<RangeConstraint>> ranges = new ArrayList<List<RangeConstraint>>();
    private final List<List<LengthConstraint>> lengths = new ArrayList<List<LengthConstraint>>();
    private final List<PatternConstraint> patterns = new ArrayList<PatternConstraint>();
    private Integer fractionDigits;

    List<List<RangeConstraint>> getAllRanges() {
        return ranges;
    }

    public List<RangeConstraint> getRange() {
        List<RangeConstraint> resolved = ranges.get(0);
        RangeConstraint firstRange = resolved.get(0);
        RangeConstraint lastRange = resolved.get(resolved.size() - 1);
        Number min = firstRange.getMin();
        Number max = lastRange.getMax();

        if (!(min instanceof UnknownBoundaryNumber)
                && !(max instanceof UnknownBoundaryNumber)) {
            return resolved;
        }

        if (firstRange == lastRange) {
            if (min instanceof UnknownBoundaryNumber) {
                min = resolveMinRange(min);
            }
            if (max instanceof UnknownBoundaryNumber) {
                max = resolveMaxRange(max);
            }
            firstRange = BaseConstraints.rangeConstraint(min, max,
                    firstRange.getDescription(), firstRange.getReference());
            resolved.set(0, firstRange);
            lastRange = BaseConstraints.rangeConstraint(min, max,
                    lastRange.getDescription(), lastRange.getReference());
            resolved.set(resolved.size() - 1, lastRange);
        } else {
            if (min instanceof UnknownBoundaryNumber) {
                min = resolveMinRange(min);
                firstRange = BaseConstraints.rangeConstraint(min,
                        firstRange.getMax(), firstRange.getDescription(),
                        firstRange.getReference());
                resolved.set(0, firstRange);
            }
            if (max instanceof UnknownBoundaryNumber) {
                max = resolveMaxRange(max);
                lastRange = BaseConstraints.rangeConstraint(lastRange.getMin(),
                        max, lastRange.getDescription(),
                        lastRange.getReference());
                resolved.set(resolved.size() - 1, lastRange);
            }
        }
        return resolved;
    }

    private Number resolveMinRange(Number min) {
        int i = 1;
        while (min instanceof UnknownBoundaryNumber) {
            List<RangeConstraint> act = ranges.get(i);
            min = act.get(0).getMin();
            i++;
        }
        return min;
    }

    private Number resolveMaxRange(Number max) {
        int i = 1;
        while (max instanceof UnknownBoundaryNumber) {
            List<RangeConstraint> act = ranges.get(i);
            max = act.get(act.size() - 1).getMax();
            i++;
        }
        return max;
    }

    public void addRanges(List<RangeConstraint> ranges) {
        if (ranges != null && ranges.size() > 0) {
            this.ranges.add(ranges);
        }
    }

    public List<List<LengthConstraint>> getAllLengths() {
        return lengths;
    }

    public List<LengthConstraint> getLength() {
        List<LengthConstraint> resolved = lengths.get(0);
        LengthConstraint firstLength = resolved.get(0);
        LengthConstraint lastLength = resolved.get(resolved.size() - 1);
        Number min = firstLength.getMin();
        Number max = lastLength.getMax();

        if (!(min instanceof UnknownBoundaryNumber)
                && !(max instanceof UnknownBoundaryNumber)) {
            return resolved;
        }

        if (firstLength == lastLength) {
            if (min instanceof UnknownBoundaryNumber) {
                min = resolveMinLength(min);
            }
            if (max instanceof UnknownBoundaryNumber) {
                max = resolveMaxLength(max);
            }
            firstLength = BaseConstraints.lengthConstraint(min, max,
                    firstLength.getDescription(), firstLength.getReference());
            resolved.set(0, firstLength);
            lastLength = BaseConstraints.lengthConstraint(min, max,
                    lastLength.getDescription(), lastLength.getReference());
            resolved.set(resolved.size() - 1, lastLength);
        } else {
            if (min instanceof UnknownBoundaryNumber) {
                min = resolveMinLength(min);
                firstLength = BaseConstraints.lengthConstraint(min,
                        firstLength.getMax(), firstLength.getDescription(),
                        firstLength.getReference());
                resolved.set(0, firstLength);
            }
            if (max instanceof UnknownBoundaryNumber) {
                max = resolveMaxLength(max);
                lastLength = BaseConstraints.lengthConstraint(
                        lastLength.getMin(), max, lastLength.getDescription(),
                        lastLength.getReference());
                resolved.set(resolved.size() - 1, lastLength);
            }
        }
        return resolved;
    }

    private Number resolveMinLength(Number min) {
        int i = 1;
        while (min instanceof UnknownBoundaryNumber) {
            List<LengthConstraint> act = lengths.get(i);
            min = act.get(0).getMin();
            i++;
        }
        return min;
    }

    private Number resolveMaxLength(Number max) {
        int i = 1;
        while (max instanceof UnknownBoundaryNumber) {
            List<LengthConstraint> act = lengths.get(i);
            max = act.get(act.size() - 1).getMax();
            i++;
        }
        return max;
    }

    public void addLengths(List<LengthConstraint> lengths) {
        if (lengths != null && lengths.size() > 0) {
            this.lengths.add(lengths);
        }
    }

    public List<PatternConstraint> getPatterns() {
        return patterns;
    }

    public void addPatterns(List<PatternConstraint> patterns) {
        this.patterns.addAll(patterns);
    }

    public Integer getFractionDigits() {
        return fractionDigits;
    }

    public void setFractionDigits(Integer fractionDigits) {
        if (fractionDigits != null) {
            this.fractionDigits = fractionDigits;
        }
    }

}
