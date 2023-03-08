package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.netology.Server.getHandler;

public class MessageProcessor implements Runnable {
    private Path pathPackage;
    private final Socket socketClient;
    private final BufferedReader in;
    private final BufferedOutputStream out;
    private StringBuilder sb = new StringBuilder();
    private Handler handler;

    public MessageProcessor(Path path, Socket socket) throws IOException {
        this.pathPackage = path;
        this.socketClient = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());
    }



    @Override
    public void run() {
        try (socketClient; in; out) {
            String[] message = parsMessage(getMessage());
            String method = message[0];
            String path = message[1];
            String body = extract();

            Request request = new Request(method, path, body);
            this.handler = getHandler(method, path);
            if (handler != null) {
                handler.handle(request, out);
            }
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] parsMessage(String message) throws IOException {
        var parts = message.split(" ");
        if (parts.length != 3) {
            System.out.println("Неправильная форма сообщения(должно быть три элемента разделенных пробелом)");
            return null;
        }

        final var method = parts[0];
        if (!methodIsNotValidMethod(method)) {
            System.out.println("Ошибка 404 Not Found - в запросе не указан верный метод");
            return null;
        }

        final var path = parts[1];
        if (!pathIsNotValidPath(path)) {
            System.out.println("Ошибка 404 Not Found - в запросе не указан путь");
            return null;
        }

        return parts;
    }

    public String getMessage() throws IOException {
        return in.readLine();
    }
    private boolean pathIsNotValidPath(String path) throws IOException {
        var pathFiles = Path.of(String.valueOf(pathPackage), path);
        if (Files.exists(pathFiles) || !path.equals("/")) {
            return true;
        } else {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return false;
        }
    }

    private boolean methodIsNotValidMethod(String method) throws IOException {
        if (method.equals("GET") || method.equals("POST")) {
            return true;
        }
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
        return false;
    }

    private String extract() throws IOException {
        sb.setLength(0);
        String s = null;
        if (in.ready()) {
            while (true) {
                s = in.readLine();
                if (s == null || s.equals("")) {
                    break;
                }
                sb.append(s);
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }
}
