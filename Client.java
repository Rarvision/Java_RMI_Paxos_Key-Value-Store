import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * The client
 * @author ZHANG Mao
 * 
 */
public class Client {
	
    private Client() {}
    
    // Read from the .txt file to get the pre-populated key-value pair then make the PUT request
    public static void readAndPut(String filePath, KVStoreInterface dict, Logger logger) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2); // split by the first "=" occurrence
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    dict.put(key, value);
                    logger.log("Client sent request: put <"+key+", "+value+"> to the dictionary");
                } else {
                	continue;
                }
            }
        } catch (IOException e) {
            logger.log("Failed to read the file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
    	
    	if (args.length != 2) {
			System.out.println("Need to provide IP of the server and the port.");
			return;
		}
    	
    	Logger logger = new Logger("Client-log.txt");
    	
    	logger.log("Client Started");
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        
        try {
            // Get remote object reference
            Registry registry = LocateRegistry.getRegistry(host, port);
            KVStoreInterface stub = (KVStoreInterface) registry.lookup("KVServer"+port);
            logger.log("Client registered to remote object '"+"ServerService"+port+"' at " + "<" + host + ":" + port + ">");

            // Pre-populate key value pairs from a txt file
            readAndPut("KeyValuePairs.txt", stub, logger);
            
            // Test PUT invocation
            stub.put("10", "msg10");
            logger.log("Client sent request: put <10, msg10> to the dictionary");
            stub.put("11", "msg10");
            logger.log("Client sent request: put <11, msg11> to the dictionary");
            stub.put("12", "msg10");
            logger.log("Client sent request: put <12, msg12> to the dictionary");
            stub.put("13", "msg10");
            logger.log("Client sent request: put <13, msg13> to the dictionary");
            stub.put("14", "msg10");
            logger.log("Client sent request: put <14, msg14> to the dictionary");
            
            // Test GET invocation
            logger.log("Client sent request: get value of key 1 : " + stub.get("1"));
            logger.log("Client sent request: get value of key 2 : " + stub.get("2"));
            logger.log("Client sent request: get value of key 3 : " + stub.get("3"));
            logger.log("Client sent request: get value of key 10 : " + stub.get("10"));
            logger.log("Client sent request: get value of key 11 : " + stub.get("11"));
            // Get an nonexistent key-value pair
            logger.log("Client sent request: get value of key 6 : " + stub.get("6"));
            
            // Test DELETE request
            logger.log("Client sent request: delete value of key 1 : " + stub.delete("1"));
            logger.log("Client sent request: delete value of key 2 : " + stub.delete("2"));
            logger.log("Client sent request: delete value of key 11 : " + stub.delete("11"));
            // Delete an nonexistent key-value pair
            logger.log("Client sent request: delete value of key 6 : " + stub.delete("6"));
            logger.log("Client sent request: delete value of key 8 : " + stub.delete("8"));
            

        } catch (Exception e) {
            logger.log("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
