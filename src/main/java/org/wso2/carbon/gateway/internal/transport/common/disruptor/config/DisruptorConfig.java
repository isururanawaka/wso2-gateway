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

package org.wso2.carbon.gateway.internal.transport.common.disruptor.config;

import com.lmax.disruptor.RingBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO class level comment.
 */
public class DisruptorConfig {

    private int bufferSize;
    private int noDisruptors;
    private int noOfEventHandlersPerDisruptor;
    private DisruptorFactory.WAITSTRATEGY waitstrategy;
    private boolean shared;
    private List<RingBuffer> disruptorMap = new ArrayList<RingBuffer>();
    private int index = 1;

    public DisruptorConfig(int bufferSize, int noDisruptors, int noOfEventHandlersPerDisruptor,
                           DisruptorFactory.WAITSTRATEGY waitstrategy, boolean shared) {
        this.bufferSize = bufferSize;
        this.noDisruptors = noDisruptors;
        this.noOfEventHandlersPerDisruptor = noOfEventHandlersPerDisruptor;
        this.waitstrategy = waitstrategy;
        this.shared = shared;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getNoDisruptors() {
        return noDisruptors;
    }

    public int getNoOfEventHandlersPerDisruptor() {
        return noOfEventHandlersPerDisruptor;
    }

    public DisruptorFactory.WAITSTRATEGY getWaitstrategy() {
        return waitstrategy;
    }

    public boolean isShared() {
        return shared;
    }

    public synchronized RingBuffer getDisruptor() {
        int ind = index % noDisruptors;
        index++;
        return disruptorMap.get(ind);
    }

    public void addDisruptor(RingBuffer ringBuffer) {
        disruptorMap.add(ringBuffer);
    }
}
