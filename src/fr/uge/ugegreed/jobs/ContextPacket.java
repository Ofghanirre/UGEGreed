package fr.uge.ugegreed.jobs;

import fr.uge.ugegreed.ConnectionContext;
import fr.uge.ugegreed.packets.Packet;

import java.util.Objects;

public record ContextPacket(Packet packet, ConnectionContext context) {

    public ContextPacket{
        Objects.requireNonNull(packet);
        Objects.requireNonNull(context);
    }
}

