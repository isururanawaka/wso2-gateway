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


import com.lmax.disruptor.RingBuffer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.log4j.Logger;
import org.wso2.carbon.gateway.internal.common.CarbonMessage;
import org.wso2.carbon.gateway.internal.common.CarbonMessageProcessor;
import org.wso2.carbon.gateway.internal.common.Pipe;
import org.wso2.carbon.gateway.internal.transport.common.Constants;
import org.wso2.carbon.gateway.internal.transport.common.HTTPContentChunk;
import org.wso2.carbon.gateway.internal.transport.common.HttpRoute;
import org.wso2.carbon.gateway.internal.transport.common.PipeImpl;
import org.wso2.carbon.gateway.internal.transport.common.Util;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorConfig;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorFactory;
import org.wso2.carbon.gateway.internal.transport.sender.TargetChanel;
import org.wso2.carbon.gateway.internal.transport.sender.TargetHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * A Class responsible for handle  incoming message through netty inbound pipeline.
 */
public class SourceHandler extends ChannelInboundHandlerAdapter {
    private static Logger log = Logger.getLogger(SourceHandler.class);

    private RingBuffer disruptor;
    private ChannelHandlerContext ctx;
    private CarbonMessage cMsg;
    private Map<String, TargetChanel> channelFutureMap = new HashMap<>();
    private TargetHandler targetHandler;
    private int queueSize;
    private DisruptorConfig disruptorConfig;
    private CarbonMessageProcessor carbonMessageProcessor;
    private ResponseCallback responseCallback;

    public SourceHandler(CarbonMessageProcessor carbonMessageProcessor, int queueSize) throws Exception {
        this.queueSize = queueSize;
        this.carbonMessageProcessor = carbonMessageProcessor;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        disruptorConfig = DisruptorFactory.getDisruptorConfig(DisruptorFactory.DisruptorType.INBOUND);
        disruptor = disruptorConfig.getDisruptor();
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            cMsg = new CarbonMessage(Constants.PROTOCOL_NAME);
            cMsg.setPort(((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
            cMsg.setHost(((InetSocketAddress) ctx.channel().remoteAddress()).getHostName());
            responseCallback = new ResponseCallback(this.ctx);
            cMsg.setCarbonCallback(responseCallback);
            HttpRequest httpRequest = (HttpRequest) msg;
            cMsg.setURI(httpRequest.getUri());
            Pipe pipe = new PipeImpl(queueSize);
            cMsg.setPipe(pipe);

            cMsg.setProperty(Constants.CHNL_HNDLR_CTX, this.ctx);
            cMsg.setProperty(Constants.SRC_HNDLR, this);
            cMsg.setProperty(Constants.HTTP_VERSION, httpRequest.getProtocolVersion().text());
            cMsg.setProperty(Constants.HTTP_METHOD, httpRequest.getMethod().name());
            cMsg.setProperty(Constants.TRANSPORT_HEADERS, Util.getHeaders(httpRequest));

            if (disruptorConfig.isShared()) {
                cMsg.setProperty(Constants.DISRUPTOR, disruptor);
            }
            //disruptor.publishEvent(new CarbonEventPublisher(cMsg));
        } else {
            HTTPContentChunk chunk;
            if (cMsg != null) {
                if (msg instanceof HttpContent) {
                    HttpContent httpContent = (HttpContent) msg;
                    chunk = new HTTPContentChunk(httpContent);
                    cMsg.getPipe().addContentChunk(chunk);
                    if (msg instanceof LastHttpContent) {
                        carbonMessageProcessor.receive(cMsg, responseCallback);
                    }
                }
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
    }

    public void addChannelFuture(HttpRoute route, TargetChanel targetChanel) {
        channelFutureMap.put(route.toString(), targetChanel);
    }

    public void removeChannelFuture(HttpRoute route) {
        log.debug("Removing channel future from map");
        channelFutureMap.remove(route.toString());
    }

    public TargetChanel getChannelFuture(HttpRoute route) {
        return channelFutureMap.get(route.toString());
    }

    public TargetHandler getTargetHandler() {
        return targetHandler;
    }

    public void setTargetHandler(TargetHandler targetHandler) {
        this.targetHandler = targetHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("Exception caught in Netty Source handler", cause);
    }
}



