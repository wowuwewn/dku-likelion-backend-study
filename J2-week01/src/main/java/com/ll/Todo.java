package com.ll;

public class Todo {
    private long id;

    private String content;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Todo(long id, String content) {
        this.id = id;
        this.content = content;
    }
}
