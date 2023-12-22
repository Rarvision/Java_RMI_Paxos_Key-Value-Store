import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * The PaxosServerCreator class is responsible for creating and binding the Paxos servers
 * within the RMI registry. It also configures the acceptors and learners for each server.
 */
public class PaxosServerCreator {

  /**
   * The main method to launch the creation and binding process of the Paxos servers.
   *
   * @param args Command-line arguments (unused in this context).
   */
  public static void main(String[] args) {
    try {
    	
      if (args.length < 1) {
    	System.out.println("Usage: PaxosServerCreator <Base Port>");
		return;
	  }
      
      int numServers = 5; // Total number of servers
      
      int basePort = Integer.parseInt(args[0]); // Starting port number

      Server[] servers = new Server[numServers];

      // Create and bind servers
      for (int serverId = 0; serverId < numServers; serverId++) {
        int port = basePort + serverId; // Increment port for each server

        // Create server instance
        servers[serverId] = new Server(serverId, numServers);

        // Bind the server to the RMI registry
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("KVServer" + port, servers[serverId]);

        System.out.println("Server " + serverId + " is ready at port " + port);
      }

      // Set acceptors and learners for each server
      for (int serverId = 0; serverId < numServers; serverId++) {
        AcceptorInterface[] acceptors = new AcceptorInterface[numServers];
        LearnerInterface[] learners = new LearnerInterface[numServers];
        for (int i = 0; i < numServers; i++) {
          //if (i != serverId) {
            acceptors[i] = servers[i];
            learners[i] = servers[i];
          //}
        }
        servers[serverId].setAcceptors(acceptors);
        servers[serverId].setLearners(learners);
      }

    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
      e.printStackTrace();
    }
    
    System.out.println("Servers are running. Press Enter to exit.");
    try {
        System.in.read(); // Wait for the user to press Enter
    } catch (IOException e) {
        System.err.println("I/O error: " + e.getMessage());
    }

    System.out.println("Shutting down servers...");
  }
}
