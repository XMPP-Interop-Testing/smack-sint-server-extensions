package org.igniterealtime.smack.inttest.rfc6121.section2;

import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * A Smack AbstractIqRequestHandler that intends to replace the handler that is used by Smack internally, to manage
 * its 'Roster' implementation. This replacement should delegate to the original (as to not break Smack functionality)
 * but can also be used to register a syncpoint that is triggered when the first few roster pushes are sent by the server.
 */
final class RosterPushListenerForAmount extends AbstractIqRequestHandler
{
    private IQRequestHandler delegate;

    private ResultSyncPoint<List<RosterPacket>, Exception> syncPoint;
    private int count;
    private List<RosterPacket> pushes;

    RosterPushListenerForAmount()
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

    public synchronized void registerSyncPointFor(final ResultSyncPoint<List<RosterPacket>, Exception> syncPoint, final int count)
    {
        this.syncPoint = syncPoint;
        this.count = count;
        this.pushes = new ArrayList<>(count);
    }

    @Override
    public IQ handleIQRequest(IQ iqRequest)
    {
        final IQ result = delegate.handleIQRequest(iqRequest);

        synchronized (this) {
            if (count > 0) {
                final RosterPacket rosterPacket = (RosterPacket) iqRequest;
                pushes.add(rosterPacket);
                if (pushes.size() == count) {
                    syncPoint.signal(pushes);
                }
            }
        }
        return result;
    }
}
