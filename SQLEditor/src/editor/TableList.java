package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
/*
 * This class is a ListView which displays all tables in the current
 * database.  The user can right click on each table to have the option
 * to delete or rename the table.
 */
public class TableList extends ListView<String>{

	/*
	 * This function creates a ListView with cells that have a context menu.
	 */
	public TableList(){
		super();
		
		setPlaceholder(new Label("Database Tables"));
		
		//set maximum height
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		setMaxHeight(primaryScreenBounds.getHeight()/3);
		
		setCellFactory(new Callback<ListView<String>, ListCell<String>>(){
			public ListCell<String> call(ListView<String> listParam){
				
				ListCell<String> lc = new ListCell<String>(){
					@Override
					protected void updateItem(String item, boolean empty) {
					     super.updateItem(item, empty);

					     if (empty || item == null) {
					         setText(null);
					         setGraphic(null);
					     } else {
					         setText(item.toString());
					     }
					 }
				};
				
				//create and display context menu
				lc.setOnContextMenuRequested(e ->{
					@SuppressWarnings("unchecked")
					final ContextMenu tableListCellMenu = getTableListMenu(
							((ListCell<String>)e.getSource()));
					lc.setContextMenu(tableListCellMenu);
					
					tableListCellMenu.show((Node)e.getSource(), e.getScreenX(), e.getScreenY());
				});
				return lc;
			}
		});
	}
	
	/*
	 * This function loads the list with all tables from the current
	 * database.
	 */
	public void updateList(){
		
		SessionConfig session = SQLEditor.getSession();
		
		if(session==null||session.getDatabase()==null)
			return;
		
		ObservableList<String> data = FXCollections.observableArrayList();
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())) 
		{
			connection.setCatalog(session.getDatabase());

			Statement statement = connection.createStatement();
			
			//all tables in current database
			ResultSet tableQuery = statement.executeQuery("SHOW TABLES;");
		
			//add tables to list
			while(tableQuery.next()){
				data.add(tableQuery.getString(1));
			}
			
			setItems(data);
			
			//update primary TableView and table name when new table is selected
			getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
				@Override
				public void changed(ObservableValue<? extends String> observable, 
						String oldValue, String newValue) {
						if(newValue!=null){
							SQLEditor.setTableName(newValue);
							SQLEditor.getTableView().initTable();
						}
				}
			});
			
			statement.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	/*
	 * This function returns a ContextMenu with items to rename or delete
	 * the table.
	 */
	private ContextMenu getTableListMenu(ListCell<String> menuSource){
		ContextMenu tableListCellMenu = new ContextMenu();
		
		MenuItem rename = new MenuItem("Rename");
		MenuItem delete = new MenuItem("Delete");
		
		tableListCellMenu.getItems().addAll(rename, delete);
		
		rename.setOnAction(e ->
		{
			renameTable(menuSource.getItem());
		});
		
		delete.setOnAction(e ->{
			deleteTable(menuSource.getItem());	
		});
		
		return tableListCellMenu;
	}
	
	/*
	 * This function creates a stage with a TextField for the user
	 * to enter the new table name, and then executes a MySQL statement
	 * to perform the name.
	 */
	private void renameTable(String currentName){
		
		SessionConfig session = SQLEditor.getSession();
		
		Stage tableRenameStage = new Stage();
		tableRenameStage.setTitle("Table Rename");
			
		Label tableRenameLabel = new Label("New Name: ");
		HBox.setMargin(tableRenameLabel, new Insets(3, 0, 0, 0));
		TextField tableRenameInput = new TextField(currentName);
		HBox tableRenameRow = new HBox(5, tableRenameLabel, tableRenameInput);
			
		Button okButton = new Button("OK");
		Button cancelButton = new Button("Cancel");
		HBox buttonRow = new HBox(5, okButton, cancelButton);
		buttonRow.setAlignment(Pos.CENTER);

		okButton.setOnAction(e -> {
			try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())){

				c.setCatalog(session.getDatabase());
				Statement statement = c.createStatement();

				statement.execute("ALTER TABLE " + currentName + 
						" RENAME TO " + tableRenameInput.getText() + ";");
				
				statement.close();
				c.close();
				tableRenameStage.close();
				
				updateList();
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		});
			
		cancelButton.setOnAction(e -> {
			tableRenameStage.close();
		});
			
		VBox tableRenameBox = new VBox(5, tableRenameRow, buttonRow);
		tableRenameStage.setScene(new Scene(tableRenameBox));
		tableRenameStage.show();
	}
	
	/*
	 * This function asks the user to confirm they want to delete the
	 * table, and executes a MySQL statement to perform the delete if
	 * they click yes.
	 */
	private void deleteTable(String tableName){
		SessionConfig session = SQLEditor.getSession();
		
		Stage confirmDeleteStage = new Stage();
		confirmDeleteStage.setTitle("Confirm Delete");
		
		Label confirmDeleteLabel = new Label("Are you sure you want to delete " 
				+ tableName + "?");
		HBox.setMargin(confirmDeleteLabel, new Insets(3, 0, 0, 0));
		confirmDeleteLabel.setAlignment(Pos.CENTER);
		HBox confirmDeleteRow = new HBox(confirmDeleteLabel);
		
		Button yesButton = new Button("Yes");
		yesButton.requestFocus();
		Button noButton = new Button("No");
		HBox buttonRow = new HBox(5, yesButton, noButton);
		buttonRow.setAlignment(Pos.CENTER);
		
		yesButton.setOnAction(e -> {
			try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())){

				c.setCatalog(session.getDatabase());
				Statement statement = c.createStatement();

				statement.execute("DROP TABLE " + tableName + ";");
				statement.close();
				c.close();
				confirmDeleteStage.close();
				SQLEditor.setTableName(null);
				updateList();
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		});
		
		noButton.setOnAction(e -> {
					confirmDeleteStage.close();
		});
		
		VBox confirmDeleteBox = new VBox(5, confirmDeleteRow, buttonRow);
		confirmDeleteStage.setScene(new Scene(confirmDeleteBox));
		confirmDeleteStage.show();
	}
}
