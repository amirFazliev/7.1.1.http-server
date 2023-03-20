package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Request {

    private String[] request;
    private String method;

    private String path;

    private String protocol;

    private List<String> headers;

    private String messageBody;

    private List<NameValuePair> listUrlParameters;

    private boolean flagProgramm = false;

    public Request(BufferedInputStream in, BufferedOutputStream out, int limit) throws IOException {
        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

        this.request = requestLine;
        parsMessage(request, out);

        if (isFlagProgramm()) {
            return;
        }

        chooseHeaders(in, out, requestLineDelimiter, requestLineEnd, buffer, read);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public boolean isFlagProgramm() {
        return flagProgramm;
    }

    public void setFlagProgramm(boolean flagProgramm) {
        this.flagProgramm = flagProgramm;
    }

    private void parsMessage(String[] parts, BufferedOutputStream out) throws IOException {
        if (parts.length != 3) {
            System.out.println("Неправильная форма сообщения(должно быть три элемента разделенных пробелом)");
            badRequest(out);
            setFlagProgramm(true);
            return;
        }

        final var method = parts[0];
        if (methodIsNotValidMethod(method)) {
            System.out.println("Ошибка 404 Not Found - в запросе не указан верный метод");
            badRequest(out);
            setFlagProgramm(true);
            return;
        }
        this.method = method;
        System.out.println("_________________");
        System.out.println(method);
        System.out.println("_________________");

        final var path = parts[1];
        if (pathIsNotValidPath(path)) {
            System.out.println("Ошибка 404 Not Found - в запросе не указан путь");
            setFlagProgramm(true);
            return;
        }
        this.path = path;
        System.out.println("_________________");
        System.out.println(path);
        System.out.println("_________________");

        this.protocol = parts[2];
    }

    private boolean pathIsNotValidPath(String path) {
        if (path.equals("/")) {
            return false;
        }
        return true;
    }

    private boolean methodIsNotValidMethod(String method) {
        if (method.equals("GET") || method.equals("POST")) {
            return false;
        }
        return true;
    }

    public void chooseHeaders(BufferedInputStream in, BufferedOutputStream out, byte[] requestLineDelimiter, int requestLineEnd, byte[] buffer, int read) throws IOException {
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            System.out.println("Ошибка в headers");
            setFlagProgramm(true);
            return;
        }

        in.reset();
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        this.headers = headers;

        System.out.println("_________________");
        System.out.println(headers);
        System.out.println("_________________");

        if (!method.equals("GET")) {
            chooseBody(in, headersDelimiter);
        }
    }

    public void chooseBody(BufferedInputStream in, byte[] headersDelimiter) throws IOException {
        in.skip(headersDelimiter.length);
        final var contentLength = extractHeader(headers, "Content-Length");
        if (contentLength.isPresent()) {
            final var length = Integer.parseInt(contentLength.get());
            final var bodyBytes = in.readNBytes(length);

            final var body = new String(bodyBytes);
            this.messageBody = body;

            System.out.println("_________________");
            System.out.println(body);
            System.out.println("_________________");
        }
    }

    public List<NameValuePair> getQueryParams() {
        String urlParam = path.substring(path.indexOf("?") + 1);
        listUrlParameters = URLEncodedUtils.parse(urlParam, StandardCharsets.UTF_8);
        return listUrlParameters;
    }

    public String getQueryParam(String name) {
        for (NameValuePair listUrlParameter : listUrlParameters) {
            if (listUrlParameter.getName().equals(name)) {
                return listUrlParameter.getValue();
            }
        }
        return null;
    }

    public static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim) // удаляет пробелы в начале и в конце строки
                .findFirst(); // находит и возвращает первый результат
    }

    public static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    // from google guava with modifications
    public static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


}
