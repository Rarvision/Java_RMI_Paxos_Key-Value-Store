import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for generating log files
 * @author Mao Zhang
 *
 */

public class Logger {
	private final String fileName;
	private final SimpleDateFormat dateFormat;

	public Logger(String fileName) {
		this.fileName = fileName;
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}

	public synchronized void log(String msg) {
		try {
			System.out.println(msg);
			FileWriter writer = new FileWriter(fileName, true);
			String timestamp = dateFormat.format(new Date());
			writer.write(timestamp + " " + msg + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
