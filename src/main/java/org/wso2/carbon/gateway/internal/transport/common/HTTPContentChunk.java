/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.gateway.internal.transport.common;

import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import org.wso2.carbon.gateway.internal.common.ContentChunk;

import java.nio.ByteBuffer;

public class HTTPContentChunk implements ContentChunk {
    private HttpContent httpContent;
    boolean lastChunk = false;
    public HTTPContentChunk(HttpContent content) {
        if (content instanceof LastHttpContent || content instanceof DefaultLastHttpContent) {
            lastChunk = true;
        }
        httpContent = content;
    }
    public ByteBuffer[] getContentChunk() {
        return httpContent.content().nioBuffers();
    }
    public HttpContent getHttpContent() {
        if (isLastChunk()) {
            return (LastHttpContent) httpContent;
        } else {
            return httpContent;
        }
    }
    public boolean isLastChunk() {
        if (lastChunk) {
            return true;
        }
        return false;
    }
}

