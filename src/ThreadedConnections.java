import java.util.HashMap;
import java.util.Map;

public class ThreadedConnections {
	private static Map<Byte, String> RequestTypes;
	static {
		RequestTypes = new HashMap<>();
		// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		RequestTypes.put((byte) 1, "RRQ");
		RequestTypes.put((byte) 2, "WRQ");
		RequestTypes.put((byte) 3, "DATA");
		RequestTypes.put((byte) 4, "ACK");
		RequestTypes.put((byte) 5, "ERROR");
	}
}
