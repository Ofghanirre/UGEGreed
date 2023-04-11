package fr.uge.ugegreed;

import fr.uge.ugegreed.http.HttpHeader;
import fr.uge.ugegreed.jobs.Job;
import fr.uge.ugegreed.readers.HttpHeaderReader;
import fr.uge.ugegreed.utils.HttpRequestParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Context for managing the download of a JAR file
 */
public final class HttpContext implements Context {
    private enum State {
        WAITING_HEADER, WAITING_BODY, DONE
    }
    private static final Logger logger = Logger.getLogger(HttpHeader.class.getName());
    private static final int BUFFER_SIZE = 16384;
    private final SelectionKey key;
    private final Job job;
    private final SocketChannel sc;
    private final ByteBuffer bufferSend;
    private final ByteBuffer bufferReceive = ByteBuffer.allocate(BUFFER_SIZE);
    private final HttpHeaderReader httpHeaderReader = new HttpHeaderReader();
    private boolean closed = false;
    private State state = State.WAITING_HEADER;
    private final Path jarPath;
    private int toRead;
    private WritableByteChannel fileOutput;

    /**
     * Creates a new HTTP Context
     * @param key selection key this is associated to
     * @param jarURL URL to the jar
     * @param jarFolder folder where to store JARs
     * @param job job which asked for the download
     * @throws MalformedURLException
     */
    public HttpContext(SelectionKey key, String jarURL, Path jarFolder, Job job) throws MalformedURLException {
        this.key = Objects.requireNonNull(key);
        this.sc = (SocketChannel) key.channel();
        this.bufferSend = StandardCharsets.US_ASCII.encode(HttpRequestParser.getRequestFromURL(jarURL)).compact();
        this.job = Objects.requireNonNull(job);

        var path = new URL(jarURL).getPath();
        var lastSlash = path.lastIndexOf("/");
        if (lastSlash == -1 || lastSlash == path.length() - 1) { throw new MalformedURLException(); }
        var filename = path.substring(lastSlash + 1);
        jarPath = jarFolder.resolve(filename);
    }

    public void processRead() throws IOException {
        for (; ; ) {
            if (state == State.WAITING_HEADER) {
                var status = httpHeaderReader.process(bufferReceive);
                switch (status) {
                    case DONE -> {
                        HttpHeader httpHeader = httpHeaderReader.get();
                        logger.info("HEADER: " + httpHeader);
                        prepareBody();
                        if (httpHeader.getCode() != 200 || httpHeader.getContentLength() == -1) {
                            job.jarDownloadFail();
                        }

                        toRead = httpHeader.getContentLength();
                        state = State.WAITING_BODY;
                    }
                    case REFILL -> {
                        return;
                    }
                    case ERROR -> {
                        silentlyClose();
                        return;
                    }
                }
            } else if (state == State.WAITING_BODY) {
                bufferReceive.flip();
                var bytesWritten = fileOutput.write(bufferReceive);
                toRead -= bytesWritten;
                bufferReceive.compact();
                if (toRead == 0) {
                    closeBody();
                    silentlyClose();
                    closed = true;
                    state = State.DONE;
                    job.jarDownloadSuccess(jarPath);
                }
                return;
            } else {
                return;
            }
        }
    }

    private void prepareBody() throws IOException {
        var stream = Files.newOutputStream(jarPath);
        fileOutput = Channels.newChannel(stream);
    }

    private void closeBody() throws IOException {
        fileOutput.close();
    }

    /**
     * Performs the read action on sc
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
        if (!key.isValid()) { return ;}
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
        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        logger.info("Connected to HTTP server...");
    }
}
