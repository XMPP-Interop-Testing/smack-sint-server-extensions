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
package org.igniterealtime.smack.inttest.xep0363.provider;

import org.igniterealtime.smack.inttest.xep0363.element.SlotRaw;
import org.igniterealtime.smack.inttest.xep0363.element.SlotRaw_V0_2;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.UploadService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A duplicate of Smack's SlotProvider implementation, but one that generates instances of
 * {@link org.igniterealtime.smack.inttest.xep0363.element.SlotRaw}
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 * @see org.igniterealtime.smack.inttest.xep0363.element.SlotRaw
 */
public class SlotRawProvider extends IqProvider<SlotRaw> {

    @Override
    public SlotRaw parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        final String namespace = parser.getNamespace();

        final UploadService.Version version = HttpFileUploadManager.namespaceToVersion(namespace);
        assert version != null;

        String putUrl = null;
        String getUrl = null;
        PutElement_V0_4_Content putElementV04Content = null;

        outerloop: while (true) {
            XmlPullParser.Event event = parser.next();

            switch (event) {
                case START_ELEMENT:
                    String name = parser.getName();
                    switch (name) {
                        case "put": {
                            switch (version) {
                            case v0_2:
                                putUrl = parser.nextText();
                                break;
                            case v0_3:
                                putElementV04Content = parsePutElement_V0_4(parser);
                                break;
                            default:
                                throw new AssertionError();
                            }
                            break;
                        }
                        case "get":
                            switch (version) {
                            case v0_2:
                                getUrl = parser.nextText();
                                break;
                            case v0_3:
                                getUrl = parser.getAttributeValue(null, "url");
                                break;
                            default:
                                throw new AssertionError();
                            }
                            break;
                    }
                    break;
                case END_ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                default:
                    // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                    break;
            }
        }

        switch (version) {
        case v0_3:
            return new SlotRaw(putElementV04Content.putUrl, getUrl, putElementV04Content.headers);
        case v0_2:
            return new SlotRaw_V0_2(putUrl, getUrl);
        default:
            throw new AssertionError();
        }
    }

    public static PutElement_V0_4_Content parsePutElement_V0_4(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();

        String putUrl = parser.getAttributeValue(null, "url");

        Map<String, String> headers = null;
        outerloop: while (true) {
            XmlPullParser.Event next = parser.next();
            switch (next) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "header":
                    String headerName = parser.getAttributeValue("name");
                    String headerValue = parser.nextText();;
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    headers.put(headerName, headerValue);
                    break;
                default:
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return new PutElement_V0_4_Content(putUrl, headers);
    }

    public static final class PutElement_V0_4_Content {
        private final String putUrl;
        private final Map<String, String> headers;

        private PutElement_V0_4_Content(String putUrl, Map<String, String> headers) {
            this.putUrl = putUrl;
            this.headers = headers;
        }

        public String getPutUrl() {
            return putUrl;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}
