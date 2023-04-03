package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.TaskExecutor;
import fr.uge.ugegreed.packets.AccPacket;
import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.packets.RefPacket;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class Jobs {
    private final Map<Long, Job> jobs = new HashMap<>();
    private final ArrayDeque<Packet> contextQueue = new ArrayDeque<>();
    private final ArrayBlockingQueue<AnsPacket> taskExecutorQueue = new ArrayBlockingQueue<>(1_000_000);
    private final TaskExecutor taskExecutor = new TaskExecutor(this.taskExecutorQueue);
    private static final Logger logger = Logger.getLogger(Jobs.class.getName());
    public void addPacket(Packet packet) {
        contextQueue.add(packet);
    }

    private void sendPacketToJob(Packet packet, long job_id) {
        var job = jobs.get(job_id);
        if (job == null) {
            logger.warning("Invalid Job_id given " + job_id);
            return;
        }
        job.handlePacket(packet);
    }
    public void processQueue() {
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
}
