/*
 * Copyright (C) 2025 Guus der Kinderen. All rights reserved.
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

package org.igniterealtime.smack.inttest.xep0433;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.SimpleIQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;

/**
 * Implements the IQ stanza representing the request/response for a search form, as defined by XEP-0433 Extended Channel Search
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0433.html">XEP-0433: Extended Channel Search</a>
 */
public class ExtendedChannelSearchForm extends SimpleIQ
{
    public static final String ELEMENT = "search";
    public static final String NAMESPACE = "urn:xmpp:channel-search:0:search";

    public ExtendedChannelSearchForm() {
        super(ELEMENT, NAMESPACE);
    }

    public static class Provider extends IqProvider<ExtendedChannelSearchForm>
    {
        @Override
        public ExtendedChannelSearchForm parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException
        {
            ExtendedChannelSearchForm search = null;

            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT && parser.getNamespace().equals("jabber:x:data")) {
                    search = new ExtendedChannelSearchForm();
                    PacketParserUtils.addExtensionElement(search, parser, xmlEnvironment);
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getName().equals(ELEMENT)) {
                        done = true;
                    }
                }
            }

            return search;
        }
    }
}
