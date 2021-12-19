package com.github.esiqveland.crawler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

// FileUtils provides a way to tag files with a unique id to help track changes as
// files are moved around in the filesystem.
public class FileUtils {

    private static final String USER_ID_ATTRIBUTE = "user:objectid";
    private static final String USER_ID = "objectid";

    public static String generateFileId(Path path) throws IOException {
        String fileId = generateFileId();
        storeFileId(path, fileId);
        return fileId;
    }

    public static Optional<String> getId(Path path) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(
                path,
                UserDefinedFileAttributeView.class
        );

        try {
            List<String> userAttributes = view.list();

            if (userAttributes.contains(USER_ID)) {
                ByteBuffer buf = ByteBuffer.allocate(view.size(USER_ID));
                view.read(USER_ID, buf);
                buf.flip();
                var value = UTF_8.decode(buf).toString();

                return Optional.ofNullable(value);
            }
        } catch (IOException e) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static void storeFileId(Path path, String fileId) throws IOException {
        Files.setAttribute(path, USER_ID_ATTRIBUTE, fileId.getBytes(UTF_8));
    }

    public static String generateFileId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
