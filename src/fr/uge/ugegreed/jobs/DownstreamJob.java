package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.CheckerRetriever;
import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.packets.ReqPacket;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public final class DownstreamJob implements Job {
    private final Logger logger = Logger.getLogger(DownstreamJob.class.getName());
    private ConnectionContext upstreamHost;
    private final long jobID;
    private final String jarUrl;
    private final String className;
    private final long start;
    private final long end;
    private long counter = 0;
    private final TaskExecutor executor;
    private boolean jobRunning = false;

    public DownstreamJob(ConnectionContext upstreamHost, ReqPacket reqPacket, TaskExecutor executor) {
        this.upstreamHost = upstreamHost;
        this.jobID = reqPacket.job_id();
        this.jarUrl = reqPacket.jar_URL();
        this.className = reqPacket.class_name();
        this.start = reqPacket.range_start();
        this.end = reqPacket.range_end();
        this.executor = executor;

        Objects.requireNonNull(executor);
        // TODO Job initialization
        // receive request
        // schedule taskExecutor
        // acc / ref packet sending<
    }


    /**
     * Prepares and starts a job
     */
    public boolean startJob() {
        // TODO: replace this!!!!
        var checker = CheckerRetriever.checkerFromHTTP(jarUrl, className);
        if (checker.isEmpty()) { return false; }

        // TODO: Add distribution of requests

        executor.addJob(checker.get(), jobID, start, end);
        jobRunning = true;
        logger.info("Job " + jobID + " started.");
        return true;
    }


    @Override
    public void handlePacket(Packet packet) {
        if (!jobRunning) { return; }
        switch (packet) {
            case AnsPacket ansPacket -> handleAnswer(ansPacket);
            default -> throw new AssertionError();
        }
    }

    private void handleAnswer(AnsPacket ansPacket) {
        upstreamHost.queuePacket(ansPacket);
        counter++;
        if (counter >= end - start) {
            jobRunning = false;
            logger.info("Job " + jobID + " finished.");
        }
    }
}
