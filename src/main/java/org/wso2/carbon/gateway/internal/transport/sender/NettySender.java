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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.internal.common.CarbonCallback;
import org.wso2.carbon.gateway.internal.common.CarbonMessage;
import org.wso2.carbon.gateway.internal.common.Pipe;
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
 * TODO class level comment.
 */
public class NettySender extends TransportSender {
    private static final Logger LOG = LoggerFactory.getLogger(NettySender.class);

    private Config config;

    private Object lock = new Object();

    private int channelCorrelator;


    public NettySender(Config conf) {
        super(Constants.PROTOCOL_NAME);
        this.config = conf;
    }

    public boolean init() {
        return true;
    }

    @Override
    public boolean send(CarbonMessage msg, CarbonCallback callback) {
        final ChannelHandlerContext inboundCtx = (ChannelHandlerContext)
                msg.getProperty(Constants.CHNL_HNDLR_CTX);
        final HttpRequest httpRequest = Util.createHttpRequest(msg);
        final Pipe pipe = msg.getPipe();
        final SourceHandler srcHandler = (SourceHandler) msg.getProperty(Constants.SRC_HNDLR);
        InetSocketAddress address = new InetSocketAddress(msg.getHost(), msg.getPort());
        final HttpRoute route = new HttpRoute(msg.getHost(), msg.getPort());
// TODO use src handler map (host port) and condition to use pool for throttling.
        if (srcHandler.getChannelFuture(route) == null) {
            synchronized (srcHandler.getLock()) {
                if (srcHandler.getChannelFuture(route) == null) {
                    synchronized (lock) {
                        channelCorrelator++;
                    }
                }
                RingBuffer ringBuffer = (RingBuffer) msg.getProperty(Constants.DISRUPTOR);
                if (ringBuffer == null) {
                    DisruptorConfig disruptorConfig = DisruptorFactory.getDisruptorConfig(Constants.SENDER);
                    ringBuffer = disruptorConfig.getDisruptor();
                }
                TargetInitializer targetInitializer =
                        new TargetInitializer(ringBuffer, channelCorrelator, config.getQueueSize());
                Bootstrap bootstrap = getNewBootstrap(inboundCtx, targetInitializer);
                ChannelFuture future = bootstrap.connect(address);
                final Channel outboundChannel = future.channel();
                addCloseListener(outboundChannel, srcHandler, route);
                TargetChanel targetChanel = new TargetChanel();
                srcHandler.addChannelFuture(route, targetChanel);
// putCallback(outboundChannel, callback);
// outboundChannel.attr(TargetHandler.callbackAttribute).set(callback);
                future.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            srcHandler.setTargetHandler(targetInitializer.getTargetHandler());
                            targetInitializer.getTargetHandler().setCallback(callback);
                            outboundChannel.write(httpRequest);
                            while (true) {
                                HTTPContentChunk chunk = (HTTPContentChunk) pipe.getContent();
                                HttpContent httpContent = chunk.getHttpContent();
                                if (httpContent instanceof LastHttpContent) {
                                    outboundChannel.writeAndFlush(httpContent);
                                    break;
                                }
                                if (httpContent != null) {
                                    outboundChannel.write(httpContent);
                                }
                            }
                            srcHandler.getChannelFuture(route).setChannelFuture(future).setChannelFutureReady(true);
// srcHandler.setChannel(outboundChannel);
                        } else {
// Close the connection if the connection attempt has failed.
                            outboundChannel.close();
                        }
                    }
                });
            }

        } else {
            while (!srcHandler.getChannelFuture(route).isChannelFutureReady()) {
//                try {
//                    Thread.currentThread().wait(1);
//                } catch (InterruptedException e) {
//                    log.error("Interuppted Exception",e);
//                }
            }
            TargetChanel targetChanel = srcHandler.getChannelFuture(route);
// putCallback(srcHandler.getChannel(), callback);
// srcHandler.getChannel().attr(TargetHandler.callbackAttribute).set(callback);
            srcHandler.getTargetHandler().setCallback(callback);
            if (targetChanel.getChannelFuture().isSuccess() && targetChanel.getChannelFuture().channel().isActive()) {
                targetChanel.getChannelFuture().channel().write(httpRequest);
                while (true) {
                    HTTPContentChunk chunk = (HTTPContentChunk) pipe.getContent();
                    HttpContent httpContent = chunk.getHttpContent();
                    if (httpContent instanceof LastHttpContent) {
                        targetChanel.getChannelFuture().channel().writeAndFlush(httpContent);
                        break;
                    }
                    targetChanel.getChannelFuture().channel().write(httpContent);
                }
            } else {
                // need to handle new connection
                LOG.error("Channel is closed");
            }
        }
        return false;
    }


    private void addCloseListener(Channel ch, final SourceHandler handler, final HttpRoute route) {
        ChannelFuture closeFuture = ch.closeFuture();
        closeFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                handler.removeChannelFuture(route);
            }
        });
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

    /**
     * TODO class level comment.
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
