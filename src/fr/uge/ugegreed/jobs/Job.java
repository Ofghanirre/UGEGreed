package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.packets.Packet;

import java.io.IOException;

public sealed interface Job permits DownstreamJob, UpstreamJob {
    void handlePacket(Packet packet) throws IOException;
}
