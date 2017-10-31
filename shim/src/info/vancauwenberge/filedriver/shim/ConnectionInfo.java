package info.vancauwenberge.filedriver.shim;

public class ConnectionInfo {

	private final String password;
	private final String userName;
	private final String connectURL;

	public ConnectionInfo(final String password, final String userName, final String connectURL) {
		this.password=password;
		this.userName=userName;
		this.connectURL=connectURL;
	}

	public String getPassword() {
		return password;
	}

	public String getUserName() {
		return userName;
	}

	public String getConnectURL() {
		return connectURL;
	}

}
