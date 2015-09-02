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

import org.apache.camel.builder.RouteBuilder;

/**
 * A class which creates a Route Builder
 */
public class GateWayRouteBuilder extends RouteBuilder {


    @Override
    public void configure() throws Exception {
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
}
