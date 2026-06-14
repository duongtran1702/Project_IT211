package atmin.infrastructure.upload;

import com.cloudinary.Cloudinary;
import atmin.common.exception.CloudStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // Validate file size (under 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the 5MB limit");
        }

        // Validate file format (PNG, JPG, JPEG)
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        boolean isValidType = (contentType != null && (contentType.equals("image/png") || contentType.equals("image/jpeg") || contentType.equals("image/jpg")))
                || (originalFilename != null && (originalFilename.toLowerCase().endsWith(".png") || originalFilename.toLowerCase().endsWith(".jpg") || originalFilename.toLowerCase().endsWith(".jpeg")));

        if (!isValidType) {
            throw new IllegalArgumentException("Invalid file format. Only PNG, JPG, and JPEG files are allowed.");
        }

        try {
            String fileName = file.getOriginalFilename();

            if (fileName == null || fileName.isBlank()) {
                fileName = "file_" + System.currentTimeMillis();
            } else if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }

            Map<String, Object> uploadParams = Map.of(
                    "public_id", fileName
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult =
                    (Map<String, Object>) cloudinary.uploader()
                            .upload(file.getBytes(), uploadParams);

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new CloudStorageException("Cloud storage service is temporarily unavailable. Please try again later.", e);
        }
    }
}