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
package org.wso2.carbon.gateway.internal.transport.listener.ssl;

import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;

/**
 * A class that encapsulates SSL Certificate Information.
 */
public class SSLHandlerFactory {

    private static final String protocol = "TLS";
    private final SSLContext serverContext;
    private boolean needClientAuth;

    public SSLHandlerFactory(SSLConfig sslConfig) {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }
        try {
            KeyStore ks = getKeyStore(sslConfig.getKeyStore(), sslConfig.getKeyStorePass());
            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, sslConfig.getCertPass() != null ? sslConfig.getCertPass().toCharArray()
                    : sslConfig.getKeyStorePass().toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();
            TrustManager[] trustManagers = null;
            if (sslConfig.getTrustStore() != null) {
                this.needClientAuth = true;
                KeyStore tks = getKeyStore(sslConfig.getTrustStore(), sslConfig.getTrustStorePass());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(tks);
                trustManagers = tmf.getTrustManagers();
            }
            serverContext = SSLContext.getInstance(protocol);
            serverContext.init(keyManagers, trustManagers, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to initialize the server-side SSLContext", e);
        }
    }

    private static KeyStore getKeyStore(File keyStore, String keyStorePassword) throws IOException {
        KeyStore ks;
        try (InputStream is = new FileInputStream(keyStore)) {
            ks = KeyStore.getInstance("JKS");
            ks.load(is, keyStorePassword.toCharArray());
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw ((RuntimeException) ex);
            }
            throw new IOException(ex);
        }
        return ks;
    }

    /**
     * @return instance of {@code SslHandler}
     */
    public SslHandler create() {
        SSLEngine engine = serverContext.createSSLEngine();
        engine.setNeedClientAuth(needClientAuth);
        engine.setUseClientMode(false);
        return new SslHandler(engine);
    }
}
