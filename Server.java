import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of a Server class that represents a node in a Paxos distributed consensus system.
 * This server plays the role of Proposer, Acceptor, and Learner in the Paxos algorithm, and it also handles key-value store operations.
 */
public class Server extends UnicastRemoteObject implements ProposerInterface, AcceptorInterface, LearnerInterface, KVStoreInterface {
  
  private static final long serialVersionUID = 6537883049774937470L;
  
  private ConcurrentHashMap<String, String> kvStore = new ConcurrentHashMap<>();
  private AcceptorInterface[] acceptors;
  private LearnerInterface[] learners;
  private int numServers;
  private int serverId;
  
  private AtomicInteger proposalNumber;
  
  private ConcurrentHashMap<String, PaxosState> paxosStates;
  private ExecutorService executorService;
  
  private Logger logger;
  
  private final float FAILURE_PROBABILITY = 0.4f;
  private final int WAIT_TIME = 600;

  /**
   * Constructor to create a Server instance.
   * @param serverId The unique ID of this server.
   * @param numServers The total number of servers in the system.
   * @throws RemoteException 
   */
  public Server(int serverId, int numServers) throws RemoteException {
	super();
	this.executorService = Executors.newFixedThreadPool(numServers);
    this.numServers = numServers;
    this.serverId = serverId;
    this.proposalNumber = new AtomicInteger(0);
    this.paxosStates = new ConcurrentHashMap<>();
    this.logger = new Logger("Server_" + Integer.toString(serverId) + ".txt");
    logger.log("Server_" + Integer.toString(serverId) + " started");
  }

  /**
   * Set the acceptors for this server.
   * @param acceptors Array of acceptors.
   */
  public void setAcceptors(AcceptorInterface[] acceptors) {
    this.acceptors = acceptors;
  }

  /**
   * Set the learners for this server.
   * @param learners Array of learners.
   */
  public void setLearners(LearnerInterface[] learners) {
    this.learners = learners;
  }
  
  @Override
  public synchronized String get(String key) throws RemoteException {
	  String result = kvStore.get(key);
	  logger.log("Applied GET operation on key: " + key + ", get value: " + result);
	  return result;
  }

  @Override
  public synchronized String put(String key, String value) throws RemoteException {
    return proposeOperation(new Operation("PUT", key, value));
  }

  @Override
  public synchronized String delete(String key) throws RemoteException {
    return proposeOperation(new Operation("DELETE", key, null));
  }

  /**
   * Propose an operation to be applied.
   * @param operation The operation to be proposed.
   * @throws RemoteException If a remote error occurs.
   */
  private String proposeOperation(Operation operation) throws RemoteException {
	  String key = operation.getKey();
	  int proposalId = generateProposalId();
	  logger.log("Proposing Operation:" + Integer.toString(proposalId));
      return propose(key, proposalId, operation);
  }

  @Override
  public synchronized ProposalResponse prepare(String key, int proposalId) throws RemoteException {
	  logger.log("PREPARE started. Proposal ID: " + proposalId);
	  Callable<ProposalResponse> prepareTask = () -> {
		  if (Math.random() < FAILURE_PROBABILITY) {
	            logger.log("PREPARE method simulated failure.");
	            Thread.sleep(WAIT_TIME); // Simulate a delay for the restart
	      }
	      PaxosState state = paxosStates.computeIfAbsent(key, k -> new PaxosState());
	      
	      if (proposalId > state.getLastPromisedId()) {
	          state.setLastPromisedId(proposalId);
	          logger.log("Successfully prepared");
	          return new ProposalResponse(state.getLastAcceptedId(), state.getLastAcceptedValue());       
	      }
	      logger.log("Preparation failed");
	      return new ProposalResponse(-2, null); // Indicates rejection      
	  };
	  
	  Future<ProposalResponse> future = executorService.submit(prepareTask);
	  try {
	      return future.get(WAIT_TIME, TimeUnit.MILLISECONDS);
	  } catch (TimeoutException e) {
	      logger.log("Prepare operation timed out");
	      return new ProposalResponse(-1, null); // Indicates timeout
	  } catch (InterruptedException | ExecutionException e) {
	      logger.log("Exception in prepare operation: " + e.getMessage());
	      return new ProposalResponse(-1, null); // or handle exception appropriately
	  }
  }

