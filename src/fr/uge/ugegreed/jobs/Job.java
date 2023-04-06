package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.packets.Packet;

import java.io.IOException;

/**
 * Represents a job
 */
public sealed interface Job permits DownstreamJob, UpstreamJob {

    /**
     * Asks a job to process a packet intended for it
     * @param packet packet to process
     * @return true is the packet was consumed, false else
     * @throws IOException
     */
    boolean handlePacket(Packet packet) throws IOException;
}
