package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

public interface Packet {
  ByteBuffer toBuffer();
}
