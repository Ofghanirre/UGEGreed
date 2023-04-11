package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.Controller;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

/**
 * Manages all aspects of the application relates to jobs
 */
public final class Jobs {

    private static final Logger logger = Logger.getLogger(Jobs.class.getName());
    private static final int TASK_EXECUTOR_MAX_READING_AMOUNT = 128;

    private final Controller controller;
    private final RandomGenerator rng = RandomGenerator.of("L64X128MixRandom");
    private final Map<Long, Job> jobs = new HashMap<>();
    private final ArrayDeque<Packet> contextQueue = new ArrayDeque<>();
    private final ArrayBlockingQueue<AnsPacket> taskExecutorQueue = new ArrayBlockingQueue<>(TASK_EXECUTOR_MAX_READING_AMOUNT);
    private final TaskExecutor taskExecutor = new TaskExecutor(this.taskExecutorQueue);
    private final Path resultPath;


    /**
     * Creates a jobs manager
     * @param resultPath path to the directory where to store results, it must already exist.
     */
    public Jobs(Path resultPath, Controller controller) {
        this.resultPath = Objects.requireNonNull(resultPath);
        this.controller = Objects.requireNonNull(controller);
    }

    private void checkJobParameters(String jarURL, String mainClass, long start, long end, String fileName) {
        if (end < start) {
            throw new IllegalArgumentException("end must be superior to start");
        }
        Objects.requireNonNull(jarURL);
        Objects.requireNonNull(mainClass);
        Objects.requireNonNull(fileName);
        if (jarURL.isEmpty() || mainClass.isEmpty() || fileName.isEmpty()) {
            throw new IllegalArgumentException("strings cannot be empty");
        }
    }


    /**
     * Creates a new job
     * @param jarURL url to the JAR to use
     * @param mainClass name of the class containing the Checker
     * @param start start of the range (included)
     * @param end end of the range (excluded)
     * @param fileName name of the file in which to store the results
     * @return true if the job is valid, false else
     */
    public boolean createJob(String jarURL, String mainClass, long start, long end, String fileName) throws IOException {
        checkJobParameters(jarURL, mainClass, start, end, fileName);
        long jobID = generateJobID();

        Path fullPath;
        try {
            fullPath = resultPath.resolve(fileName);
        } catch (InvalidPathException e) {
            return false;
        }
        var job = new UpstreamJob(jobID, jarURL, mainClass, start, end, fullPath, taskExecutor, controller);

        job.prepareJob();
        jobs.put(jobID, job);
        return true;
    }

    private long generateJobID() {
        var jobID = rng.nextLong(Long.MAX_VALUE);
        while (jobs.containsKey(jobID)) {
            jobID = rng.nextLong(Long.MAX_VALUE);
        }
        return jobID;
    }

    private void sendPacketToJob(Packet packet, long job_id) throws IOException {
        var job = jobs.get(job_id);
        if (job == null) {
            logger.warning("Invalid Job_id given " + job_id);
            return;
        }
        job.handlePacket(packet);
    }

    /**
     * Queues a packet that came from another node
     * @param packet packet to queue
     */
    public void queueContextPacket(Packet packet) {
        Objects.requireNonNull(packet);
        contextQueue.add(packet);
    }

    /**
     * Processes the queue for packets that came from other nodes
     */
    public void processContextQueue() throws IOException {
        var numberOfPackets = contextQueue.size();
        for (var i = 0 ; i < numberOfPackets ; i++) {
            var packet = contextQueue.remove();
            switch (packet) {
                case AnsPacket ansPacket -> sendPacketToJob(ansPacket, ansPacket.job_id());
                case AccPacket accPacket -> sendPacketToJob(accPacket, accPacket.job_id());
                case RefPacket refPacket -> sendPacketToJob(refPacket, refPacket.job_id());
                default -> throw new AssertionError("unhandled packet tested");
            }
        }
    }

    /**
     * Processes a request packet
     * @param reqPacket request packet
     * @param context context it came from
     * @throws IOException in case of connection errors
     */
    public void processReqPacket(ReqPacket reqPacket, ConnectionContext context) throws IOException {
        var job = new DownstreamJob(context, reqPacket, taskExecutor, controller);
        job.prepareJob();
        jobs.put(reqPacket.job_id(), job);
    }

    /**
     * Processes the queue for answers that came from this node.
     */
    public void processTaskExecutorQueue() throws IOException {
        for (int readCounter = 0; readCounter < TASK_EXECUTOR_MAX_READING_AMOUNT; readCounter++){
            AnsPacket packet = taskExecutorQueue.poll();
            if (packet == null) break;
            sendPacketToJob(packet, packet.job_id());
        }
    }

    /**
     * Returns the list of jobs which are upstream of the given node
     * @param node node
     * @return list of jobs which are upstream of the given node
     */
    public List<DownstreamJob> getJobsUpstreamOfNode(SelectionKey node) {
        Objects.requireNonNull(node);
        return jobs.values().stream().<DownstreamJob>mapMulti((job, consumer) -> {
            switch (job) {
                case UpstreamJob ignored -> {}
                case DownstreamJob downstreamJob -> {
                    if (downstreamJob.getUpstreamContext().key() != node) {
                        consumer.accept(downstreamJob);
                    }
                }
                default -> throw new AssertionError();
            }
        }).toList();
    }

    /**
     * Swaps the upstream host of downstream jobs for a new one.
     * @param previous previous host
     * @param swap new host
     */
    public void swapUpstreamHost(SelectionKey previous, SelectionKey swap) {
        Objects.requireNonNull(previous);
        Objects.requireNonNull(swap);
        jobs.forEach((k, job) -> {
            switch (job) {
                case UpstreamJob ignored -> {}
                case DownstreamJob downstreamJob -> {
                    if (downstreamJob.getUpstreamContext().key() == previous) {
                        downstreamJob.setUpstreamContext((ConnectionContext) swap.attachment());
                    }
                }
                default -> throw new AssertionError();
            }
        });
    }

    /**
     * Swaps the upstream host of a certain job for a new one.
     * @param jobID job that must be updated
     * @param swap new host
     */
    public void swapUpstreamHost(long jobID, SelectionKey swap) {
        var job = jobs.get(jobID);
        if (job != null) {
            switch (job) {
                case DownstreamJob downstreamJob -> downstreamJob.setUpstreamContext((ConnectionContext) swap.attachment());
                case UpstreamJob ignored -> logger.warning("Trying to change upstream host of a job that is already upstream");
                default -> throw new AssertionError();
            }
        }
    }

    /**
     * Sends ref packets to every upstream node for work that this node took on
     * but did not complete
     */
    public void cancelAllOngoingDownstreamWork() {
        jobs.forEach((k, job) -> {
            switch (job) {
                case UpstreamJob ignored -> {}
                case DownstreamJob downstreamJob -> downstreamJob.cancelOngoingWork();
                default -> throw new AssertionError();
            }
        });
    }
}
