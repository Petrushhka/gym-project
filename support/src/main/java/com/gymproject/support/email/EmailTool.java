package com.gymproject.support.email;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 순수하게 이메일만 발송하는 기능
public class EmailTool {

    private static Logger log = LoggerFactory.getLogger(EmailTool.class);

    private final JavaMailSender mailSender;

    public void sendMail(String email, String title, String text) {
        SimpleMailMessage emailForm = createEmailForm(email, title, text);
        try{
            mailSender.send(emailForm);
            log.info("Email 발송 성공");
        } catch (Exception e){
            log.error("Email 발송 실패");
        }
    }

    private SimpleMailMessage createEmailForm(String email, String title, String text) {
        SimpleMailMessage emailForm = new SimpleMailMessage();
        emailForm.setTo(email);
        emailForm.setSubject(title);
        emailForm.setText(text);

        return emailForm;
    }




}
