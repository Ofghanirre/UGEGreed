package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.packets.ReqPacket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UpstreamJob implements Job {
    private final ReqPacket request;
    private final BufferedWriter outputFile;

    public UpstreamJob(ReqPacket request, Path outputFilePath) throws IOException {
        this.request = request;
        this.outputFile = Files.newBufferedWriter(outputFilePath);
    }

    public void end() throws IOException {
        this.outputFile.close();
    }

    @Override
    public void handlePacket(Packet packet) {
        switch (packet) {
            case AnsPacket ansPacket -> handleAnswer(ansPacket);
            default -> throw new AssertionError();
        }
    }

    private void handleAnswer(AnsPacket ansPacket) {
    }
}
