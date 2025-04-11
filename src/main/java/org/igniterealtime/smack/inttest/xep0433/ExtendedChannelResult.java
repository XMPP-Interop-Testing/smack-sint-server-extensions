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

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents search result element, as defined by XEP-0433 Extended Channel Search
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="https://xmpp.org/extensions/xep-0433.html">XEP-0433: Extended Channel Search</a>
 */
public class ExtendedChannelResult extends SimpleIQ
{
    public static final String ELEMENT = "result";
    public static final String NAMESPACE = "urn:xmpp:channel-search:0:search";

    private final List<Item> items;

    public ExtendedChannelResult() {
        super(ELEMENT, NAMESPACE);
        this.items = new LinkedList<>();
    }

    public ExtendedChannelResult(final List<Item> items) {
        super(ELEMENT, NAMESPACE);
        this.items = items;
    }

    public List<Item> getItems()
    {
        return items;
    }

    public static class Provider extends IqProvider<ExtendedChannelResult>
    {
        @Override
        public ExtendedChannelResult parse(XmlPullParser parser, int i, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException, ParseException
        {
            final List<Item> items = new LinkedList<>();
            String address = null;
            String name = null;
            String description = null;
            String language = null;
            Integer nusers = null;
            String serviceType = null;
            Boolean isOpen = null;
            String anonymityMode = null;

            boolean done = false;

            while(!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    if (parser.getName().equals("item")) {
                        address = ParserUtils.getRequiredAttribute(parser, "address");
                    } else {
                        switch (parser.getName()) {
                            case "name":
                                name = parser.nextText();
                                break;
                            case "description":
                                description = parser.nextText();
                                break;
                            case "language":
                                language = parser.nextText();
                                break;
                            case "nusers":
                                nusers = ParserUtils.getIntegerFromNextText(parser);
                                break;
                            case "service-type":
                                serviceType = parser.nextText();
                                break;
                            case "is-open":
                                isOpen = true;
                                break;
                            case "anonymity-mode":
                                anonymityMode = parser.nextText();
                                break;
                        }
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getName().equals("item")) {
                        final Item item = new Item(address, name, description, language, nusers, serviceType, isOpen, anonymityMode);
                        address = null;
                        name = null;
                        description = null;
                        language = null;
                        nusers = null;
                        serviceType = null;
                        isOpen = null;
                        anonymityMode = null;

                        items.add(item);
                    } else if (parser.getName().equals(ELEMENT)) {
                        done = true;
                    }
                }
            }

            return new ExtendedChannelResult(items);
        }
    }

    public static class Item {
        final String address;
        final String name;
        final String description;
        final String language;
        final Integer nusers;
        final String serviceType;
        final Boolean isOpen;
        final String anonymityMode;

        public Item(String address, String name, String description, String language, Integer nusers, String serviceType, Boolean isOpen, String anonymityMode)
        {
            this.address = address;
            this.name = name;
            this.description = description;
            this.language = language;
            this.nusers = nusers;
            this.serviceType = serviceType;
            this.isOpen = isOpen;
            this.anonymityMode = anonymityMode;
        }
    }
}
