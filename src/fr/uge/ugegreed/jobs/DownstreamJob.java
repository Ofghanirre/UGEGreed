package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.CheckerRetriever;
import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.Controller;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

public final class DownstreamJob implements Job {
    private static class WorkRange {
        private final long start;
        private final long end;
        private long lastSent;

        private WorkRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        private void updateLastSent(long lastSent) {
            if (lastSent <= this.lastSent) { throw new IllegalStateException(); }
            this.lastSent = lastSent;
        }
    }
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

    // Field about the work that was taken by the node itself
    private final ArrayList<WorkRange> workRanges = new ArrayList<>();

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
        var totalPotential = controller.potential() - upstreamHost.potential();
        var sizeOfSlices = Long.max(Math.ceilDiv(end - start, totalPotential), 1);

        var localPotential = 1;
        var cursor = start;
        executor.addJob(checker.get(), jobID, cursor, cursor + sizeOfSlices);
        workRanges.add(new WorkRange(cursor, cursor + sizeOfSlices));
        cursor += sizeOfSlices * localPotential;

        var hosts = controller.availableNodesStream()
            .filter(ctx -> ctx.key() != upstreamHost.key())
            .toList();
        for (var context : hosts) {
            if (cursor >= end) { break; }
            localPotential = context.potential();
            context.queuePacket(
                new ReqPacket(jobID, jarURL, className, cursor, Long.min(cursor + sizeOfSlices * localPotential, end))
            );
            cursor += sizeOfSlices * localPotential;
        }

        // If for some reason there are remaining numbers, the node takes them
        if (cursor < end) {
            logger.warning("Numbers " + cursor + " to " + end + " for job " + jobID +
                " were not distributed, scheduling them locally...");
            executor.addJob(checker.get(), jobID, cursor, end);
            workRanges.add(new WorkRange(cursor, end));
        }

        upstreamHost.queuePacket(new AccPacket(jobID, start, end));
        jobRunning = true;
        logger.info("Job " + jobID + " started.");
        return true;
    }


    @Override
    public boolean handlePacket(Packet packet) {
        if (!jobRunning) { return true; }
        return switch (packet) {
            case AnsPacket ansPacket -> handleAnswer(ansPacket);
            case AccPacket accPacket -> handleAccept(accPacket);
            case RefPacket refPacket -> handleRefuse(refPacket);
            default -> throw new AssertionError();
        };
    }

    public ConnectionContext getUpstreamContext() {
        return upstreamHost;
    }

    public void setUpstreamContext(ConnectionContext newContext) {
        upstreamHost = newContext;
    }

    @Override
    public long jobID() {
        return jobID;
    }

    private boolean handleRefuse(RefPacket refPacket) {
        // Takes job for himself

        // TODO: replace this as well...
        var checker = CheckerRetriever.checkerFromHTTP(jarURL, className);
        if (checker.isEmpty()) { throw new AssertionError(); }

        logger.info("Received refusal for range " + refPacket.range_start() + " to "
            + refPacket.range_end() + ", rescheduling locally...");
        executor.addJob(checker.get(), jobID, refPacket.range_start(), refPacket.range_end());
        workRanges.add(new WorkRange(refPacket.range_start(), refPacket.range_end()));
        return true;
    }

    private boolean handleAccept(AccPacket ignored) {
        // Do nothing
        return true;
    }

    private boolean handleAnswer(AnsPacket ansPacket) {
        if (upstreamHost.isDisconnecting()) {
            return false;
        }
        upstreamHost.queuePacket(ansPacket);
        updateWorkRanges(ansPacket.number());
        counter++;
        if (counter >= end - start) {
            jobRunning = false;
            logger.info("Job " + jobID + " finished.");
        }
        return true;
    }

    private void updateWorkRanges(long number) {
        for (var workRange : workRanges) {
            if (number >= workRange.start && number < workRange.end) {
                workRange.updateLastSent(number);
                break;
            }
        }
    }

    /**
     * Sends refpackets to the upstream node for each work ranges this node took on and that isn't completed
     */
    public void cancelOngoingWork() {
        for (var workRange : workRanges) {
            if (workRange.lastSent < workRange.end - 1) {
                upstreamHost.queuePacket(new RefPacket(jobID, workRange.lastSent + 1, workRange.end));
            }
        }
    }
}
