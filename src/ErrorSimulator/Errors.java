package ErrorSimulator;

public enum Errors {
	INVALID_TID("error 5"),
	ILLEGAL_TFTP_OP("error 4"),
	LOSE_PACKET("lose"),
	DELAY_PACKET("delay"),
	DUPLICATE_PACKET("duplicate"),
	NORMAL_MODE("normal");
	
	private String name;
	
	Errors(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
