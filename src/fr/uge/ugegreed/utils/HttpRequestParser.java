package fr.uge.ugegreed.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class HttpRequestParser {
    public static String getRequestFromURL(String jarURL) throws MalformedURLException {
        URL url = new URL(jarURL);
        String host = url.getHost();
        int port = url.getPort();
        if (port == -1) port = 80;
        String path = url.getPath();

        return "GET " + path + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Accept: */*\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    public static boolean testHttpHeaderCode(int code, Logger logger) {
        if (code == 200) return true;
        String message = "HTTP Request resulted in code " + code + "; Cause:" +
                switch (code) {
                    case 400 -> "Bad Request";
                    case 401 -> "Unauthorized";
                    case 403 -> "Forbidden";
                    case 404 -> "Not Found";
                    case 500 -> "Internal Server Error";
                    default -> " unknown";
                };

        logger.warning(message);
        return false;
    }
}
