package org.yanhuang.tools.ip2mail;

import javax.mail.*;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LookupSend {

	private static final String FROM_EMAIL = "zhyhang_mail@tom.com";
	private static String FROM_PASS = "your_password";
	private static final String TO_EMAILS = "zhyhang_mail@tom.com,zhyhang1978@gmail.com";
	private static final String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir","/tmp");
	private static final Path FILE_LAST_IP_LIST = Path.of(JAVA_IO_TMPDIR, ".ip-list-lookup-send");
	private static final int HOURS_FORCE_SEND = 12;
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

	private static LocalDateTime lastSendTime = LocalDateTime.now().minusDays(1);

	public static void main(String[] args) {
		parseArgs(args);
		System.out.format("log file in %s, last ip list saved to %s\n", Toolkit.getLogFilePath(LocalDateTime.now()), FILE_LAST_IP_LIST);
		try {
			TimeUnit.MINUTES.sleep(1);
			while (true) {
				lookupSend();
				TimeUnit.MINUTES.sleep(5);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void parseArgs(String[] args) {
		if (args.length > 0) {
			FROM_PASS = args[0];
		}
	}

	private static void lookupSend() {
		try {
			Toolkit.redirectSysOutByTime(System.currentTimeMillis());
			ipListSend(lastSendTime.until(LocalDateTime.now(), ChronoUnit.HOURS) > HOURS_FORCE_SEND);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		ipList.append("\n\n")
				.append("Public ipv4\n")
				.append(lookupPublicIp());
		return ipList;
	}

	private static void sendEmail(String msg) {
		System.out.println();
		System.out.println("------------------------------------------------------------");
		System.out.println("TLSEmail Start");
		System.out.println("last ip list saved to " + FILE_LAST_IP_LIST);
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
			System.out.println(body);
			Transport.send(msg);
			System.out.println("EMail sent successfully!!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String lookupPublicIp(){
		final String urlString = "http://checkip.amazonaws.com/";
		final Duration timeout = Duration.ofSeconds(30);
		final HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
		final HttpRequest req = HttpRequest.newBuilder().timeout(timeout)
				.uri(URI.create(urlString))
				.GET()
				.build();
		try {
			return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
		} catch (Exception e) {
			e.printStackTrace();
			return "exception";
		}
	}
}
