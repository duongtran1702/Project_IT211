package atmin.controller.file;

import atmin.common.response.ApiResponse;
import atmin.infrastructure.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/manager/files/upload")
    public ResponseEntity<ApiResponse<String>> uploadManagerFile(@RequestParam("file") MultipartFile file) {
        String secureUrl = uploadService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", secureUrl));
    }

    @PostMapping("/customer/files/upload")
    public ResponseEntity<ApiResponse<String>> uploadCustomerFile(@RequestParam("file") MultipartFile file) {
        String secureUrl = uploadService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", secureUrl));
    }

    @PostMapping({"/admin/files/upload/courts/{id}", "/manager/files/upload/courts/{id}"})
    public ResponseEntity<ApiResponse<String>> uploadCourtImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        String secureUrl = uploadService.uploadFile(file, id);
        return ResponseEntity.ok(ApiResponse.success("Court image updated successfully", secureUrl));
    }
}
