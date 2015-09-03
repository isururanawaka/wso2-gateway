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
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.log4j.Logger;
import org.wso2.carbon.gateway.internal.GateWayRouteBuilder;
import org.wso2.carbon.gateway.internal.common.CarbonMessageProcessor;
import org.wso2.carbon.gateway.internal.common.TransportSender;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationComponent;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationEngine;
import org.wso2.carbon.gateway.internal.transport.common.Constants;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorConfig;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorFactory;
import org.wso2.carbon.gateway.internal.transport.sender.NettySender;
import org.wso2.carbon.transport.http.netty.listener.CarbonNettyServerInitializer;

import java.util.Map;

/**
 * A class that responsible for create server side channels.
 */
public class GatewayNettyInitializer implements CarbonNettyServerInitializer {

    private static final Logger log = Logger.getLogger(GatewayNettyInitializer.class);
    private final Object lock = new Object();
    private CarbonMessageProcessor engine;
    private int channelNumber;
    private int queueSize = 32544;

    public GatewayNettyInitializer() {

    }

    @Override
    public void setup(Map<String, String> parameters) {
        if (parameters != null) {
            DisruptorConfig disruptorConfig =
                       new DisruptorConfig(parameters.get(Constants.DISRUPTOR_BUFFER_SIZE),
                                           parameters.get(Constants.DISRUPTOR_COUNT),
                                           parameters.get(Constants.DISRUPTOR_EVENT_HANDLER_COUNT),
                                           parameters.get(Constants.WAIT_STRATEGY),
                                           Boolean.parseBoolean(Constants.SHARE_DISRUPTOR_WITH_OUTBOUND));
            DisruptorFactory.createDisruptors(Constants.INBOUND, disruptorConfig);
            String queueSize = parameters.get(Constants.CONTENT_QUEUE_SIZE);
            if (queueSize != null) {
                this.queueSize = Integer.parseInt(queueSize);
            }
        } else {
            log.warn("Disruptor specific parameters are not specified in configuration hence using default configs");
            DisruptorConfig disruptorConfig = new DisruptorConfig();
            DisruptorFactory.createDisruptors(Constants.INBOUND, disruptorConfig);
        }

        NettySender.Config config = new NettySender.Config("netty-gw-sender").setQueueSize(this.queueSize);
        TransportSender sender = new NettySender(config);
        CamelContext context = new DefaultCamelContext();
        context.disableJMX();
        CamelMediationEngine engine = new CamelMediationEngine(sender);
        context.addComponent("wso2-gw", new CamelMediationComponent(engine));
        this.engine = engine;
        try {
            context.addRoutes(new GateWayRouteBuilder());
            context.start();
        } catch (Exception e) {
            log.error("Cannot start Camel Context", e);
        }
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
            channelNumber++;
            try {
                p.addLast("handler", new SourceHandler(engine, channelNumber, queueSize));
            } catch (Exception e) {
                log.error("Cannot Create SourceHandler ", e);
            }
        }
    }

}
