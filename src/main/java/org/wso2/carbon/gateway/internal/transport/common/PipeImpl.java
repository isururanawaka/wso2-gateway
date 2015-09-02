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

import org.apache.log4j.Logger;
import org.wso2.carbon.gateway.internal.common.ContentChunk;
import org.wso2.carbon.gateway.internal.common.Pipe;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO class level comment.
 */
public class PipeImpl implements Pipe {
    private static Logger log = Logger.getLogger(PipeImpl.class);
    private String name = "Buffer";
    private int queueSize;
    private BlockingQueue<ContentChunk> contentQueue;
    private Map trailingheaders = new ConcurrentHashMap<String, String>();
    private AtomicBoolean isReadComplete = new AtomicBoolean(false);
    private AtomicBoolean isWriteComplete = new AtomicBoolean(false);

    public PipeImpl(String name, int blockingQueueSize) {
        this.name = name;
        this.queueSize = blockingQueueSize;
        this.contentQueue = new LinkedBlockingQueue<ContentChunk>(queueSize);
    }

    public ContentChunk getContent() {
        try {
            return contentQueue.take();
        } catch (InterruptedException e) {
            log.error("Error while retrieving chunk from queue.", e);
            return null;
        }
    }

    public void addContentChunk(ContentChunk contentChunk) {
        if (contentChunk.isLastChunk()) {
            isReadComplete.getAndSet(true);
        }
        contentQueue.add(contentChunk);
    }

    public boolean isWriteComplete() {
        return isWriteComplete.get();
    }

    public boolean isReadComplete() {
        return isReadComplete.get();
    }

    public void addTrailingHeader(String key, String value) {
        trailingheaders.put(key, value);
    }

    public Map getTrailingheaderMap() {
        return trailingheaders;
    }
}
