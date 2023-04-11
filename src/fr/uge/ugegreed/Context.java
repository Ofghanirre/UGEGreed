package fr.uge.ugegreed;

import java.io.IOException;

public sealed interface Context permits ConnectionContext, HttpContext {
  void doConnect() throws IOException;
  void doWrite() throws IOException;
  void doRead() throws IOException;
}
