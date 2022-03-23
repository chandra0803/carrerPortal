package com.chandra.jobportal.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.chandra.jobportal.entity.JobPostingsView;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;

/**
 * Created by Olga on 7/15/2016.
 */
@Component
public class EmailServiceImpl implements EmailService {

    @Autowired
    public JavaMailSender emailSender;
    @Autowired
    public TemplateEngine templateEngine;
 

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("CarrerSearch.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            System.out.println("$$$$$$$$$$$ email message:"  + message);
            emailSender.send(message);
        } catch (MailException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void sendSimpleMessageUsingTemplate(String to,
                                               String subject,
                                               SimpleMailMessage template,
                                               String ...templateArgs) {
        String text = String.format(template.getText(), templateArgs);  
        sendSimpleMessage(to, subject, text);
    }

    @Override
    public void sendMessageWithAttachment(String to,
                                          String subject,
                                          String text,
                                          String pathToAttachment) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            // pass 'true' to the constructor to create a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment("Invoice", file);

            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    

    @Override
    public void sendMessageWithInLineMsg(String to,
                                          String subject,
                                          String text,
                                          String pathToAttachment) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            // pass 'true' to the constructor to create a multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text,true);
           
          //  FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
          //  helper.addInline("jobs", file);

            emailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
   

    public String sendSelectedJobs(String to,String subject, List<?>  text)  {
        Context context = new Context();
        context.setVariable("jobs", text);
		 System.out.println("$$$$$$$$$$$ from sendSelectedJobs  context variables:" +context.getVariables());
        String process = templateEngine.process("emails/welcome", context);
        javax.mail.internet.MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        try {
			helper.setSubject("Welcome " + subject);
		
        helper.setText(process, true);
        helper.setTo(to);
		 System.out.println("$$$$$$$$$$$ from sendSelectedJobs  process size:" +process.length());
        emailSender.send(mimeMessage);
        
        } catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return "Sent";
    }
}
