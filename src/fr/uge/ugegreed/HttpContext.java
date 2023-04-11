package fr.uge.ugegreed;

import fr.uge.ugegreed.http.HttpHeader;
import fr.uge.ugegreed.readers.HttpHeaderReader;
import fr.uge.ugegreed.utils.HttpRequestParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public final class HttpContext implements Context {
    private static final Logger logger = Logger.getLogger(HttpHeader.class.getName());
    private static final int BUFFER_SIZE = 16384;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferSend;
    private final ByteBuffer bufferReceive = ByteBuffer.allocate(BUFFER_SIZE);
    private final HttpHeaderReader httpHeaderReader = new HttpHeaderReader();
    private boolean closed = false;


    public HttpContext(SelectionKey key, String jar_URL) throws MalformedURLException {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.bufferSend = StandardCharsets.US_ASCII.encode(HttpRequestParser.getRequestFromURL(jar_URL));
    }

    public void processRead() {
        for (;;) {
            var status = httpHeaderReader.process(bufferReceive);
            switch (status) {
                case DONE -> {
                    HttpHeader httpHeader = httpHeaderReader.get();
                    if (HttpRequestParser.testHttpHeaderCode(httpHeader.getCode(), logger)) {
                        closed = true;
                        logger.info(httpHeader.toString());
                    }
                }
                case REFILL -> { return ; }
                case ERROR -> {
                    silentlyClose();
                    return;
                }
            }
        }
    }

    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException
     */
    public void doRead() throws IOException {
        var byteRead = sc.read(bufferReceive);
        if (byteRead == -1) {
            closed = true;
        }
        processRead();
        updateInterestOps();
    }

    /**
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doWrite and after the call
     *
     * @throws IOException
     */
    public void doWrite() throws IOException {
        bufferSend.flip();
        sc.write(bufferSend);
        bufferSend.compact();
        updateInterestOps();
    }

    /**
     * Update the interestOps of the key looking only at values of the boolean
     * closed and of both ByteBuffers.
     * The convention is that both buffers are in write-mode before the call to
     * updateInterestOps and after the call. Also it is assumed that process has
     * been be called just before updateInterestOps.
     * --
     * While bufferReceive can accept data we stay in OP_READ mode
     * While bufferSend has data left to send we stay in OP_WRITE mode
     *
     */
    private void updateInterestOps() {
        int ops = 0;
        if (!closed && bufferReceive.hasRemaining()) {
            ops |= SelectionKey.OP_READ;
        }
        bufferSend.flip();
        if (!closed && bufferSend.hasRemaining()) {
            ops |= SelectionKey.OP_WRITE;
        }
        bufferSend.compact();
        key.interestOps(ops);
    }

    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException ignore) {}
    }

    public void doConnect() throws IOException {
        if (!sc.finishConnect()) return;
        key.interestOps(SelectionKey.OP_WRITE);
        logger.info("Connected to HTTP server...");
    }
}
