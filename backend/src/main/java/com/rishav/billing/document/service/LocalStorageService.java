package com.rishav.billing.document.service;

import com.rishav.billing.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local-path:./storage}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not initialize storage directory at " + this.rootLocation, e);
        }
    }

    @Override
    public String store(String path, byte[] data) {
        try {
            Path targetPath = this.rootLocation.resolve(path).normalize();
            if (!targetPath.startsWith(this.rootLocation)) {
                throw new IllegalArgumentException("Cannot store file outside current storage directory: " + path);
            }
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.write(targetPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file: " + path, e);
        }
    }

    @Override
    public byte[] retrieve(String path) {
        try {
            Path targetPath = this.rootLocation.resolve(path).normalize();
            if (!targetPath.startsWith(this.rootLocation)) {
                throw new IllegalArgumentException("Cannot retrieve file outside storage directory: " + path);
            }
            if (!Files.exists(targetPath)) {
                throw new ResourceNotFoundException("File", "path", path);
            }
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path targetPath = this.rootLocation.resolve(path).normalize();
            if (!targetPath.startsWith(this.rootLocation)) {
                throw new IllegalArgumentException("Cannot delete file outside storage directory: " + path);
            }
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file: " + path, e);
        }
    }
}
