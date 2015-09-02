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
package org.wso2.carbon.gateway.internal.common;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a CarbonMessage.
 */
public abstract class CarbonMessage {
    public static final int IN = 0;
    public static final int OUT = 1;

    public abstract UUID getId();

    public abstract void setId(UUID id);

    public abstract int getDirection();

    public abstract void setDirection(int direction);

    public abstract Pipe getPipe();

    public abstract void setPipe(Pipe pipe);

    public abstract String getHost();

    public abstract void setHost(String host);

    public abstract int getPort();

    public abstract void setPort(int port);

    public abstract String getURI();

    public abstract void setURI(String to);

    public abstract String getReplyTo();

    public abstract void setReplyTo(String replyTo);

    public abstract String getProtocol();

    public abstract void setProtocol(String protocol);

    public abstract Object getProperty(String key);

    public abstract void setProperty(String key, Object value);

    public abstract Map<String, Object> getProperties();

    public abstract void setProperties(Map<String, Object> properties);
}
