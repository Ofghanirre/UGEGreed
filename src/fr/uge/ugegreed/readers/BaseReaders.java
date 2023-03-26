package fr.uge.ugegreed.readers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

/**
 * Contains factories for base readers
 */
public class BaseReaders {

  /**
   * Returns a reader for single bytes
   * @return reader for single bytes
   */
  public static Reader<Byte> byteReader() {
    return new BaseReader<>(Byte.BYTES, ByteBuffer::get);
  }

  /**
   * Returns a reader for integers
   * @return reader for integers
   */
  public static Reader<Integer> intReader() {
    return new BaseReader<>(Integer.BYTES, ByteBuffer::getInt);
  }

  /**
   * Returns a reader for longs
   * @return reader for longs
   */
  public static Reader<Long> longReader() {
    return new BaseReader<>(Long.BYTES, ByteBuffer::getLong);
  }

  /**
   * Returns a reader for hosts (IPv4 + port)
   * @return reader for hosts
   */
  public static Reader<InetSocketAddress> hostReader() {
    return new BaseReader<>(6, byteBuffer -> {
      var ip = new byte[4];
      IntStream.range(0, 4).forEach(i -> ip[i] = byteBuffer.get());
      var port = byteBuffer.getShort();
      try {
        return new InetSocketAddress(InetAddress.getByAddress(ip), port);
      } catch (UnknownHostException e) {
        // ip array is necessarily of the correct size
        throw new AssertionError();
      }
    });
  }

  public static void main(String[] args) {
//    var buffer = ByteBuffer.allocate(Integer.BYTES).putInt(2_000_000_000);
//    var reader = intReader();
//    reader.process(buffer);
//    System.out.println(reader.get());

//    var buffer = ByteBuffer.allocate(Integer.BYTES).put((byte)69);
//    var reader = byteReader();
//    reader.process(buffer);
//    System.out.println(reader.get());

    byte[] ip = {127, 0, 0, 1};
    var buffer = ByteBuffer.allocate(6).put(ip).putShort((short) 7777);
    var reader = hostReader();
    reader.process(buffer);
    var address = reader.get();
    System.out.println(address.getAddress() + " " + address.getPort());

  }
}
