package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.packets.Packet;

public sealed interface Job permits DownstreamJob, UpstreamJob {
    void handlePacket(Packet packet);
}
