package servent.message;

public class SearchQueryMessage extends BasicMessage {

	private static final long serialVersionUID = 5512277781001882010L;

	private final int originPort;
	private final long seq;
	private final int hopsLeft;
	private final String query;

	public SearchQueryMessage(int senderPort, int receiverPort,
			int originPort, long seq, int hopsLeft, String query) {
		super(MessageType.SEARCH_QUERY, senderPort, receiverPort);
		this.originPort = originPort;
		this.seq = seq;
		this.hopsLeft = hopsLeft;
		this.query = query;
	}

	public int getOriginPort() {
		return originPort;
	}

	public long getSeq() {
		return seq;
	}

	public int getHopsLeft() {
		return hopsLeft;
	}

	public String getQuery() {
		return query;
	}
}
