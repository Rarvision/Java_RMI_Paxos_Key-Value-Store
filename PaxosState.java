/**
 * @author ZHANG Mao
 *
 */
public class PaxosState {
	private int lastPromisedId = -1;
	private int lastAcceptedId = -1;
	private Object lastAcceptedValue = null;
	
	public int getLastPromisedId() {
		return lastPromisedId;
	}
	
	public void setLastPromisedId(int lastPromisedId) {
		this.lastPromisedId = lastPromisedId;
	}
	
	public int getLastAcceptedId() {
		return lastAcceptedId;
	}
	
	public void setLastAcceptedId(int lastAcceptedid) {
		this.lastAcceptedId = lastAcceptedid;
	}
	
	public Object getLastAcceptedValue() {
		return lastAcceptedValue;
	}
	
	public void setLastAcceptedValue(Object lastAcceptedValue) {
		this.lastAcceptedValue = lastAcceptedValue;
	}
	
	
}
