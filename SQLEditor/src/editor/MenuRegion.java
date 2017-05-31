package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
/*
 * This class initializes the application's main menu, as well as a connection
 * bar which allows the user to connect faster.  Since this class contains the
 * forms to collect connection information, it is responsible for setting the
 * SessionConfig variable in the SQLEditor class.
 */
public class MenuRegion extends VBox{
	
	//contains all menus in primary stage
	private MenuBar mainMenu;
	
	//list of all databases in the server, updated at each connection
	private Menu databaseMenu = new Menu("Databases");
	
	//provides a faster way to connect to MySQL database
	private HBox connectionBar;
	
	public MenuRegion(){
		super(5);
		
		mainMenu = getMenuBar();
		connectionBar = getConnectionBar();
		
		getChildren().addAll(mainMenu, connectionBar);
	}
	
	private MenuBar getMenuBar(){
		
		Menu file = getFileMenu();
		
		Menu add = getAddMenu();
			
		return new MenuBar(file, databaseMenu, add);
	}
	
	/*
	 * This function initializes the File Menu and its items,
	 * as well as the item's onAction properties.
	 */
	Menu getFileMenu(){
		Menu file = new Menu("File");
		MenuItem imp = new MenuItem("Import");
		MenuItem connect = new MenuItem("Connect");
		MenuItem quit = new MenuItem("Quit");
		
		file.getItems().addAll(connect, imp, quit);
		
		connect.setOnAction(e -> {getConnection();});
		
		imp.setOnAction(e -> {
			FileImporter importer = new FileImporter(SQLEditor.getSession());
			importer.getFile(SQLEditor.getTableList());
		});
		
		quit.setOnAction(e -> Platform.exit());
		
		return file;
	}
	
	/*
	 * This function initializes the Add Menu and its items,
	 * as well as the item's onAction properties.
	 */
	Menu getAddMenu(){
		Menu add = new Menu("Add");
		MenuItem column = new MenuItem("Add Column");
		MenuItem table = new MenuItem("Add Table");

		column.setOnAction(e -> {
			ColumnBuilder builder = new ColumnBuilder(SQLEditor.getSession());
			builder.buildColumn();
		});
		
		table.setOnAction(e ->{
			TableBuilder builder = new TableBuilder(SQLEditor.getSession());
			builder.buildTable(SQLEditor.getTableList());
		});
		
		add.getItems().addAll(table, column);
		return add;
	}
	
	/*
	 * This function provides TextFields for users to provide their database
	 * URL, account name, and password.
	 */
	private void getConnection(){
		Stage connectWindow = new Stage();
		connectWindow.setTitle("Connect");
		
		TextField urlInput = new TextField("localhost:3306");
		TextField accountInput = new TextField();
		PasswordField passwordInput = new PasswordField();
		
		HBox urlRow = new HBox(5, new Label("Domain: "), urlInput);
		urlInput.setAlignment(Pos.CENTER);
		HBox accountRow = new HBox(5, new Label("Account: "), accountInput);
		HBox passwordRow = new HBox(5, new Label("Password: "), passwordInput);
		
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		
		cancel.setOnAction(v ->{
			connectWindow.close();
		});
		
		ok.setOnAction(v -> {
			connect(urlInput.getText(), accountInput.getText(),
				passwordInput.getText());
							
			connectWindow.close();
		});
		
		HBox buttons = new HBox(5, ok, cancel);
		buttons.setAlignment(Pos.CENTER);
		
		VBox vBox = new VBox(10, urlRow, accountRow, passwordRow, buttons);
		
		Scene scene = new Scene(vBox);
		connectWindow.setScene(scene);
		connectWindow.show();
	}

	/*
	 * This function returns an HBox containing a form which can be used to more
	 * quickly connect to the database server
	 */
	private HBox getConnectionBar(){
		
		Label urlLabel = new Label("Database URL:");
		HBox.setMargin(urlLabel, new Insets(3, 0, 0, 0));
		TextField urlInput = new TextField("localhost:3306");
		
		Label usernameLabel = new Label("Username:");
		HBox.setMargin(usernameLabel, new Insets(3, 0, 0, 0));
		TextField usernameInput = new TextField();

		Label passwordLabel = new Label("Password: ");
		HBox.setMargin(passwordLabel, new Insets(3, 0, 0, 0));
		PasswordField passwordInput = new PasswordField();
		
		Button connectButton = new Button("Connect");
		
		connectButton.setOnAction(e ->{
			connect(urlInput.getText(), usernameInput.getText(),
					passwordInput.getText());
		});
		
		return new HBox(5, urlLabel, urlInput, usernameLabel, 
				usernameInput, passwordLabel, passwordInput, 
				connectButton);
	}
	
	/*
	 * This function creates an object to store the details of the latest
	 * connection and updates the database menu.
	 */
	private void connect(String URL, String user, String password){
		
		//URL += "/java?verifyServerCertificate=false&useSSL=true";
		
		SessionConfig session = new SessionConfig("jdbc:mysql://" + URL, user, 
				password);
		
		//find databaseName in URL
		String databaseName = "";
		
		if(URL.indexOf("?")>0&&URL.indexOf("/")>=0)
			databaseName = URL.substring(URL.indexOf("/")+1, URL.indexOf("?"));
		else if(URL.indexOf("/")>=0)
			databaseName = URL.substring(URL.indexOf("/")+1);
		
		if(!databaseName.equals("")){
			session.setDatabase(databaseName);
			SQLEditor.setSession(session);
			SQLEditor.getTableList().updateList();
		}
		
		SQLEditor.setSession(session);
		updateDatabaseList();
	}
	
	/*This function loads the Database Menu with all visible databases on the
	 * server.  When one of these items is selected, the session database is
	 * changed and the TableList is updated.
	*/
	private void updateDatabaseList(){
		SessionConfig session = SQLEditor.getSession();
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())) 
		{
			Statement statement = connection.createStatement();
			
			//all databases in current server
			ResultSet databases = statement.executeQuery("show databases;");
		    
		    while(databases.next()){
		    	databaseMenu.getItems().add(new MenuItem(databases.getString(1)));
		    }

		    //change session variable and create new table list whenever
		    //database is changed
		    for(MenuItem m : databaseMenu.getItems()){
		    	m.setOnAction( e->{
		    		SQLEditor.getSession().setDatabase(m.getText());
		    		SQLEditor.getTableList().updateList();
		    	});
		    }
		    
		    statement.close();
		    connection.close();
		    
		} catch (SQLException ex) {
		    throw new IllegalStateException("Cannot connect the database!", ex);
		}
	}
}
