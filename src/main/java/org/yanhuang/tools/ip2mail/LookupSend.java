package org.yanhuang.tools.ip2mail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LookupSend {

	private static final String FROM_EMAIL = "zhyhang_mail@tom.com";
	private static final String FROM_PASS = "your_password";
	private static final String TO_EMAILS = "zhyhang_mail@tom.com,zhyhang1978@gmail.com";
	private static final String JAVA_IO_TMPDIR_KEY = "java.io.tmpdir";
	private static final Path FILE_LAST_IP_LIST = Paths.get(System.getProperty(JAVA_IO_TMPDIR_KEY), ".ip-list-lookup" +
			"-send");
	public static final int HOURS_FORCE_SEND = 6;
	private static final Properties MAIL_SERVER_PROP = new Properties();

	static {
		MAIL_SERVER_PROP.put("mail.smtp.host", "smtp.tom.com"); //SMTP Host
		MAIL_SERVER_PROP.put("mail.smtp.port", "25"); //TLS Port
		MAIL_SERVER_PROP.put("mail.smtp.auth", "true"); //enable authentication
//		MAIL_SERVER_PROP.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
	}

	private static String EMAIL_TITLE;

	static {
		try {
			EMAIL_TITLE = InetAddress.getLocalHost().getHostName() + " ip list";
		} catch (UnknownHostException e) {
			EMAIL_TITLE = "p51 ip list";
		}
	}

	private static final ScheduledExecutorService SES = Executors.newSingleThreadScheduledExecutor();

	private static LocalDateTime lastSendTime = LocalDateTime.now().minusDays(1);

	public static void main(String[] args) {
		System.out.format("log file in %s\n", Toolkit.getLogFilePath(LocalDateTime.now()));
		Toolkit.redirectSysOutByTime(System.currentTimeMillis());
		SES.scheduleWithFixedDelay(() -> {
			try {
				scheduleTask();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 1, 5, TimeUnit.MINUTES);
	}

	private static void scheduleTask() throws IOException {
		ipListSend(lastSendTime.until(LocalDateTime.now(), ChronoUnit.HOURS) > HOURS_FORCE_SEND);
	}

	private static void ipListSend(boolean forceSend) throws IOException {
		final StringBuilder ipList = lookupIpList();
		final byte[] ipListBytes = ipList.toString().getBytes(StandardCharsets.UTF_8);
		final byte[] savedIpListBytes = Files.exists(FILE_LAST_IP_LIST) ?
				Files.readAllBytes(FILE_LAST_IP_LIST) : new byte[0];
		if (!forceSend && Objects.deepEquals(ipListBytes, savedIpListBytes)) {
			return;
		}
		sendEmail(ipList.toString());
		lastSendTime = LocalDateTime.now();
		Files.write(FILE_LAST_IP_LIST, ipListBytes);
	}

	private static StringBuilder lookupIpList() throws SocketException {
		final StringBuilder ipList = new StringBuilder();
		final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			final NetworkInterface n = e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			if (!ee.hasMoreElements()) {
				continue;
			}
			ipList.append('\n').append('\n')
					.append(n.getDisplayName());
			while (ee.hasMoreElements()) {
				final InetAddress i = ee.nextElement();
				ipList.append('\n').append(i.getHostAddress());
			}
		}
		return ipList;
	}

	private static void sendEmail(String msg) {
		System.out.println("TLSEmail Start");
		//create Authenticator object to pass in Session.getInstance argument
		final Authenticator auth = new Authenticator() {
			//override the getPasswordAuthentication method
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(FROM_EMAIL, FROM_PASS);
			}
		};
		Session session = Session.getInstance(MAIL_SERVER_PROP, auth);
		sendEmail(session, TO_EMAILS, EMAIL_TITLE, msg);
	}

	private static void sendEmail(Session session, String toEmail, String subject, String body) {
		try {
			MimeMessage msg = new MimeMessage(session);
			//set message headers
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			msg.setFrom(new InternetAddress(FROM_EMAIL, "My Self"));

			msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));

			msg.setSubject(subject, "UTF-8");

			msg.setText(body, "UTF-8");

			msg.setSentDate(new Date());

			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
			System.out.println(LocalDateTime.now());
			System.out.println("Message is ready");
			System.out.println("------------------------------------------------------------");
			System.out.println(body);
			System.out.println("------------------------------------------------------------");
			Transport.send(msg);
			System.out.println("EMail Sent Successfully!!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
