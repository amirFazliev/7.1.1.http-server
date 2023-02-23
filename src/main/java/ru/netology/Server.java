package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private int port;

    private Socket socket;

    private BufferedReader in;

    private BufferedOutputStream out;

    private List<String> validPaths;

    public Server (int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
    }

    public void startServer (Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public String getMessage () throws IOException {
        return in.readLine();
    }

    public void setValidPath(List<String> validPath) {
        this.validPaths = validPath;
    }
    public void messageProcessing (String message) throws IOException {
        final var parts = message.split(" ");

        if (parts.length != 3) {
            System.out.println("Неправильная форма сообщения(должно быть три элемента разделенных пробелом)");
            return;
        }

        final var path = parts[1];
        if (pathIsNotValidPath(path)) {
            System.out.println("Ошибка 404 Not Found - в запросе не указан путь");
            return;
        }

        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        if (pathIsClassicHtml(path, filePath, mimeType)) {
            return;
        }

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    public boolean pathIsNotValidPath (String path) throws IOException {
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return true;
        }
        return false;
    }

    public boolean pathIsClassicHtml (String path, Path filePath, String mimeType) throws IOException {
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            return true;
        }
        return false;
    }
}


