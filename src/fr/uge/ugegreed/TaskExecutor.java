package fr.uge.ugegreed;

import fr.uge.ugegreed.packets.AnsPacket;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {
    private final ExecutorService executorService;
    private final ArrayBlockingQueue<AnsPacket> queue;
    public TaskExecutor(ArrayBlockingQueue<AnsPacket> queue) {
        Objects.requireNonNull(queue);
        this.queue = queue;
        this.executorService = Executors.newCachedThreadPool();
    }
    public void addJob(Checker checker, long job_id, long range_start, long range_end) {
        executorService.submit(() -> {
            for (long value = range_start; value < range_end; value++) {
                try {
                    queue.put(new AnsPacket(job_id, value, checker.check(value)));
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }
}
