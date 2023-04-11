package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.packets.Packet;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a job
 */
public sealed interface Job permits DownstreamJob, UpstreamJob {

    /**
     * Asks a job to process a packet intended for it
     * @param packet packet to process
     * @throws IOException
     */
    void handlePacket(Packet packet) throws IOException;

    /**
     * Returns job id
     * @return job id
     */
    long jobID();

    /**
     * Manages the case where a JAR could not be downloaded
     */
    void jarDownloadFail();

    /**
     * Manages the case where the JAR was downloaded sucessfully
     * @param jarPath path to the jar
     */
    void jarDownloadSuccess(Path jarPath);
}
