package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.packets.ReqPacket;

public final class DownstreamJob implements Job {
    private ConnectionContext upstreamHost;
    private final ReqPacket request;

    public DownstreamJob(ConnectionContext upstreamHost, ReqPacket reqPacket) {
        this.upstreamHost = upstreamHost;
        this.request = reqPacket;

        // TODO Job initialization
        // receive request
        // schedule taskExecutor
        // acc / ref packet sending
    }

    @Override
    public void handlePacket(Packet packet) {
        switch (packet) {
            case AnsPacket ansPacket -> handleAnswer(ansPacket);
            default -> throw new AssertionError();
        }
    }

    private void handleAnswer(AnsPacket ansPacket) {
        upstreamHost.queuePacket(ansPacket);
    }
}
