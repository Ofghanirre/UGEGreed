package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.Controller;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.*;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

/**
 * Manages all aspects of the application relates to jobs
 */
public class Jobs {

    private static final Logger logger = Logger.getLogger(Jobs.class.getName());
    private static final int TASK_EXECUTOR_MAX_READING_AMOUNT = 128;

    private final Controller controller;
    private final RandomGenerator rng = RandomGenerator.of("L64X128MixRandom");
    private final Map<Long, Job> jobs = new HashMap<>();
    private final ArrayDeque<ContextPacket> contextQueue = new ArrayDeque<>();
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
    public boolean createJob(String jarURL, String mainClass, long start, long end, String fileName) {
        checkJobParameters(jarURL, mainClass, start, end, fileName);
        long jobID = generateJobID();

        Path fullPath;
        try {
            fullPath = resultPath.resolve(fileName);
        } catch (InvalidPathException e) {
            return false;
        }
        var job = new UpstreamJob(jobID, jarURL, mainClass, start, end, fullPath, taskExecutor, controller);

        try {
            if (!job.startJob()) { return false; }
        } catch (IOException e) {
            return false;
        }

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
        //logger.info(packet.toString());
        if (job == null) {
            logger.warning("Invalid Job_id given " + job_id);
            return;
        }
        job.handlePacket(packet);
    }

    /**
     * Queues a packet that came from another node
     * @param packet packet to queue
     * @param context context of the node it came from
     */
    public void queueContextPacket(Packet packet, ConnectionContext context) {
        contextQueue.add(new ContextPacket(packet, context));
    }

    /**
     * Processes the queue for packets that came from other nodes
     */
    public void processContextQueue() throws IOException {
        while (!contextQueue.isEmpty()) {
            ContextPacket contextPacket = contextQueue.remove();
            switch (contextPacket.packet()) {
                case AnsPacket ansPacket -> sendPacketToJob(ansPacket, ansPacket.job_id());
                case AccPacket accPacket -> sendPacketToJob(accPacket, accPacket.job_id());
                case RefPacket refPacket -> sendPacketToJob(refPacket, refPacket.job_id());
                case ReqPacket reqPacket -> processReqPacket(reqPacket, contextPacket.context());
                default -> throw new AssertionError("unhandled Packet tested");
            }
        }
    }

    private void processReqPacket(ReqPacket reqPacket, ConnectionContext context) {
        var job = new DownstreamJob(context, reqPacket, taskExecutor, controller);
        if (!job.startJob()) {
            logger.warning("Could not start job based on " + reqPacket);
            context.queuePacket(new RefPacket(reqPacket.job_id(), reqPacket.range_start(), reqPacket.range_end()));
            return;
        }
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
}
