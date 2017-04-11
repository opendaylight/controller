/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subchannel;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by HanJie on 2017/1/25.
 *
 * @author Han Jie
 */
public interface PostCallBack {

    PostCallBack NO_OP_CALLBACK = new PostCallBack() {
        @Override
        public void run() {
        }

        @Override
        public void success() {
        }

        @Override
        public void failure() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }
    };

    class Reference extends AtomicReference<PostCallBack> {
        private static final long serialVersionUID = 1L;

        public Reference(PostCallBack initialValue) {
            super(initialValue);
        }
    }

    void run();
    void pause();
    void resume();
    void success();
    void failure();

    public interface PostCallBackResult {
        short POST_SUCCESS = 0;
        short POST_FAILED = 1;
        short POST_FAILURE_REMOTE_UNABLE = 2;
    }
}
