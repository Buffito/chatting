package com.unipi.p15173.mywallchat;

public class Message {
    private String id;
    private String text;
    private String name;
    private String photoUrl;
    private String url;

    public Message(String text, String name, String photoUrl, String url) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
