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

package org.wso2.carbon.gateway.internal.transport.common.disruptor.event;

import com.lmax.disruptor.EventFactory;

/**
 * TODO class level comment.
 */
public class CarbonDisruptorEvent {

    public static final EventFactory<CarbonDisruptorEvent> EVENT_FACTORY = new EventFactory<CarbonDisruptorEvent>() {
        public CarbonDisruptorEvent newInstance() {
            return new CarbonDisruptorEvent();
        }
    };
    private Object event;
    private int eventId;

    public CarbonDisruptorEvent() {
    }

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
}
