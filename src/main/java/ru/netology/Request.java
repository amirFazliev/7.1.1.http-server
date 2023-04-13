package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {

    private String[] request;
    private String method;
    private String path;
    private String protocol;
    private List<String> headers;
    private String messageBody;
    private List<NameValuePair> listUrlParameters;
    private List<String> postParams = new LinkedList<>();
    private List<List<String>> listMultipart = new LinkedList<>();
    private String fileName;
    private String fileString;
    private File fileMultiPart;
    private boolean flagProgramm = false;
    private String boundaryForMultipartFormData;
    private String contentType;

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

    public void setContentType() {
        for (String header : headers) {
            if (header.contains("Content-Type")) {
                String[] str = header.split(" ");
                if (str[1].contains("multipart/form-data")) {
                    contentType = str[1].substring(0, str[1].length() - 1);
                    boundaryForMultipartFormData = str[2].substring("boundary=".length());
                } else {
                    contentType = str[1];
                }
                return;
            }
        }
    }

    public String getContentType() {
        return contentType;
    }

    public File getFileMultiPart() {
        return fileMultiPart;
    }

    public String getFileString() {
        return fileString;
    }

    public void setFileMultiPart(File fileMultiPart) {
        this.fileMultiPart = fileMultiPart;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
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
        if (!pathIsNotValidPath(path)) {
            System.out.println("Ошибка 404 Not Found - в запросе не указан путь");
            setFlagProgramm(true);
            return;
        }
        this.path = path;
        System.out.println("_________________");
        System.out.println(path);
        System.out.println("_________________");

        this.protocol = parts[2];

        if (path.contains("?")) {
            String urlParam = path.substring(path.indexOf("?") + 1);
            listUrlParameters = URLEncodedUtils.parse(urlParam, StandardCharsets.UTF_8);
        }
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

        setContentType();

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

            if (contentType.equals("application/x-www-form-urlencoded")){
                postParams.addAll(Arrays.asList(messageBody.split("&")));
            }

            if (contentType.equals("multipart/form-data")) {
                String splits = "--" + boundaryForMultipartFormData + "\r\n";
                String splitEnd = splits + "--" + "\r\n";
                String text = messageBody.substring(splits.length(), (messageBody.length() - splitEnd.length()));
                for (String s : text.split(splits)) {
                    String[] parsText = s.split("\r\n\r\n");
                    List<String> list = new ArrayList<>();
                    if (s.contains("Content-Type: ")) {
                        String fileNameBasic = s.substring(s.indexOf("filename=") + "filename=".length() + 1);
                        fileName = fileNameBasic.substring(0, fileNameBasic.indexOf("\""));
                        list.add(fileName);
                        fileString = parsText[1];
                        list.add(fileString);
                        fileMultiPart = new File(fileName);
                        try (FileOutputStream fos = new FileOutputStream(fileMultiPart)) {
                            byte[] bytes = fileString.getBytes();
                            fos.write(bytes, 0, bytes.length);
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                        listMultipart.add(list);
                    } else {
                        String fileStart = parsText[0].substring(s.indexOf("name"));
                        String strFirst = fileStart.substring("name".length() + 2, fileStart.lastIndexOf("\""));
                        list.add(strFirst);
                        String strEnd = parsText[1].substring(0, parsText[1].length() - 2);
                        list.add(strEnd);
                        listMultipart.add(list);
                    }
                }
            }
        }
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

    public List<NameValuePair> getQueryParams() {
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

    public List<String> getPostParams() {
        return postParams;
    }

    public List<List<String>> getPostParam(String name) {
        List<List<String>> bigList = new LinkedList<>();
        for (String postParam : postParams) {
            List<String> list = new LinkedList<>();
            String first = postParam.substring(0, postParam.indexOf("="));
            if (first.equals(name)) {
                String end = postParam.substring(postParam.indexOf("=") + 1);
                list.add(first);
                list.add(end);
                bigList.add(list);
            }
        }
        return bigList;
    }

    public List<List<String>> getParts() {
        return listMultipart;
    }

    public List<List<String>> getPart(String name) {
        List<List<String>> bigList = new LinkedList<>();
        for (List<String> listPart : listMultipart) {
            List<String> list = new LinkedList<>();
            if (listPart.get(0).equals(name)) {
                list.add(listPart.get(0));
                list.add(listPart.get(1));
                bigList.add(list);
            }
        }
        return bigList;
    }
}
