package ru.netology;

public class Request {
    private String method;

    private String path;

    private String messageBody;



    public Request(String method, String path, String messageBody) {
        this.method = method;
        this.path = path;
        this.messageBody = messageBody;
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

}
