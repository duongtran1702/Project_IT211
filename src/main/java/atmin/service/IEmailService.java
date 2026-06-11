package atmin.service;

public interface IEmailService {
    void sendResetPasswordEmail(String toEmail, String token);
}
