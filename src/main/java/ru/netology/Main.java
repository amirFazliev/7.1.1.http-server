package ru.netology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
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

        int port = 9999;

        Server server = new Server();

        // TODO example
        server.addHandlers("GET", fullPathOneInClassicHtml, handlerIsClassicHtml());
        server.addHandlers("POST", fullPathThree, handlerIsOtherPath());

        server.startServer(port);
    }

    public static Handler handlerIsClassicHtml () {
        Handler handler = (request, bos) -> {
            Path filePath = Path.of("./public", request.getPath());
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
            Path filePath = Path.of("./public", request.getPath());
            try {
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                bos.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
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
}


