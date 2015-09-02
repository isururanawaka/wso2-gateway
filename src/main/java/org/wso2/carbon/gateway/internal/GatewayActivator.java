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

package org.wso2.carbon.gateway.internal;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.internal.common.TransportSender;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationComponent;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationEngine;
import org.wso2.carbon.gateway.internal.transport.common.Constants;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorConfig;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorFactory;
import org.wso2.carbon.gateway.internal.transport.listener.GateWayNettyInitializer;
import org.wso2.carbon.gateway.internal.transport.sender.NettySender;
import org.wso2.carbon.transport.http.netty.listener.CarbonNettyServerInitializer;

import java.util.Hashtable;
import java.util.Properties;

/**
 * OSGi Bundle Activator of the gateway Carbon component.
 */
public class GatewayActivator implements BundleActivator {
    private static final String CHANNEL_ID_KEY = "channel.id";
    private static Logger log = LoggerFactory.getLogger(TransportSender.class);

    public void start(BundleContext bundleContext) throws Exception {
        Properties props = new Properties();
        String waitstrategy = props.getProperty("wait_strategy", Constants.PHASED_BACKOFF);

        DisruptorConfig disruptorConfig =
                new DisruptorConfig(Integer.parseInt(props.getProperty("disruptor_buffer_Size", "1024")),
                        Integer.parseInt(props.getProperty("no_of_disurptors", "1")),
                        Integer.parseInt(props.getProperty("no_of_eventHandlers_per_disruptor", "1")),
                        waitstrategy,
                        true);
        DisruptorFactory.createDisruptors(Constants.INBOUND, disruptorConfig);


        NettySender.Config config =
                new NettySender.Config("netty-gw-sender").
                        setQueueSize(Integer.parseInt(props.getProperty("queue_size", "32544")));
        TransportSender sender = new NettySender(config);
        //  Engine engine = new POCMediationEngine(sender);

        CamelContext context = new DefaultCamelContext();
        context.disableJMX();
        CamelMediationEngine engine = new CamelMediationEngine(sender);
        context.addComponent("wso2-gw", new CamelMediationComponent(engine));

        try {
            context.addRoutes(new GateWayRouteBuilder());
            context.start();
            Hashtable<String, String> httpInitParams = new Hashtable<>();
            httpInitParams.put(CHANNEL_ID_KEY, "netty-gw");
            GateWayNettyInitializer gateWayNettyInitializer =
                    new GateWayNettyInitializer(engine, config.getQueueSize());
            bundleContext.registerService(CarbonNettyServerInitializer.class, gateWayNettyInitializer, httpInitParams);

        } catch (Exception e) {
            log.error("Cannot start Gateway Activator", e);
        }

    }

    public void stop(BundleContext bundleContext) throws Exception {

    }

}

