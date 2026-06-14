package atmin.service.Impl;

import atmin.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.from-name}")
    private String fromName;

    @Override
    public void sendResetPasswordEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("🏸 Đặt lại mật khẩu tài khoản cầu lông của bạn");

            String content = "<h3>Xin chào,</h3>"
                    + "<p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản đăng ký bằng email này.</p>"
                    + "<p>Vui lòng sử dụng mã thông báo reset (Reset Token) dưới đây để thực hiện thay đổi mật khẩu của bạn:</p>"
                    + "<p style='font-size: 18px; font-weight: bold; color: #1e88e5; background-color: #f5f5f5; padding: 10px; display: inline-block; border-radius: 4px;'>" + token + "</p>"
                    + "<p>Mã thông báo này sẽ <b>hết hạn trong 10 phút</b>.</p>"
                    + "<p>Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email này.</p>"
                    + "<br/>"
                    + "<p>Trân trọng,<br/>Đội ngũ Cầu Lông Atmin</p>";

            helper.setText(content, true);

            mailSender.send(message);
            log.info("Reset password email successfully sent to {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send reset password email to {}", toEmail, e);
            throw new RuntimeException("Failed to send reset password email", e);
        }
    }
}
