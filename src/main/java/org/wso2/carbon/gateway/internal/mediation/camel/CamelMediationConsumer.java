/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.wso2.carbon.gateway.internal.mediation.camel;

import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.util.ObjectHelper;

/**
 * The CamelMediation consumer. Client incoming messages will
 */
public class CamelMediationConsumer extends DefaultConsumer {
    private final CamelMediationEngine engine;

    public CamelMediationConsumer(CamelMediationEndpoint endpoint, Processor processor, CamelMediationEngine engine) {
        super(endpoint, processor);
        this.engine = engine;
    }

    @Override protected void doStop() throws Exception {
        super.doStop();
        engine.removeConsumer(getEndpoint().getEndpointKey());
    }

    @Override protected void doStart() throws Exception {
        super.doStart();
        String endPointUrlOnly = ObjectHelper.after(getEndpoint().getEndpointKey(), "://");
        engine.addConsumer(endPointUrlOnly, this);
    }

    @Override public CamelMediationEndpoint getEndpoint() {
        return (CamelMediationEndpoint) super.getEndpoint();
    }
/*
    private Map<String, Object> getCamelHeaders(CarbonMessage msg, Exchange exchange) {

        Map<String, Object> headers = new HashMap<String, Object>();

        //put Carbon message parameters as camel headers
        headers.put(ConversionParameters.MESSAGE_ID, msg.getId());
        headers.put(ConversionParameters.DIRECTION, msg.getDirection());
        headers.put(ConversionParameters.PROTOCOL, msg.getProtocol());
        headers.put(ConversionParameters.HOST, msg.getHost());
        headers.put(ConversionParameters.PORT, msg.getPort());
        headers.put(ConversionParameters.TO, msg.getURI());
        headers.put(ConversionParameters.REPLY_TO, msg.getReplyTo());
        headers.put(ConversionParameters.PIPE, msg.getPipe());
        headers.put(ConversionParameters.PROPERTIES, msg.getProperties());

        //populate headers
        headers.put(Exchange.HTTP_METHOD, msg.getProperty(msg.getProtocol(), Constants.HTTP_METHOD));

        // strip query parameters from the uri
        String reqUrl = msg.getURI();
        String reqUri = reqUrl;
        if (reqUrl.contains("?")) {
            reqUrl = ObjectHelper.before(reqUrl, "?");
        }

        // we want the full path for the url, as the client may provide the url in the HTTP headers as absolute or relative, eg
        //   /foo
        //   http://servername/foo

        String urlPrefix = msg.getProtocol() + "://";
        if (!reqUrl.startsWith("http")) {
            reqUri = reqUrl;
            reqUrl = urlPrefix + POCController.props.getProperty("ip", "localhost") + ":" +
                     POCController.props.getProperty("port") + reqUrl;
        } else {
            int index = reqUri.indexOf('/');
            index = reqUri.indexOf('/', index + 2);
            reqUri = reqUri.substring(index);
        }
        headers.put(Exchange.HTTP_URL, reqUrl);

        //URI is without the ip and port
        URI uri = null;
        try {
            uri = new URI(reqUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        headers.put(Exchange.HTTP_URI, uri.getPath());
        headers.put(Exchange.HTTP_QUERY, uri.getQuery());
        headers.put(Exchange.HTTP_RAW_QUERY, uri.getRawQuery());

        //HTTP_PATH vs HTTP_URI ?
        headers.put(Exchange.HTTP_PATH, uri.getPath());

        if (log.isDebugEnabled()) {
            log.info("HTTP_URI :" + uri.getPath());
            log.info("HTTP_QUERY :" + uri.getQuery());
        }

        Map<String, Map<String, Object>> properties = msg.getProperties();
        Map<String, Object> carbonMsgHeaders =
                (Map<String, Object>) msg.getProperty(msg.getProtocol(), Constants.TRANSPORT_HEADERS);

        //System.out.println("size : "+carbonMsgHeaders.size());

        for (Map.Entry<String, Object> entry : carbonMsgHeaders.entrySet()) {
            //System.out.println(entry.getKey()+" : "+entry.getValue());
            headers.put(entry.getKey(), entry.getValue());
        }

        // add uri parameters as headers to the Camel message
        if (reqUri.contains("?")) {
            String query = ObjectHelper.after(reqUri, "?");
            Map<String, Object> uriParameters = null;
            try {
                uriParameters = URISupport.parseQuery(query, false, true);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            for (Map.Entry<String, Object> entry : uriParameters.entrySet()) {
                String name = entry.getKey();
                Object values = entry.getValue();
                *//*
                Iterator<?> it = ObjectHelper.createIterator(values);
                while (it.hasNext()) {
                    Object extracted = it.next();
                    Object decoded = shouldUrlDecodeHeader(configuration, name, extracted, "UTF-8");
                    LOG.trace("URI-Parameter: {}", extracted);
                    if (headerFilterStrategy != null
                        && !headerFilterStrategy.applyFilterToExternalHeaders(name, decoded, exchange)) {
                        NettyHttpHelper.appendHeader(headers, name, decoded);
                    }
                }
                *//*
                headers.put(name, values);
            }
        }
*//*
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
*//*
        return headers;
    }
    */
}
