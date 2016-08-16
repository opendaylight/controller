/**
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans;

import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.opendaylight.yangtools.concepts.Builder;

/**
 * Xtend extension method for &gt;&gt; operator support for {@link Builder}s.
 *
 * <p><pre>import static extension org.opendaylight.controller.md.sal.binding.test.xtendbeans
 * .XtendBuilderExtensions.operator_doubleGreaterThan</pre>
 *
 * <p>allows to write (in an *.xtend, not *.java):
 *
 * <pre>new InterfaceBuilder &gt;&gt; [
 *          name = "hello, world"
 *      ]</pre>
 *
 * <p>instead of:
 *
 * <pre>(new InterfaceBuilder =&gt; [
 *          name = "hello, world"
 *      ]).build</pre>
 *
 * <p>See also org.eclipse.xtext.xbase.lib.ObjectExtensions.operator_doubleArrow for background.
 *
 * @author Michael Vorburger
 */
public class XtendBuilderExtensions {

    public static <P extends Object, T extends Builder<P>> P operator_doubleGreaterThan(
            final T object, final Procedure1<? super T> block) {

        block.apply(object);
        return object.build();
    }

}
