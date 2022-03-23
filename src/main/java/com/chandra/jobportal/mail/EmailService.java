package com.chandra.jobportal.mail;

import java.util.List;

import javax.mail.MessagingException;

import org.springframework.mail.SimpleMailMessage;

import com.chandra.jobportal.entity.JobPostingsView;

/**
 * Created by Olga on 8/22/2016.
 */
public interface EmailService {
    void sendSimpleMessage(String to,
                           String subject,
                           String text);
    void sendSimpleMessageUsingTemplate(String to,
                                        String subject,
                                        SimpleMailMessage template,
                                        String ...templateArgs);
    void sendMessageWithAttachment(String to,
                                   String subject,
                                   String text,
                                   String pathToAttachment);
	void sendMessageWithInLineMsg(String to, String subject, String text, String pathToAttachment);
	
	  String sendSelectedJobs(String to,String subject,  List<?>  text) ;
}
