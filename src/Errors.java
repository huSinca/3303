
public enum Errors {

	INVALID_TID("error 5"),
	ILLEGAL_TFTP_OP("error 4"),
	NORMAL_MODE("normal mode");
	
	private String name;
	
	Errors(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
