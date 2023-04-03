package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.Checker;
import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class Jobs {
    private static final int TASK_EXECUTOR_MAX_READING_AMOUNT = 128;
    private final Map<Long, Job> jobs = new HashMap<>();
    private final ArrayDeque<Packet> contextQueue = new ArrayDeque<>();
    private final ArrayBlockingQueue<AnsPacket> taskExecutorQueue = new ArrayBlockingQueue<>(TASK_EXECUTOR_MAX_READING_AMOUNT);
    private final TaskExecutor taskExecutor = new TaskExecutor(this.taskExecutorQueue);
    private static final Logger logger = Logger.getLogger(Jobs.class.getName());

    public void addPacket(Packet packet) {
        contextQueue.add(packet);
    }

    // TODO REMOVE LATER
    public void addJob(Checker checker, long job_id, long range_start, long range_end) throws IOException {
        jobs.put(job_id,
                new UpstreamJob(new ReqPacket(job_id, "test/debug", "main", range_start, range_end),
                Path.of("response/test")));
        taskExecutor.addJob(checker, job_id, range_start, range_end);
    }

    private void sendPacketToJob(Packet packet, long job_id) {
        var job = jobs.get(job_id);
        logger.info(packet.toString());
        if (job == null) {
            logger.warning("Invalid Job_id given " + job_id);
            return;
        }
        job.handlePacket(packet);
    }
    public void processContextQueue() {
        while (!contextQueue.isEmpty()) {
            Packet packet = contextQueue.remove();
            switch (packet) {
                case AnsPacket ansPacket -> sendPacketToJob(ansPacket, ansPacket.job_id());
                case AccPacket accPacket -> sendPacketToJob(accPacket, accPacket.job_id());
                case RefPacket refPacket -> sendPacketToJob(refPacket, refPacket.job_id());
                default -> throw new AssertionError("unhandled Packet tested");
            }
        }
    }

    public void processTaskExecutorQueue() {
        for (int readCounter = 0; readCounter < TASK_EXECUTOR_MAX_READING_AMOUNT; readCounter++){
            AnsPacket packet = taskExecutorQueue.poll();
            if (packet == null) break;
            sendPacketToJob(packet, packet.job_id());
        }
    }
}
