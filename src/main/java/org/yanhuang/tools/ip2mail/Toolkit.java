package org.yanhuang.tools.ip2mail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.CREATE;

public final class Toolkit {

	private static LocalDate currentLogDate;

	public synchronized static void redirectSysOutByTime(long ts) {
		final var sysLogDate = parseFrom(ts).toLocalDate();
		if (currentLogDate == null || !currentLogDate.equals(sysLogDate)) {
			currentLogDate = sysLogDate;
			final Path logFile = getLogFilePath(currentLogDate.atStartOfDay());
			if (!Files.exists(logFile.getParent())) {
				final boolean ignored = logFile.getParent().toFile().mkdirs();
			}
			redirectSysOut(logFile);
		}
	}

	public static Path getLogFilePath(LocalDateTime currentLogTime) {
		return Path.of(".").resolve("logs")
				.resolve(toDateHumanString(currentLogTime) + ".sys.log");
	}

	private static void redirectSysOut(Path logFile) {
		final PrintStream oldOut = System.out;
		final PrintStream oldErr = System.err;
		OutputStream outFileStream = null;
		try {
			outFileStream = Files.newOutputStream(logFile, CREATE, APPEND);
			final PrintStream filePs = new PrintStream(outFileStream, true, StandardCharsets.UTF_8);
			System.setOut(filePs);
			System.setErr(filePs);
		} catch (Exception e) {
			System.setOut(oldOut);
			System.setErr(oldErr);
			e.printStackTrace();
			closeIgnoreError(outFileStream);
			return;
		}
		oldOut.close();
		oldErr.close();
	}

	private static void closeIgnoreError(OutputStream outFileStream) {
		if (outFileStream != null) {
			try {
				outFileStream.close();
			} catch (IOException ignored) {
			}
		}
	}

	public static String toDateHumanString(LocalDateTime time) {
		return time.getYear() + toStringAlign2Chars(time.getMonthValue()) + toStringAlign2Chars(time.getDayOfMonth());
	}

	private static LocalDateTime parseFrom(long ts) {
		return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	private static String toStringAlign2Chars(int v) {
		if (v < 10) {
			return 0 + String.valueOf(v);
		}
		return String.valueOf(v);
	}
}
