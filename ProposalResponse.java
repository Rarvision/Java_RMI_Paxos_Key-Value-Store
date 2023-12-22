import java.io.Serializable;

/**
 * @author ZHANG Mao
 *
 */
public class ProposalResponse implements Serializable {
	private static final long serialVersionUID = -9088065429567096196L;
	
	private final int lastAcceptedId;
    private final Object lastAcceptedValue;

    public ProposalResponse(int lastAcceptedId, Object lastAcceptedValue) {
        this.lastAcceptedId = lastAcceptedId;
        this.lastAcceptedValue = lastAcceptedValue;
    }

    public int getLastAcceptedId() {
        return lastAcceptedId;
    }

    public Object getLastAcceptedValue() {
        return lastAcceptedValue;
    }
}