  @Override
  public synchronized boolean accept(String key, int proposalId, Object proposalValue) throws RemoteException {
	  logger.log("ACCEPT started. Proposal ID: " + proposalId);
	  Callable<Boolean> acceptTask = () -> {
		  
		  if (Math.random() < FAILURE_PROBABILITY) {
	            logger.log("ACCEPT method simulated failure.");
	            Thread.sleep(WAIT_TIME); // Simulate a delay for the restart
	      }
		  
		  PaxosState state = paxosStates.computeIfAbsent(key, k -> new PaxosState());
		  
		  if (proposalId >= state.getLastPromisedId()) {
		        state.setLastAcceptedId(proposalId);
		        state.setLastAcceptedValue(proposalValue);
		        logger.log("Successfully accepted");
		        return true;
		  }
		  logger.log("Acceptance failed");
	      return false;
	  };
	  
	  Future<Boolean> future = executorService.submit(acceptTask);
	  try {
	      return future.get(WAIT_TIME, TimeUnit.MILLISECONDS);
	  } catch (TimeoutException e) {
	      logger.log("Accept operation timed out");
	      return false; // or handle timeout appropriately
	  } catch (InterruptedException | ExecutionException e) {
	      logger.log("Exception in accept operation: " + e.getMessage());
	      return false; // or handle exception appropriately
	  }
  }

  @Override
  public synchronized String propose(String key, int proposalId, Object proposalValue) throws RemoteException {
	  logger.log("PROPOSE started. Proposal ID: " + proposalId);
	  int promiseCount = 0;
	  ProposalResponse highestResponse = new ProposalResponse(-1, null);
	  for(AcceptorInterface acceptor: acceptors) {
		  try {
			  ProposalResponse response = acceptor.prepare(key, proposalId);
			  if(response.getLastAcceptedId() != -2) {
				  promiseCount++;
				  if (response.getLastAcceptedId() > highestResponse.getLastAcceptedId()) {
					  highestResponse = response;
				  }
			  }
		  } catch (RemoteException e) {
			  logger.log(e.toString());
		  }
	  }
	  
	  if (promiseCount > numServers / 2) {
		  Object valueToPropose = highestResponse.getLastAcceptedId() == -1 ? proposalValue : highestResponse.getLastAcceptedValue();
		  int acceptCount = 0;
		  for (AcceptorInterface acceptor : acceptors) {
			  try {
				  if (acceptor.accept(key, proposalId, valueToPropose)) {
					  acceptCount++;
				  }
			  }  catch (RemoteException e) {
				  // log
			  }
		  }
		  
		  String result = null;
		  if (acceptCount > numServers / 2) {
			  for (LearnerInterface learner : learners) {
				  try {
					  result = learner.learn(key, proposalId, valueToPropose);
				  } catch (RemoteException e) {
					  logger.log(e.toString());
				  }
			  }
		  } else {
			  logger.log("*****************Acceptance number is less than a quorum. Proposal failed.*****************");
		  }
		  return result;
	  } else {
		  logger.log("*****************Promise number is less than a quorum. proposal failed.*****************");
		  return null;
	  }
  }

  @Override
  public synchronized String learn(String key, int proposalId, Object acceptedValue) throws RemoteException {
	  logger.log("LEARN started. Proposal ID: " + proposalId);
	  
	  Callable<String> learnTask = () -> {
		  if (acceptedValue instanceof Operation) {
		        Operation operation = (Operation) acceptedValue;
		        logger.log("Finish learning");
		        return applyOperation(operation);
		  } else {
		      logger.log("Failed. Unrecognized operation.");
			  return null;
		  }
	  };
	  
	  Future<String> future = executorService.submit(learnTask);
	  try {
	      return future.get(WAIT_TIME, TimeUnit.MILLISECONDS);
	  } catch (TimeoutException e) {
	      logger.log("Learn operation timed out");
	      return null; // or handle timeout appropriately
	  } catch (InterruptedException | ExecutionException e) {
	      logger.log("Exception in learn operation: " + e.getMessage());
	      return null; // or handle exception appropriately
	  }
  }

  /**
   * Generates a unique proposal ID.
   * @return A unique proposal ID.
   */
  private int generateProposalId() {
    return serverId * 1000000 + proposalNumber.incrementAndGet();
  }

  /**
   * Apply the given operation to the key-value store.
   * @param operation The operation to apply.
   */
  private String applyOperation(Operation operation) {
	  if (operation == null) {
	        return null; // No operation to apply
	    }

	    String key = operation.getKey();
	    String result = null;
	    switch (operation.getType()) {
	        case "PUT":
	            String valueToPut = operation.getValue();
	            result = kvStore.put(key, valueToPut);
	            logger.log("Applied PUT operation on key: " + key + " with value: " + valueToPut);
	            break;
	        case "DELETE":
	            result = kvStore.remove(key);
	            logger.log("Applied DELETE operation on key: " + key);
	            break;
	        default:
	        	logger.log("Unknown operation type: " + operation.getType());
	            // Optionally handle unknown operation types
	            break;
	    }
	    return result;
  }
  
  public void shutdown() {
      logger.log("Server is shutting down.");
      System.exit(0); // This will terminate the JVM running the server
  }

  /**
   * Static class representing an operation on the key-value store.
   */
  private static class Operation {
    String type;
    String key;
    String value;

    Operation(String type, String key, String value) {
      this.type = type;
      this.key = key;
      this.value = value;
    }
    
    public String getType() {
    	return type;
    }

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
  }

  // Other methods as needed
}
