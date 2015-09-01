/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.gateway.internal;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.gateway.internal.common.TransportSender;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationComponent;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationEngine;
import org.wso2.carbon.gateway.internal.transport.common.Constants;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorConfig;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorFactory;
import org.wso2.carbon.gateway.internal.transport.listener.GateWayNettyInitializer;
import org.wso2.carbon.gateway.internal.transport.sender.NettySender;
import org.wso2.carbon.transport.http.netty.listener.CarbonNettyServerInitializer;

import java.util.Properties;


public class GatewayActivator implements BundleActivator {
    public void start(BundleContext bundleContext) throws Exception {
        Properties props = new Properties();
        String waitstrategy = props.getProperty("wait_strategy", Constants.PHASED_BACKOFF);
        DisruptorFactory.WAITSTRATEGY waitstrategy1 = null;
        if (waitstrategy.equals(Constants.BLOCKING_WAIT)) {
            waitstrategy1 = DisruptorFactory.WAITSTRATEGY.BLOCKING_WAIT;
        } else if (waitstrategy.equals(Constants.BUSY_SPIN)) {
            waitstrategy1 = DisruptorFactory.WAITSTRATEGY.BUSY_SPIN;
        } else if (waitstrategy.equals(Constants.TIME_BLOCKING)) {
            waitstrategy1 = DisruptorFactory.WAITSTRATEGY.TIME_BLOCKING;
        } else if (waitstrategy.equals(Constants.PHASED_BACKOFF)) {
            waitstrategy1 = DisruptorFactory.WAITSTRATEGY.PHASED_BACKOFF;
        } else if (waitstrategy.equals(Constants.LITE_BLOCKING)) {
            waitstrategy1 = DisruptorFactory.WAITSTRATEGY.LITE_BLOCKING;
        }
        DisruptorConfig disruptorConfig = new DisruptorConfig(Integer.valueOf(props.getProperty("disruptor_buffer_Size", "1024")),
                                                              Integer.valueOf(props.getProperty("no_of_disurptors", "1")),
                                                              Integer.valueOf(props.getProperty("no_of_eventHandlers_per_disruptor", "1")), waitstrategy1, true);
        DisruptorFactory.createDisruptors(Constants.LISTENER, disruptorConfig);

        NettySender.Config config = new NettySender.Config("netty-gw-sender").setQueueSize(Integer.parseInt(props.getProperty("queue_size", "32544")));
        TransportSender sender = new NettySender(config);
        //  Engine engine = new POCMediationEngine(sender);

        CamelContext context = new DefaultCamelContext();
        context.disableJMX();
        CamelMediationEngine engine = new CamelMediationEngine(sender);
        context.addComponent("wso2-gw", new CamelMediationComponent(engine));

        try {
            context.addRoutes(new RouteBuilder() {
                public void configure() {

                    from("wso2-gw:http://204.13.85.2:9090/service").choice().when(header("routeId").regex("r1"))
                               .to("wso2-gw:http://204.13.85.5:5050/services/echo")
                               .when(header("routeId").regex("r2"))
                               .to("wso2-gw:http://204.13.85.5:6060/services/echo")
                               .otherwise()
                               .to("wso2-gw:http://204.13.85.5:7070/services/echo");

                    from("wso2-gw:http://localhost:9090/service").choice().when(header("routeId").regex("r1"))
                               .to("wso2-gw:http://localhost:8080/services/echo")
                               .when(header("routeId").regex("r2"))
                               .to("wso2-gw:http://localhost:6060/services/echo")
                               .otherwise()
                               .to("wso2-gw:http://localhost:7070/services/echo");

                }

            });
            context.start();

            while (true) {
                Thread.sleep(100000);
            }
        } catch (Exception e) {
            //  LOG.error("Error Starting camel context ... ", e);
        }
        GateWayNettyInitializer gateWayNettyInitializer = new GateWayNettyInitializer(engine, config.getQueueSize());
        bundleContext.registerService(CarbonNettyServerInitializer.class, gateWayNettyInitializer, null);

    }

    public void stop(BundleContext bundleContext) throws Exception {

    }
}
