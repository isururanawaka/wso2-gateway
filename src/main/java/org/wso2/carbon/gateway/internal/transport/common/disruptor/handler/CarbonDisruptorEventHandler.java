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

package org.wso2.carbon.gateway.internal.transport.common.disruptor.handler;

import org.wso2.carbon.gateway.internal.common.CarbonCallback;
import org.wso2.carbon.gateway.internal.common.CarbonMessage;
import org.wso2.carbon.gateway.internal.common.CarbonMessageProcessor;
import org.wso2.carbon.gateway.internal.transport.common.Constants;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.config.DisruptorFactory;
import org.wso2.carbon.gateway.internal.transport.common.disruptor.event.CarbonDisruptorEvent;

/**
 * TODO class level comment.
 */
public class CarbonDisruptorEventHandler extends DisruptorEventHandler {

    private int eventHandlerid;

    public CarbonDisruptorEventHandler(int eventHandlerid) {
        this.eventHandlerid = eventHandlerid;
    }

    @Override
    public void onEvent(CarbonDisruptorEvent carbonDisruptorEvent, long l, boolean b) throws Exception {
        CarbonMessage carbonMessage = (CarbonMessage) carbonDisruptorEvent.getEvent();
        int messageID = carbonDisruptorEvent.getEventId();
        if (carbonMessage.getDirection() == CarbonMessage.IN &&
                canProcess(DisruptorFactory.getDisruptorConfig(Constants.LISTENER).getNoOfEventHandlersPerDisruptor(),
                        eventHandlerid, messageID)) {
            CarbonMessageProcessor engine = (CarbonMessageProcessor) carbonMessage.getProperty(Constants.ENGINE);
            CarbonCallback carbonCallback = (CarbonCallback) carbonMessage.getProperty(Constants.RESPONSE_CALLBACK);
            engine.receive(carbonMessage, carbonCallback);
        } else if (carbonMessage.getDirection() == CarbonMessage.OUT &&
                DisruptorFactory.getDisruptorConfig(Constants.SENDER) != null &&
                canProcess(DisruptorFactory.getDisruptorConfig(Constants.SENDER).getNoOfEventHandlersPerDisruptor(),
                        eventHandlerid, messageID)) {

            CarbonCallback carbonCallback = (CarbonCallback) carbonMessage.getProperty(Constants.RESPONSE_CALLBACK);
            carbonCallback.done(carbonMessage);

        } else if (carbonMessage.getDirection() == CarbonMessage.OUT &&
                DisruptorFactory.getDisruptorConfig(Constants.SENDER) == null && canProcess(
                DisruptorFactory.getDisruptorConfig(Constants.LISTENER).getNoOfEventHandlersPerDisruptor(),
                eventHandlerid, messageID)) {
            CarbonCallback carbonCallback = (CarbonCallback) carbonMessage.getProperty(Constants.RESPONSE_CALLBACK);
            carbonCallback.done(carbonMessage);

        }

    }

}

