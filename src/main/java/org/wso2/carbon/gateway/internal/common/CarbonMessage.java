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

public abstract class CarbonMessage {
    public final static int IN = 0;
    public final static int OUT = 1;

    abstract public UUID getId();

    abstract public void setId(UUID id);

    abstract public int getDirection();

    abstract public void setDirection(int direction);

    abstract public Pipe getPipe();

    abstract public void setPipe(Pipe pipe);

    abstract public String getHost();

    abstract public void setHost(String host);

    abstract public int getPort();

    abstract public void setPort(int port);

    abstract public String getURI();

    abstract public void setURI(String to);

    abstract public String getReplyTo();

    abstract public void setReplyTo(String replyTo);

    abstract public String getProtocol();

    abstract public void setProtocol(String protocol);

    abstract public Object getProperty(String key);

    abstract public void setProperty(String key, Object value);

    abstract public Map<String, Object> getProperties();

    abstract public void setProperties(Map<String, Object> properties);
}
