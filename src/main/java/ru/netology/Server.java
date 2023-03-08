package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ExecutorService threadPool = Executors.newFixedThreadPool(64);

    private static ConcurrentMap<String, ConcurrentMap<String, Handler>> handlersMap = new ConcurrentHashMap<>();

    public void startServer (int port) throws IOException {
        try (var serverSocket = new ServerSocket(port)) {
            var path = Path.of("./public");
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(new MessageProcessor(path, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Handler getHandler (String method, String path) {
        if (handlersMap.containsKey(method)) {
            if (handlersMap.get(method).containsKey(path)) {
                return handlersMap.get(method).get(path);
            }
        }
        return null;
    }

    public void addHandlers (String method, String message, Handler handler) {
        ConcurrentMap<String, Handler> handlerConcurrentMap = new ConcurrentHashMap<>();
        handlerConcurrentMap.put(message, handler);
        if (handlersMap.containsKey(method)) {
            if (handlersMap.get(method).containsKey(message)) {
                System.out.println("Method - " + method + " with handler is in package ./public" + message);
            } else {
                handlersMap.put(method, handlerConcurrentMap);
            }
        } else {
            handlersMap.put(method, handlerConcurrentMap);
        }
    }
}


