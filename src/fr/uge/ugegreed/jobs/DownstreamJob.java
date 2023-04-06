package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.CheckerRetriever;
import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.Controller;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.packets.ReqPacket;

import java.util.Objects;
import java.util.logging.Logger;

public final class DownstreamJob implements Job {
    private final Logger logger = Logger.getLogger(DownstreamJob.class.getName());
    private ConnectionContext upstreamHost;
    private final long jobID;
    private final String jarURL;
    private final String className;
    private final long start;
    private final long end;
    private long counter = 0;
    private final TaskExecutor executor;
    private final Controller controller;
    private boolean jobRunning = false;

    public DownstreamJob(ConnectionContext upstreamHost, ReqPacket reqPacket, TaskExecutor executor,
                         Controller controller) {
        Objects.requireNonNull(reqPacket);
        this.upstreamHost = Objects.requireNonNull(upstreamHost);
        this.jobID = reqPacket.job_id();
        this.jarURL = reqPacket.jar_URL();
        this.className = reqPacket.class_name();
        this.start = reqPacket.range_start();
        this.end = reqPacket.range_end();
        this.executor = Objects.requireNonNull(executor);
        this.controller = Objects.requireNonNull(controller);
    }


    /**
     * Prepares and starts a job
     */
    public boolean startJob() {
        // TODO: replace this!!!!
        var checker = CheckerRetriever.checkerFromHTTP(jarURL, className);
        if (checker.isEmpty()) { return false; }

        // Distribution algorithm
        var totalPotential = controller.potential();
        var sizeOfSlices = Long.max((end - start) / totalPotential, 1);

        var localPotential = 1;
        var cursor = start;
        executor.addJob(checker.get(), jobID, cursor, cursor + sizeOfSlices);

        var hosts = controller.connectedNodeStream()
            .filter(ctx -> ctx.key() != upstreamHost.key())
            .toList();
        for (var context : hosts) {
            cursor += sizeOfSlices * localPotential;
            if (cursor >= end) { break; }
            localPotential = context.potential();
            context.queuePacket(
                new ReqPacket(jobID, jarURL, className, cursor, Long.min(cursor + sizeOfSlices * localPotential, end))
            );
        }

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
