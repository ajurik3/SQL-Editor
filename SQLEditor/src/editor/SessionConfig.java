package editor;

public class SessionConfig {
	private String username;
	private String url;
	private String password;
	private String database;
	
	public SessionConfig(String connectionURL, String name, String pass){
		url = connectionURL;
		username = name;
		password = pass;
		database = "java";
	}
	
	public SessionConfig(String connectionURL, String name, String pass, String databaseName){
		url = connectionURL;
		username = name;
		password = pass;
		database = databaseName;
	}
	
	void setDatabase(String name){
		database = name;
	}
	
	String getDatabase(){
		return database;
	}
	
	void setUserName(String name){
		username = name;
	}
	
	void setURL(String connectionURL){
		url = connectionURL;
	}
	
	void setPassword(String pass){
		password = pass;
	}
	
	String getUserName(){
		return username;
	}
	
	String getURL(){
		return url;
	}
	
	String getPassword(){
		return password;
	}
}
