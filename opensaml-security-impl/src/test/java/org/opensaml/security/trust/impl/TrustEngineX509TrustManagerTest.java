/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.security.trust.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.cryptacular.util.KeyPairUtil;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.ssl.SslConfig;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.unboundid.ldap.sdk.LDAPException;

/**
 * Test of {@link TrustEngineX509TrustManager} implementation.
 */
public class TrustEngineX509TrustManagerTest {

    private final static String DATA_PATH = "src/test/resources/org/opensaml/security/ldap/impl/";
    
    private InMemoryDirectory directoryServer;

    /** LDAP DN to test. */
    private final String context = "ou=people,dc=example,dc=org";

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found in test resources.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     * @throws IOException 
     */
    @BeforeTest public void setupDirectoryServer() throws IOException, LDAPException {
        directoryServer = new InMemoryDirectory(new File(DATA_PATH + "test-ldap.ldif"), new File(DATA_PATH + "test-ldap.keystore"));
        directoryServer.start();
    }

    /**
     * Shutdown the in-memory directory server.
     */
    @AfterTest public void teardownDirectoryServer() {
        directoryServer.stop();
    }

    /** Make sure default trust fails. */
    @Test(expectedExceptions=LdapException.class)
    public void testDefaultTrust() throws LdapException {
        final ConnectionConfig config = new ConnectionConfig();
        config.setLdapUrl("ldap://localhost:10389");
        config.setUseStartTLS(true);
        final DefaultConnectionFactory factory = new DefaultConnectionFactory(config);
        final Connection conn = factory.getConnection();
        try {
            conn.open();
        } finally {
            conn.close();
        }
    }
    
    /** No trust engine. */
    @Test(expectedExceptions=LdapException.class)
    public void testNullTrust() throws LdapException {
        final TrustEngineX509TrustManager trustManager = new TrustEngineX509TrustManager();
        final SslConfig sslConfig = new SslConfig();
        sslConfig.setTrustManagers(trustManager);
        final ConnectionConfig config = new ConnectionConfig();
        config.setLdapUrl("ldap://localhost:10389");
        config.setUseStartTLS(true);
        config.setSslConfig(sslConfig);
        final DefaultConnectionFactory factory = new DefaultConnectionFactory(config);
        final Connection conn = factory.getConnection();
        try {
            conn.open();
        } finally {
            conn.close();
        }
    }
    
    /** Static trust engine. */
    @Test
    public void testStaticTrust() throws LdapException, FileNotFoundException, IOException {
        final StaticCredentialResolver resolver;
        try (final FileInputStream is = new FileInputStream(new File(DATA_PATH + "test-ldap.key"))) {
            resolver = new StaticCredentialResolver(new BasicCredential(KeyPairUtil.readPublicKey(is)));
        }
        final TrustEngineX509TrustManager trustManager = new TrustEngineX509TrustManager();
        trustManager.setTLSTrustEngine(new ExplicitKeyTrustEngine(resolver));
        final SslConfig sslConfig = new SslConfig();
        sslConfig.setTrustManagers(trustManager);
        final ConnectionConfig config = new ConnectionConfig();
        config.setLdapUrl("ldap://localhost:10389");
        config.setUseStartTLS(true);
        config.setSslConfig(sslConfig);
        final DefaultConnectionFactory factory = new DefaultConnectionFactory(config);
        final Connection conn = factory.getConnection();
        try {
            conn.open();
            doSearch(conn);
        } finally {
            conn.close();
        }
    }

    protected void doSearch(@Nonnull final Connection conn) throws LdapException {
        final SearchOperation search = new SearchOperation(conn);
        final Response<SearchResult> result =
                search.execute(SearchRequest.newObjectScopeSearchRequest(context, new String[] {"description"}));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResultCode(), ResultCode.SUCCESS);
    }
    
}