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

package org.wso2.carbon.gateway.internal.transport.sender;

import com.lmax.disruptor.RingBuffer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import org.wso2.carbon.gateway.internal.common.CarbonCallback;
import org.wso2.carbon.gateway.internal.common.CarbonMessage;
import org.wso2.carbon.gateway.internal.common.TransportSender;
import org.wso2.carbon.gateway.internal.transport.common.Constants;
import org.wso2.carbon.gateway.internal.transport.common.HTTPContentChunk;
import org.wso2.carbon.gateway.internal.transport.common.HttpRoute;
import org.wso2.carbon.gateway.internal.transport.common.Util;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorConfig;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorFactory;
import org.wso2.carbon.gateway.internal.transport.listener.SourceHandler;
import org.wso2.carbon.transport.http.netty.listener.ssl.SSLConfig;

import java.net.InetSocketAddress;

/**
 * A class creates connections with BE and send messages.
 */
public class NettySender implements TransportSender {
    private Config config;


    public NettySender(Config conf) {
        this.config = conf;
    }


    @Override
    public boolean send(CarbonMessage msg, CarbonCallback callback) {

        final HttpRequest httpRequest = Util.createHttpRequest(msg);

        final HttpRoute route = new HttpRoute(msg.getHost(), msg.getPort());

        if (!isRouteExists(route, msg)) {

            createAndCacheNewConnection(msg, route, config.getQueueSize(), callback, httpRequest);

        } else {

            writeUsingExistingConnection(msg, httpRequest, route, callback);
        }
        return false;
    }


    private void addCloseListener(Channel ch, final SourceHandler handler, final HttpRoute route) {
        ChannelFuture closeFuture = ch.closeFuture();
        closeFuture.addListener(future -> handler.removeChannelFuture(route));
    }


    private Bootstrap getNewBootstrap(ChannelHandlerContext ctx, TargetInitializer targetInitializer) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                   .channel(ctx.channel().getClass())
                   .handler(targetInitializer);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000);
        bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);
        return bootstrap;
    }


    private boolean isRouteExists(HttpRoute httpRoute, CarbonMessage carbonMessage) {
        final SourceHandler srcHandler = (SourceHandler) carbonMessage.getProperty(Constants.SRC_HNDLR);
        return srcHandler.getChannelFuture(httpRoute) != null;
    }

    private void writeContent(Channel channel, HttpRequest httpRequest, CarbonMessage carbonMessage) {
        channel.write(httpRequest);
        while (true) {
            HTTPContentChunk chunk = (HTTPContentChunk) carbonMessage.getPipe().getContent();
            HttpContent httpContent = chunk.getHttpContent();
            if (httpContent instanceof LastHttpContent) {
                channel.writeAndFlush(httpContent);
                break;
            }
            if (httpContent != null) {
                channel.write(httpContent);
            }
        }

    }

    //create and cache new connections for BE and cache it in httproute map in channel for later use.
    private void createAndCacheNewConnection(CarbonMessage carbonMessage, HttpRoute route, int queueSize,
                                             CarbonCallback carbonCallback, HttpRequest httpRequest) {
        SourceHandler srcHandler = (SourceHandler) carbonMessage.getProperty(Constants.SRC_HNDLR);
        ChannelHandlerContext inboundCtx = (ChannelHandlerContext) carbonMessage.getProperty(Constants.CHNL_HNDLR_CTX);

        RingBuffer ringBuffer = (RingBuffer) carbonMessage.getProperty(Constants.DISRUPTOR);
        if (ringBuffer == null) {
            DisruptorConfig disruptorConfig = DisruptorFactory.
                       getDisruptorConfig(DisruptorFactory.DisruptorType.OUTBOUND);
            ringBuffer = disruptorConfig.getDisruptor();
        }
        TargetInitializer targetInitializer =
                   new TargetInitializer(ringBuffer, queueSize);
        Bootstrap bootstrap = getNewBootstrap(inboundCtx, targetInitializer);
        InetSocketAddress inetSocketAddress = new InetSocketAddress
                   (carbonMessage.getHost(), carbonMessage.getPort());
        ChannelFuture future = bootstrap.connect(inetSocketAddress);
        final Channel outboundChannel = future.channel();
        addCloseListener(outboundChannel, srcHandler, route);
        TargetChanel targetChanel = new TargetChanel();
        srcHandler.addChannelFuture(route, targetChanel);

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    srcHandler.setTargetHandler(targetInitializer.getTargetHandler());
                    targetInitializer.getTargetHandler().setCallback(carbonCallback);

                    writeContent(outboundChannel, httpRequest, carbonMessage);

                    srcHandler.getChannelFuture(route).setChannelFuture(future);

                } else {
                    outboundChannel.close();
                }
            }
        });

    }


    private void writeUsingExistingConnection(CarbonMessage carbonMessage, HttpRequest httpRequest, HttpRoute route,
                                              CarbonCallback carbonCallback) {
        SourceHandler srcHandler = (SourceHandler) carbonMessage.getProperty(Constants.SRC_HNDLR);

        TargetChanel targetChanel = srcHandler.getChannelFuture(route);

        srcHandler.getTargetHandler().setCallback(carbonCallback);

        if (targetChanel.getChannelFuture().isSuccess() && targetChanel.getChannelFuture().channel().isActive()) {

            writeContent(targetChanel.getChannelFuture().channel(), httpRequest, carbonMessage);

        } else {

            //handle closed connections
            srcHandler.removeChannelFuture(route);

            createAndCacheNewConnection(carbonMessage, route, config.getQueueSize(), carbonCallback, httpRequest);
        }
    }

    /**
     * Class representing configs related to Transport Sender.
     */
    public static class Config {

        private String id;

        private SSLConfig sslConfig;

        private int queueSize;


        public Config(String id) {
            if (id == null) {
                throw new IllegalArgumentException("Netty transport ID is null");
            }
            this.id = id;
        }

        public String getId() {
            return id;
        }


        public Config enableSsl(SSLConfig sslConfig) {
            this.sslConfig = sslConfig;
            return this;
        }

        public SSLConfig getSslConfig() {
            return sslConfig;
        }


        public int getQueueSize() {
            return queueSize;
        }

        public Config setQueueSize(int queuesize) {
            this.queueSize = queuesize;
            return this;
        }


    }

}
