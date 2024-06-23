package com.spring_boot_file_upload_monitoring.service;

import com.spring_boot_file_upload_monitoring.model.FileModel;
import com.spring_boot_file_upload_monitoring.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(FileMonitorService.class);
    private final FileRepository fileRepository;
    private FileAlterationMonitor monitor;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileMonitorService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @PostConstruct
    public void init() {
        FileAlterationObserver observer = new FileAlterationObserver(uploadDir);
        observer.addListener(new FileAlterationListener() {
            @Override
            public void onFileDelete(File file) {
                logger.info("File deleted event detected: {}", file.getAbsolutePath());
                checkDatabaseFiles();
                checkOrphanFiles();
            }

            @Override
            public void onStart(FileAlterationObserver observer) {
                logger.info("File monitor started.");
                checkOrphanFiles();
            }

            @Override
            public void onDirectoryCreate(File directory) {
                logger.info("Directory created: {}", directory.getAbsolutePath());
            }

            @Override
            public void onDirectoryChange(File directory) {
                logger.info("Directory changed: {}", directory.getAbsolutePath());
                checkOrphanFiles();
            }

            @Override
            public void onDirectoryDelete(File directory) {
                logger.info("Directory deleted: {}", directory.getAbsolutePath());
            }

            @Override
            public void onFileCreate(File file) {
                logger.info("File created: {}", file.getAbsolutePath());
                checkOrphanFiles();
            }

            @Override
            public void onFileChange(File file) {
                logger.info("File changed: {}", file.getAbsolutePath());
                checkOrphanFiles();
            }

            @Override
            public void onStop(FileAlterationObserver observer) {
                logger.info("File monitor stopped.");
            }
        });

        monitor = new FileAlterationMonitor(10000, observer);
        try {
            monitor.start();
            logger.info("File monitor started successfully.");
        } catch (Exception e) {
            logger.error("Error starting file monitor", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (monitor != null) {
            try {
                monitor.stop();
                logger.info("File monitor stopped successfully.");
            } catch (Exception e) {
                logger.error("Error stopping file monitor", e);
            }
        }
    }

    private void checkDatabaseFiles() {
        List<FileModel> allFiles = fileRepository.findAll();
        for (FileModel fileModel : allFiles) {
            Path filePath = Paths.get(fileModel.getPath());
            if (!Files.exists(filePath)) {
                logger.info("File not found on disk, deleting record from database: {}", filePath);
                fileRepository.delete(fileModel);
            }
        }
    }

    private void checkOrphanFiles() {
        try (Stream<Path> paths = Files.walk(Paths.get(uploadDir))) {
            paths.filter(Files::isRegularFile).forEach(filePath -> {
                if (!fileRepository.existsByPath(filePath.toString())) {
                    try {
                        Files.delete(filePath);
                        logger.info("Orphan file deleted: {}", filePath);
                    } catch (Exception e) {
                        logger.error("Error deleting orphan file: {}", filePath, e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Error checking orphan files", e);
        }
    }
}