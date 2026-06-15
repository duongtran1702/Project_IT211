package atmin.service.impl;

import atmin.common.exception.ResourceNotFoundException;
import atmin.entity.Court;
import atmin.infrastructure.upload.UploadService;
import atmin.repository.CourtRepository;
import atmin.service.ICourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CourtService implements ICourtService {
    private final CourtRepository courtRepository;
    private final UploadService uploadService;

    @Override
    @Transactional
    public String uploadCourtImage(Long courtId, MultipartFile file) {
        if (courtId == null) {
            throw new IllegalArgumentException("Court ID must not be null");
        }

        // 1. Kiểm duyệt thông tin Sân trước khi thực hiện hành vi tải ảnh lên hạ tầng
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + courtId));
        if (court.isDeleted()) {
            throw new ResourceNotFoundException("Court not found with id: " + courtId);
        }

        // 2. Upload file lên Cloudinary
        String secureUrl = uploadService.uploadFile(file);

        // 3. Cập nhật URL ảnh và lưu lại cơ sở dữ liệu
        court.setImageUrl(secureUrl);
        courtRepository.save(court);

        return secureUrl;
    }
}
