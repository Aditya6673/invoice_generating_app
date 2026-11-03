package com.rishav.billing.document.service;

public interface StorageService {

    String store(String path, byte[] data);

    byte[] retrieve(String path);

    void delete(String path);
}
