/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.gateway.internal.transport.listener;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.apache.log4j.Logger;
import org.wso2.carbon.gateway.internal.common.CarbonMessageProcessor;
import org.wso2.carbon.transport.http.netty.listener.CarbonNettyServerInitializer;

/**
 * A class that responsible for create server side channels.
 */
public class GateWayNettyInitializer implements CarbonNettyServerInitializer {

    private static final Logger log = Logger.getLogger(GateWayNettyInitializer.class);
    private final Object lock = new Object();
    private CarbonMessageProcessor engine;
    private int noOfChannels;
    private int queueSize;


    public GateWayNettyInitializer(CarbonMessageProcessor engine, int queueSize) {
        this.engine = engine;
        this.queueSize = queueSize;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        if (log.isDebugEnabled()) {
            log.info("Initializing source channel pipeline");
        }
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        synchronized (lock) {
            noOfChannels++;
            try {
                p.addLast("handler", new SourceHandler(engine, noOfChannels, queueSize));
            } catch (Exception e) {
                log.error("Cannot Create SourceHandler ", e);
            }
        }
    }

}
