package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.CheckerRetriever;
import fr.uge.ugegreed.Controller;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public final class UpstreamJob implements Job {
    private final static Logger logger = Logger.getLogger(UpstreamJob.class.getName());
    private final TaskExecutor executor;
    private final Controller controller;
    private final long jobID;
    private final String jarURL;
    private final String className;
    private final long start;
    private final long end;
    private final Path outputPath;
    private BufferedWriter output;
    private boolean jobRunning = false;
    private long counter;

    /**
     * Creates a new upstream job
     * @param jobID id of the job
     * @param jarURL URL to the jar containing the Checker
     * @param className name of the checker
     * @param start start of the range (included)
     * @param end end of the range (excluded)
     * @param outputFilePath path to the output file
     * @param executor taskExecutor this job must use
     */
    public UpstreamJob(long jobID, String jarURL, String className, long start, long end, Path outputFilePath,
                       TaskExecutor executor, Controller controller) {
        if (jobID < 0) {
            throw new IllegalArgumentException("jobID must be positive");
        }
        this.jobID = jobID;
        this.jarURL = Objects.requireNonNull(jarURL);
        this.className = Objects.requireNonNull(className);
        if (end < start) {
            throw new IllegalArgumentException("end must be superior to start");
        }
        this.start = start;
        this.end = end;
        this.outputPath = Objects.requireNonNull(outputFilePath);
        this.executor = Objects.requireNonNull(executor);
        this.controller = Objects.requireNonNull(controller);
    }


    /**
     * Prepares and starts a job
     */
    public boolean startJob() throws IOException {
        this.output = Files.newBufferedWriter(outputPath);

        // TODO: replace this!!!!
        var checker = CheckerRetriever.checkerFromHTTP(jarURL, className);
        if (checker.isEmpty()) { return false; }

        // Distribution algorithm
        var totalPotential = controller.potential();
        var sizeOfSlices = Long.max(Math.ceilDiv(end - start, totalPotential), 1);

        var localPotential = 1;
        var cursor = start;

        logger.info("Scheduling " + cursor + " to " + (cursor + sizeOfSlices * localPotential) + " for job " + jobID);
        executor.addJob(checker.get(), jobID, cursor, cursor + sizeOfSlices * localPotential);
        cursor += sizeOfSlices * localPotential;

        var hosts = controller.availableNodesStream().toList();
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
        }

        jobRunning = true;
        logger.info("Job " + jobID + " distributed and started.");
        return true;
    }

    public void end() throws IOException {
        this.output.close();
    }

    @Override
    public boolean handlePacket(Packet packet) throws IOException {
        if (!jobRunning) { return true; }
        return switch (packet) {
            case AnsPacket ansPacket -> handleAnswer(ansPacket);
            case AccPacket accPacket -> handleAccept(accPacket);
            case RefPacket refPacket -> handleRefuse(refPacket);
            default -> throw new AssertionError();
        };
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
        return true;
    }

    private boolean handleAccept(AccPacket ignored) {
        // Do nothing
        return true;
    }

    private boolean handleAnswer(AnsPacket ansPacket) throws IOException {
        output.write(ansPacket.result());
        output.newLine();
        counter++;
        if (counter >= end - start) {
            jobRunning = false;
            logger.info("Job " + jobID + " finished.");
            end();
        }
        return true;
    }
}
