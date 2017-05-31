package editor;

import filters.*;

import javafx.stage.*;
import javafx.geometry.*;
import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.layout.*;


/*
 * This class launches the application and initializes all components
 * of the application's primary window.  The class has methods for
 * other classes to request a reference to these components, or other
 * state variables maintained in this class.
 */
public class SQLEditor extends Application{

	//ListView displaying all tables in current database
	private static TableList tables = new TableList();
	
	//tabs to set searches or filters for any column in the current table
	private static FilterTabPane filterTabs = new FilterTabPane();
	
	//TableView displaying the currently selected MySQL Table
	private static MySQLTableView tableview = new MySQLTableView(filterTabs);
	
	//MySQL name of the table currently selected
	private static String tableName;
	
	//session data for current connection or previous connection
	private static SessionConfig session;
	
	public void start(Stage stage) throws Exception{
	
		MenuRegion menuRegion = new MenuRegion();
		
		BorderPane border = new BorderPane();
		
		border.setTop(menuRegion);
		
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		Scene scene = new Scene(border);
		stage.setScene(scene);
		stage.setTitle("SQL Editor");
		stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
		stage.show();		

		VBox leftPane = new VBox(5, tables, filterTabs);
		leftPane.setMinWidth(border.getWidth()/5.7);
		
		border.setLeft(leftPane);
		
		tableview.setMinWidth(border.getWidth()*6/7);
		border.setCenter(tableview);
	}

	public static TableList getTableList(){
		return tables;
	}
	
	public static SessionConfig getSession(){
		return session;
	}
	
	public static void setSession(SessionConfig currentSession){
		session = currentSession;
	}
	
	public static String getTableName(){
		return (tableName==null) ? "" : tableName;
	}
	
	static void setTableName(String name){
		tableName = name; 
	}
	
	public static MySQLTableView getTableView(){
		return tableview;
	}
	public static void main(String[] args){
		Application.launch(args);
	}
}