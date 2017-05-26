package editor;

/*
 * This object holds fields frequently used when establishing a new JDBC MySQL
 * connection.
 */

public class SessionConfig {
	
	//MySQL user connecting to the database
	private String username;
	
	//database URL of the form jdbc:MySQL:subname
	private String url;
	
	//MySQL user password
	private String password;
	
	//database the user is currently using 
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
