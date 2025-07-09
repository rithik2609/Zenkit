package com.toolkit.util;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.*;

public class EmailUtil {

    private static final MailClient mailClient;

    static {
        MailConfig config = new MailConfig()
                .setHostname("smtp.gmail.com")
                .setPort(587)
                .setStarttls(StartTLSOptions.REQUIRED)
                .setUsername("your_email@gmail.com")         // ✅ your email
                .setPassword("your_app_password")            // ✅ App password (not your Gmail login)
                .setSsl(false);

        mailClient = MailClient.createShared(Vertx.vertx(), config, "mailClient");
    }

    public static void sendEmail(String to, String subject, String content) {
        MailMessage message = new MailMessage()
                .setFrom("your_email@gmail.com")             // ✅ same as username
                .setTo(to)
                .setSubject(subject)
                .setText(content);

        mailClient.sendMail(message, res -> {
            if (res.succeeded()) {
                System.out.println("📧 Email sent to " + to);
            } else {
                System.err.println("❌ Failed to send email: " + res.cause().getMessage());
            }
        });
    }
}
