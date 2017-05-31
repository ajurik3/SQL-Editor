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
	}
	
	public SessionConfig(String connectionURL, String name, String pass, String databaseName){
		url = connectionURL;
		username = name;
		password = pass;
		database = databaseName;
	}
	
	public void setDatabase(String name){
		database = name;
	}
	
	public String getDatabase(){
		return database;
	}
	
	public void setUserName(String name){
		username = name;
	}
	
	public void setURL(String connectionURL){
		url = connectionURL;
	}
	
	public void setPassword(String pass){
		password = pass;
	}
	
	public String getUserName(){
		return username;
	}
	
	public String getURL(){
		return url;
	}
	
	public String getPassword(){
		return password;
	}
}
