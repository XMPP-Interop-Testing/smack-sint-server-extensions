package org.igniterealtime.smack.inttest.xep0421.provider;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import javax.xml.namespace.QName;
import java.io.IOException;

public class OccupantId implements ExtensionElement
{
    public static final String ELEMENT_NAME = "occupant-id";
    public static final String NAMESPACE = "urn:xmpp:occupant-id:0";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT_NAME);
    private final String id;

    public OccupantId(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML(XmlEnvironment enclosingNamespace) {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\" ");
        buf.append("id=\"").append(this.getId());
        buf.append("\"/>");
        return buf.toString();
    }

    public static class Provider extends ExtensionElementProvider<OccupantId>
    {
        public Provider() {
        }

        public OccupantId parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException
        {
            final String id = ParserUtils.getRequiredAttribute(parser, "id");
            parser.next();
            return new OccupantId(id);
        }
    }
}
