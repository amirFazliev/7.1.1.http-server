package ru.netology;

import org.apache.http.NameValuePair;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.netology.Main.extractPath;
import static ru.netology.Server.getHandler;

public class MessageProcessor implements Runnable {
    private final Socket socketClient;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;
    private StringBuilder sb = new StringBuilder();
    private Handler handler;

    public MessageProcessor(Socket socket) throws IOException {
        this.socketClient = socket;
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try (socketClient; in; out) {
            while (true) {
                final var limit = 4096;

                Request request = new Request(in, out, limit);

                if (request.isFlagProgramm()) {
                    break;
                }

                if (request.getPath().contains("?")) {
                    System.out.println(request.getQueryParams());
                    for (NameValuePair queryParam : request.getQueryParams()) {
                        System.out.println("Name - " + queryParam.getName() + "; Value - " + queryParam.getValue());
                    }
                    System.out.println(request.getQueryParam("fsfsdf"));
                    System.out.println(request.getQueryParam("last"));
                }

                System.out.println(request.getPostParams());
                System.out.println(request.getPostParam("login"));


                this.handler = getHandler(request.getMethod(), extractPath(request.getPath()));
                if (handler != null) {
                    System.out.println("handler use");
                    handler.handle(request, out);
                } else {
                    System.out.println("handler not use");
                }
                out.flush();
                break;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
