package org.igniterealtime.smack.inttest.rfc6121.section2;

import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

import java.util.LinkedList;
import java.util.List;

/**
 * A Smack AbstractIqRequestHandler that intends to replace the handler that is used by Smack internally, to manage
 * its 'Roster' implementation. This replacement should delegate to the original (as to not break Smack functionality)
 * but can also be used to register a syncpoint that is triggered when any roster push for an item that has a particular
 * JID is sent by the server.
 */
final class RosterPushListenerWithTarget extends AbstractIqRequestHandler
{
    private IQRequestHandler delegate;

    private ResultSyncPoint<RosterPacket, Exception> syncPoint;
    private Jid target;

    RosterPushListenerWithTarget()
    {
        super(RosterPacket.ELEMENT, RosterPacket.NAMESPACE, IQ.Type.set, Mode.sync);
    }

    /**
     * Allows this handler to be chained with another handler. Expected to be used with the handler that is originally
     * registered by Smack's Roster implementation.
     */
    public void setDelegate(IQRequestHandler delegate)
    {
        this.delegate = delegate;
    }

    public synchronized void registerSyncPointFor(final ResultSyncPoint<RosterPacket, Exception> syncPoint, final Jid target)
    {
        this.syncPoint = syncPoint;
        this.target = target;
    }

    @Override
    public IQ handleIQRequest(IQ iqRequest)
    {
        final IQ result = delegate.handleIQRequest(iqRequest);

        synchronized (this) {
            if (target != null) {
                final RosterPacket rosterPacket = (RosterPacket) iqRequest;
                for (RosterPacket.Item rosterItem : rosterPacket.getRosterItems()) {
                    if (rosterItem.getJid().equals(target)) {
                        syncPoint.signal(rosterPacket);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Sends on the provided connection a roster stanza that is expected to change the roster (it should be a 'set' request),
     * then waits until the server has sent back both the IQ response to the IQ request as the associated roster push.
     *
     * The associated roster push that is sent from the server back to the client will contain a roster 'ver'. This
     * value is the return value of this method.
     *
     * @param connection The connection on which XMPP stanzas are exchanged.
     * @param rosterPacket The stanza that will be sent (should represent a roster change request).
     * @return The roster push that was received for the change.
     */
    public static RosterPacket sendRosterChangeAndWaitForResultAndPush(final XMPPConnection connection, final long timeout, final RosterPacket rosterPacket) throws Exception
    {
        if (rosterPacket == null || rosterPacket.getRosterItems().size() != 1 || rosterPacket.getType() != IQ.Type.set) {
            throw new IllegalArgumentException();
        }

        final BareJid target = rosterPacket.getRosterItems().iterator().next().getJid();

        final RosterPushListenerWithTarget rosterPushHandler = new RosterPushListenerWithTarget();
        final IQRequestHandler oldHandler = connection.registerIQRequestHandler(rosterPushHandler);
        rosterPushHandler.setDelegate(oldHandler); // Allows Smack internal classes (like Roster) to keep on processing roster changes.
        try
        {
            final ResultSyncPoint<RosterPacket, Exception> rosterPushStartPointReceived = new ResultSyncPoint<>();
            rosterPushHandler.registerSyncPointFor(rosterPushStartPointReceived, target);

            connection.sendIqRequestAndWaitForResponse(rosterPacket);

            return rosterPushStartPointReceived.waitForResult(timeout);
        }
        finally
        {
            if (oldHandler != null) {
                connection.registerIQRequestHandler(oldHandler);
            } else {
                connection.unregisterIQRequestHandler(rosterPushHandler);
            }
        }
    }

    /**
     * Sends on the provided connection a series of roster packets that are expected to change the roster (they should be 'set' requests).
     * Waits between each stanza until the server has sent back both the IQ response to the IQ request as the associated roster push.
     *
     * The associated roster pushes that are sent from the server back to the client are returned by this method (in the
     * same order as the requests where sent).
     *
     * @param connection The connection on which XMPP stanzas are exchanged.
     * @param rosterPackets The stanzas that will be sent (should represent roster change requests).
     * @return The roster pushes that are received for each change.
     */
    public static List<RosterPacket> sendRosterChangesAndWaitForResultAndPush(final XMPPConnection connection, final long timeout, final RosterPacket... rosterPackets) throws Exception
    {
        final List<RosterPacket> result = new LinkedList<>();
        for (final RosterPacket rosterPacket : rosterPackets) {
            result.add(sendRosterChangeAndWaitForResultAndPush(connection, timeout, rosterPacket));
        }
        return result;
    }
}
