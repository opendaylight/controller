/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorContext;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import scala.Option;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class MyActor extends UntypedActor {

    public MyActor() {
    }

    @Override
    public void akka$actor$Actor$_setter_$context_$eq(ActorContext actorContext) {

    }

    @Override
    public ActorContext context() {
        return null;
    }

    @Override
    public void aroundReceive(PartialFunction<Object, BoxedUnit> partialFunction, Object o) {

    }

    @Override
    public void aroundPreStart() {

    }

    @Override
    public void aroundPostStop() {

    }

    @Override
    public void aroundPreRestart(Throwable throwable, Option<Object> option) {

    }

    @Override
    public void aroundPostRestart(Throwable throwable) {

    }

    @Override
    public void onReceive(Object o) throws Throwable {

    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return null;
    }

    @Override
    public void preStart() throws Exception {

    }

    @Override
    public void postStop() throws Exception {

    }

    @Override
    public void preRestart(Throwable throwable, Option<Object> option) throws Exception {

    }

    @Override
    public void postRestart(Throwable throwable) throws Exception {

    }

    @Override
    public void unhandled(Object o) {

    }
}
