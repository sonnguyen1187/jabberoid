package uk.ac.napier.android.jabberoid.service;

interface ConnectionServiceCall {
	boolean isLoggedIn();
	void setStatus(String state, String type, String mode);
	void login();
	void logOff();
	void connect(String state, String type, String mode);
	void disconnect();
	void sendMessage(String user, String message);
	List<String> getLastStatusMessages();
	void insertAndUseMessage(String message);
}