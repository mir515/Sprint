package framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import jakarta.servlet.http.Part;

public class FileUpload {
    public String fileName;
    public String fileType;
    public long fileSize;
    public byte[] fileData;

    public FileUpload(String fileName, String fileType, long fileSize, byte[] fileData) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileData = fileData;
    }

    public static FileUpload fromPart(Part part) throws IOException {
        if (part == null) {
            throw new IllegalArgumentException("La partie de fichier ne peut pas être nulle");
        }

        byte[] data;
        try (InputStream input = part.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            data = output.toByteArray();
        }

        return new FileUpload(part.getSubmittedFileName(), part.getContentType(), data.length, data);
    }

    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
        this.fileSize = fileData == null ? 0 : fileData.length;
    }

    /** Writes the bytes to the exact path selected by the caller. */
    public void save(String filePath) throws IOException {
        if (fileData == null) {
            throw new IllegalStateException("Aucun contenu binaire à enregistrer");
        }
        Path path = Paths.get(filePath);
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, fileData);
    }

    /** Stores the file under a generated name in the given directory. */
    public Path saveTo(Path directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Le dossier de destination ne peut pas être nul");
        }
        String safeName = fileName == null ? "upload" : fileName.replace('\\', '/');
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.isEmpty() || ".".equals(safeName) || "..".equals(safeName)) {
            safeName = "upload";
        }
        Files.createDirectories(directory);
        Path destination = directory.resolve(UUID.randomUUID().toString() + "-" + safeName);
        save(destination.toString());
        return destination;
    }
}
