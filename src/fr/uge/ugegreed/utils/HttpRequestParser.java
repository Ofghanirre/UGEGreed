package fr.uge.ugegreed.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class HttpRequestParser {
    public static String getRequestFromURL(String jarURL) throws MalformedURLException {
        URL url = new URL(jarURL);
        String host = url.getHost();
        String path = url.getPath();

        return "GET " + path + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Accept: */*\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }
}
