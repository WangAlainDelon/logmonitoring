package com.wx.logmonitor.mail;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

public class MailUtils {


	public static void sendMail(MailInfo mailInfo) throws AddressException, MessagingException {



	}

	private static void send(String email,String emailMsg)throws AddressException, MessagingException
    {

        // 1.创建一个程序与邮件服务器会话对象 Session

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "SMTP");
        props.setProperty("mail.host", "smtp.qq.com");//smtp.126.com  smtp.qq.com
        props.setProperty("mail.smtp.auth", "true");// 指定验证为true 是否开启权限控制

        //发送qq邮件需要增加以下的步骤
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.ssl.enable", "true");  //530 Error: A secure connection is requiered(such as ssl)
        props.setProperty("mail.debug", "true");  //测试邮件时是否提醒
        // 创建验证器
        Authenticator auth = new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("***@qq.com", "***");
            }
        };

        Session session = Session.getInstance(props, auth);

        // 2.创建一个Message，它相当于是邮件内容
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress("***@qq.com")); // 设置发送者

        message.setRecipient(RecipientType.TO, new InternetAddress(email)); // 设置发送方式与接收者

        message.setSubject("用户激活");
        // message.setText("这是一封激活邮件，请<a href='#'>点击</a>");

        message.setContent(emailMsg, "text/html;charset=utf-8");

        // 3.创建 Transport用于将邮件发送

        Transport.send(message);
    }
}
