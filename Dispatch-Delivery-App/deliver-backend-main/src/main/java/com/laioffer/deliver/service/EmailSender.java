package com.laioffer.deliver.service;

public interface EmailSender {
    void send(String to, String subject, String content);
}
