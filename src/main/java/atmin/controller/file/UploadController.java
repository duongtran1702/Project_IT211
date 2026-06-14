package atmin.controller.file;

import atmin.common.response.ApiResponse;
import atmin.infrastructure.upload.UploadService;
import atmin.service.ICourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final ICourtService courtService;

    @PostMapping("/manager/files/upload")
    public ResponseEntity<ApiResponse<String>> uploadManagerFile(@RequestParam("file") MultipartFile file) {
        String secureUrl = uploadService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", secureUrl));
    }

    @PostMapping("/manager/files/upload/courts/{id}")
    public ResponseEntity<ApiResponse<String>> uploadCourtImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String secureUrl = courtService.uploadCourtImage(id, file);
        return ResponseEntity.ok(ApiResponse.success("Court image updated successfully", secureUrl));
    }
}
