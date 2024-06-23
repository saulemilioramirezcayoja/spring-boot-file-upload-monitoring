package com.spring_boot_file_upload_monitoring.service;

import com.spring_boot_file_upload_monitoring.model.FileModel;
import com.spring_boot_file_upload_monitoring.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class UploadService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-size}")
    private long maxSize;

    @Value("#{'${file.allowed-types}'.split(',')}")
    private List<String> allowedTypes;

    private final FileRepository fileRepository;

    public UploadService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    private Path getUploadPath() {
        LocalDate today = LocalDate.now();
        String month = String.format("%02d", today.getMonthValue());
        String day = String.format("%02d", today.getDayOfMonth());
        return Paths.get(uploadDir, String.valueOf(today.getYear()), month, day);
    }

    private Path getUniqueFilePath(Path directory, String fileName) {
        Path filePath = directory.resolve(fileName);
        String fileBaseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String fileExtension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
        int counter = 1;
        while (Files.exists(filePath)) {
            String newFileName = fileBaseName + "_" + counter + fileExtension;
            filePath = directory.resolve(newFileName);
            counter++;
        }
        return filePath;
    }

    public String storeFile(String fileName, String base64) throws IOException {
        byte[] data = Base64.getDecoder().decode(base64);

        if (data.length > maxSize) {
            throw new IOException("File size exceeds the maximum allowed limit.");
        }

        String fileType = Files.probeContentType(Paths.get(fileName));
        if (!allowedTypes.contains(fileType)) {
            throw new IOException("File type is not allowed.");
        }

        Path uploadPath = getUploadPath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = getUniqueFilePath(uploadPath, fileName);
        Files.write(filePath, data);
        String normalizedPath = filePath.normalize().toString();

        FileModel fileModel = new FileModel();
        fileModel.setName(fileName);
        fileModel.setPath(normalizedPath);
        fileModel.setSize(data.length);
        fileModel.setType(fileType);
        fileModel.setUploadTime(LocalDateTime.now());
        fileRepository.save(fileModel);

        return normalizedPath;
    }

    public byte[] readFile(Long id) throws IOException {
        Optional<FileModel> fileModelOptional = fileRepository.findById(id);
        if (fileModelOptional.isPresent()) {
            FileModel fileModel = fileModelOptional.get();
            Path filePath = Paths.get(fileModel.getPath());
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                fileRepository.delete(fileModel);
                throw new IOException("File not found");
            }
        } else {
            throw new IOException("File not found");
        }
    }

    public FileModel getFileModelById(Long id) throws IOException {
        return fileRepository.findById(id).orElseThrow(() -> new IOException("File not found"));
    }
}