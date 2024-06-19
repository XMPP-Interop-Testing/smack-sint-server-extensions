/**
 * Copyright 2024 Guus der Kinderen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.igniterealtime.smack.inttest.xep0363;

import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;
import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.xep0363.element.RetryError;
import org.igniterealtime.smack.inttest.xep0363.element.SlotRaw;
import org.igniterealtime.smack.inttest.xep0363.provider.RetryErrorProvider;
import org.igniterealtime.smack.inttest.xep0363.provider.SlotRawProvider;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.element.SlotRequest;

import javax.net.ssl.SSLSocketFactory;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for HTTP file Upload. This completes the integration test that's provided by Smack.
 *
 * @see {@link org.jivesoftware.smackx.httpfileupload.HttpFileUploadIntegrationTest}
 */
@SpecificationReference(document = "XEP-0363", version = "1.1.0")
public class HttpFileUploadExtIntegrationTest extends AbstractSmackIntegrationTest
{
    private final HttpFileUploadManager hfumOne;

    private final SSLSocketFactory tlsSocketFactory;

    public HttpFileUploadExtIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);
        hfumOne = HttpFileUploadManager.getInstanceFor(conOne);
        if (!hfumOne.discoverUploadService()) {
            throw new TestNotPossibleException("Unable to find any service on domain that supports XEP-0363: HTTP File Upload.");
        }

        if (environment.configuration.sslContextFactory != null) {
            tlsSocketFactory = environment.configuration.sslContextFactory.createSslContext().getSocketFactory();
        } else {
            tlsSocketFactory = null;
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with [..] a GET URL wrapped by a <slot> element.")
    public void testSlotResponseContainsGet() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "testfile-" + StringUtils.randomString(5) + ".txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertNotNull(response.getGetUrl(), "Expected the slot requested by '" + conOne + "' from '" + request.getTo() + "' to contain a GET URL (but it did not).");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with [...] a PUT [...] wrapped by a <slot> element.")
    public void testSlotResponseContainsPut() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "testfile-" + StringUtils.randomString(5) + ".txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertNotNull(response.getPutUrl(), "Expected the slot requested by '" + conOne + "' from '" + request.getTo() + "' to contain a PUT URL (but it did not).");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with both a PUT and a GET URL [...] The host MUST provide Transport Layer Security (RFC 5246).")
    public void testSlotResponseGetUrlProvideTLS() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "testfile-" + StringUtils.randomString(5) + ".txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertEquals("https", response.getGetUrl().substring(0, 5), "Expected the GET URL from the slot returned by '" + request.getTo() + "' to '" + conOne + "' to start with 'https' (but it did not).");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with both a PUT and a GET URL [...] The host MUST provide Transport Layer Security (RFC 5246).")
    public void testSlotResponsePutUrlProvideTLS() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "testfile-" + StringUtils.randomString(5) + ".txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertEquals("https", response.getPutUrl().substring(0, 5), "Expected the PUT URL from the slot returned by '" + request.getTo() + "' to '" + conOne + "' to start with 'https' (but it did not).");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with both a PUT and a GET URL [...] Both HTTPS URLs MUST adhere to RFC 3986")
    public void testSlotResponseGetUrlAdhereToRFC3986() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "testfile-" + StringUtils.randomString(5) + ".txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            try {
                new LazyUri(new Precoded(response.getGetUrl())).fragment().isPresent(); // call fragment().isPresent() to cause entire URI to be parsed.
            } catch (RuntimeException e) {
                fail("Expected the GET URL from the slot returned by '" + request.getTo() + "' to '" + conOne + "' to parse as a RFC3986 URI (but it did not).", e);
            }
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with both a PUT and a GET URL [...] Both HTTPS URLs MUST adhere to RFC 3986")
    public void testSlotResponsePutUrlAdhereToRFC3986() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "testfile-" + StringUtils.randomString(5) + ".txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            try {
                new LazyUri(new Precoded(response.getPutUrl())).fragment().isPresent(); // call fragment().isPresent() to cause entire URI to be parsed.
            } catch (RuntimeException e) {
                fail("Expected the PUT URL from the slot returned by '" + request.getTo() + "' to '" + conOne + "' to parse as a RFC3986 URI (but it did not).", e);
            }
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with both a PUT and a GET URL [...]. Non ASCII characters MUST be percent-encoded.")
    public void testSlotResponseGetUrlIsPercentEncoded() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "très cool.txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            final String rawGetUrl = response.getGetUrl();
            final String decodedGetUrl = URLDecoder.decode(rawGetUrl, StandardCharsets.UTF_8);
            // Note: we can't be sure that the service used the file name from the request in the response. We can only check for the absence of characters that need to be percent-encoded.
            assertTrue(StandardCharsets.US_ASCII.newEncoder().canEncode(rawGetUrl), "Expected the GET URL from the slot returned by '" + request.getTo() + "' to '" + conOne + "' to contain only ASCII characters (but it did not).");
            if (rawGetUrl.equals(decodedGetUrl)) {
                throw new TestNotPossibleException("The generated GET URL did not contain percent-encoded character nor characters that needed percent-encoding.");
            }
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "A client requests a new upload slot [...] The upload service responds with both a PUT and a GET URL [...]. Non ASCII characters MUST be percent-encoded.")
    public void testSlotResponsePutUrlIsPercentEncoded() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "très cool.txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            final String rawPutUrl = response.getPutUrl();
            final String decodedPutUrl = URLDecoder.decode(rawPutUrl, StandardCharsets.UTF_8);
            // Note: we can't be sure that the service used the file name from the request in the response. We can only check for the absence of characters that need to be percent-encoded.
            assertTrue(StandardCharsets.US_ASCII.newEncoder().canEncode(rawPutUrl), "Expected the PUT URL from the slot returned by '" + request.getTo() + "' to '" + conOne + "' to contain only ASCII characters (but it did not).");
            if (rawPutUrl.equals(decodedPutUrl)) {
                throw new TestNotPossibleException("The generated PUT URL did not contain percent-encoded character nor characters that needed percent-encoding.");
            }
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <put> element MAY also contain a number of <header> elements [...]. Each <header> element MUST have a name-attribute [...]")
    public void testPutHeadersHaveNameAttribute() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "très cool.txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);
            final Map<String, String> headers = response.getHeaders();

            // Verify result.
            if (headers.isEmpty()) {
                throw new TestNotPossibleException("The generated PUT URL did not contain any headers that can be tested.");
            }
            assertFalse(headers.containsKey(null), "Expected all of the headers in the 'put' element in the slot sent to '" + request.getTo() + "' by '" + conOne + "' to have a name-attribute (but not all had - detected a missing or 'null' name).");
            assertFalse(headers.containsKey(""), "Expected all of the headers in the 'put' element in the slot sent to '" + request.getTo() + "' by '" + conOne + "' to have a name-attribute (but not all had - detected a missing or empty name).");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "4", quote = "The <put> element MAY also contain a number of <header> elements [...]. Only the following header names are allowed: Authorization, Cookie, Expires")
    public void testPutHeadersHaveValidNames() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        try {
            final String data = "This is part of an integration test.";
            final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "très cool.txt", data.getBytes().length);

            // Execute system-under-test.
            final SlotRaw response = connection.sendIqRequestAndWaitForResponse(request);
            final Map<String, String> headers = response.getHeaders();

            // Verify result.
            if (headers.isEmpty()) {
                throw new TestNotPossibleException("The generated PUT URL did not contain any headers that can be tested.");
            }
            final Set<String> validHeaderNames = Set.of("authorization", "cookie", "expires"); // Each header name MAY be present zero or more times, and are case insensitive (eXpires is the same as Expires).
            final Set<String> offendingNames = new HashSet<>();
            for (final String headerName : headers.keySet()) {
                if (headerName != null && !validHeaderNames.contains(headerName.toLowerCase())) {
                    offendingNames.add(headerName);
                }
            }
            assertTrue(offendingNames.isEmpty(), "Expected all of the headers returned in the 'put' element in the slot sent to '" + request.getTo() + "' by '" + conOne + "' to have a name-attribute that (case-insensitively) matches one of [" + String.join(", ", validHeaderNames) + "] (but not all had. Invalid value(s): [" + String.join(", ", offendingNames) + "]).");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
        }
    }

    @SmackIntegrationTest(section = "5", quote = " the service MAY include a <retry/> element [...]. The retry element MUST include an attribute 'stamp' which indicates the time at which the requesting entity may try again. The format of the timestamp MUST adhere to the date-time format specified in XMPP Date and Time Profiles (XEP-0082) and MUST be expressed in UTC.")
    public void testRetryError() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        final IqProvider<IQ> oldProvider = ProviderManager.getIQProvider("slot", "urn:xmpp:http:upload:0");
        ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", new SlotRawProvider());
        final ExtensionElementProvider<ExtensionElement> oldExtensionProvider = ProviderManager.getExtensionProvider(RetryError.ELEMENT, RetryError.NAMESPACE);
        ProviderManager.addExtensionProvider(RetryError.ELEMENT, RetryError.NAMESPACE, new RetryErrorProvider());
        try {
            // Execute system-under-test (attempt to hit a rate-limit).
            for (int i = 0; i < 10; i++) {
                final String data = "This is part of an integration test that attempts to hit a rate-limit. " + StringUtils.randomString(10);
                final SlotRequest request = new SlotRequest(hfumOne.getDefaultUploadService().getAddress(), "test-ratelimit-" + StringUtils.randomString(4) + ".txt", data.getBytes().length);
                connection.sendIqRequestAndWaitForResponse(request);
            }
            throw new TestNotPossibleException("The test was unable to generate an error of which this test intends to assert its properties.");
        } catch (XMPPException.XMPPErrorException e) {
            // Verify result.
            final RetryError retry = e.getStanzaError().getExtension("retry", "urn:xmpp:http:upload:0");
            if (retry != null) {
                // Multiple assertions, that could go in seperate tests. As this test involves generating a flood of requests (trying to reach resource starvation), let's combine all the tests into one. This reduces overhead.
                assertNotNull(retry.getStamp(), "The 'retry' element included in the error returned by " + hfumOne.getDefaultUploadService().getAddress() + "' to '" + conOne + "' was expected to contain a 'stamp' attribute value (but it did not).");
                try {
                    ParserUtils.getDateFromXep82String(retry.getStamp());
                } catch (ParseException pe) {
                    fail("Expected the 'stamp' attribute value of the 'retry' element included in the error returned by " + hfumOne.getDefaultUploadService().getAddress() + "' to '" + conOne + "' to be in the dateTime format specified in XMPP Date and Time Profiles (XEP-0082), but it was not. Offending value: " + retry.getStamp(), pe);
                }
                final String[] zuluEndings = new String[] { "z", "Z", "+00:00", "-00:00"};
                assertTrue(Arrays.stream(zuluEndings).anyMatch(retry.getStamp()::endsWith), "Expected the 'stamp' attribute value of the 'retry' element included in the error returned by " + hfumOne.getDefaultUploadService().getAddress() + "' to '" + conOne + "' to be expressed in UTC, but it was not. Offending value: " + retry.getStamp());
                return; // Done asserting. Do not process anything else.
            }
            // This test intentionally floods the service. This might result in an exception that is not explicitly the subject of the assertions of this text, but not unexpected either. Do not rethrow.
            throw new TestNotPossibleException("The error returned by the service did not contain an attribute that can be checked by this test.");
        } finally {
            // Restore the previous provider, if there was one.
            ProviderManager.removeIQProvider("slot", "urn:xmpp:http:upload:0");
            if (oldProvider != null) {
                ProviderManager.addIQProvider("slot", "urn:xmpp:http:upload:0", oldProvider);
            }
            ProviderManager.removeExtensionProvider(RetryError.ELEMENT, RetryError.NAMESPACE);
            if (oldExtensionProvider != null) {
                ProviderManager.addExtensionProvider(RetryError.ELEMENT, RetryError.NAMESPACE, oldExtensionProvider);
            }
        }
    }
}
