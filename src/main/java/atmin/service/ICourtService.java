package atmin.service;

import org.springframework.web.multipart.MultipartFile;

public interface ICourtService {
    String uploadCourtImage(Long courtId, MultipartFile file);
}
