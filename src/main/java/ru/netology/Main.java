package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    static int port = 9999;
    public static void main(String[] args) throws IOException {

        String fullPathOneInClassicHtml = "/classic.html";
        String fullPathTwo = "/spring.svg";
        String fullPathThree = "/spring.png";
        String fullPathFour = "/resources.html";
        String fullPathFive = "/styles.css";
        String fullPathSix = "/app.js";
        String fullPathSeven = "/links.html";
        String fullPathEight = "/forms.html";
        String fullPathNine = "/index.html";
        String fullPathTen = "/events.html";
        String fullPathEleven = "/events.js";


        Server server = new Server();

        // TODO example
//        server.addHandlers("GET", fullPathOneInClassicHtml, handlerIsClassicHtml());
//        server.addHandlers("POST", fullPathThree, handlerIsOtherPath());

        server.addHandlers("GET", fullPathEight, handlerIsOtherPath());
        server.addHandlers("POST", fullPathEight, handlerIsOtherPath());

        server.startServer(port);
    }

    public static Handler handlerIsClassicHtml () {
        Handler handler = (request, bos) -> {
            Path filePath = Path.of("./public", extractPath(request.getPath()));
            try {
                final var template = Files.readString(filePath);
                final var mimeType = Files.probeContentType(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                bos.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                bos.write(content);
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return handler;
    }

    public static Handler handlerIsOtherPath () {
        Handler handler = (request, bos) -> {
            Path filePath = Path.of("./public", extractPath(request.getPath()));
            try {
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                bos.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: "  + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, bos);
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return handler;
    }

    public static String extractPath(String path) {
        int first = path.startsWith("/") ? 0 : (path.lastIndexOf("/"));
        int end = path.endsWith("?") ? path.length() : path.indexOf("?");

        String text = path.substring(first, end);

        return text;
    }
}


