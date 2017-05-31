package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import filters.Filter;
import filters.FilterTabPane;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

/*
 * This class is an editable TableView which displays all data in a 
 * MySQL table as strings.  "Update" on the individual cell ContextMenus 
 * must be click to submit edits to the MySQL table.
 */
public class MySQLTableView extends TableView<ObservableList<String>>{

	FilterTabPane filters;
	
	public MySQLTableView(FilterTabPane filterTabPane){
		super();
		
		filters = filterTabPane;
		
		setPlaceholder(new Label("Current Table"));
		setEditable(true);
	}
	
	/*
	 * This function initializes all TableColumns and Filters from the
	 * names and the MySQL columns and loads the table data.
	 */
	@SuppressWarnings("unchecked")
	public void initTable(){
		getColumns().clear();
		
		if(SQLEditor.getTableName()==null){
			return;
		}
		
		//primary TableView's data
		ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
		
		//filters for primary TableView
		ObservableList<Filter> filterList = FXCollections.observableArrayList();
		ObservableList<Filter> searchList = FXCollections.observableArrayList();
		
		SessionConfig session = SQLEditor.getSession();
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			
			connection.setCatalog(session.getDatabase());
			
			Statement statement = connection.createStatement();
			
			//data from primary TableView's MySQL table
			ResultSet tableData = statement.executeQuery("SELECT * FROM " 
					+ SQLEditor.getTableName());
			
			//MySQL table's column information
			ResultSetMetaData tableInfo = tableData.getMetaData();
			
			for(int i = 0; i < tableInfo.getColumnCount(); i++){
				
				//create TableColumn and Filters with current column's name
				TableColumn<ObservableList<String>, String> col = 
						getColumn(i, tableInfo.getColumnName(i+1));
				filterList.add(new Filter(tableInfo.getColumnName(i+1)));
				searchList.add(new Filter(tableInfo.getColumnName(i+1)));
				
				getColumns().add(col);
			}
			
			//load MySQL table data into TableView data model
			while(tableData.next()){
				ObservableList<String> row = FXCollections.observableArrayList();
				for(int i = 1; i <= tableInfo.getColumnCount(); i++){
					row.add(tableData.getString(i));
				}
				data.add(row);
			}
			
			setItems(data);
			filters.setAll(filterList, searchList);
			
			connection.close();
			statement.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	/*
	 * This function creates a new TableColumn and sets its factories,
	 * comparator, and context menus.
	 */
	@SuppressWarnings("rawtypes")
	private TableColumn getColumn(final int index, String name){
		
		TableColumn<ObservableList<String>, String> col = 
				new TableColumn<ObservableList<String>, String>(name);
		
		col.setComparator(new CellComparator());

		col.setCellFactory(new UpdatableCellFactory());
		
		col.setCellValueFactory(new Callback<CellDataFeatures<ObservableList<String>, String>, ObservableValue<String>>()
		{
			public ObservableValue<String> call(CellDataFeatures<ObservableList<String>, String> param){
				if(param.getValue().get(index)==null)
					return new SimpleStringProperty("");

				return new SimpleStringProperty(param.getValue().get(index).toString());
			}
		});
		
		col.setContextMenu(getColumnContextMenu(col));
		
		return col;
	}

	/*
	 * This function sets the TableView's data to the contents of the
	 * ResultSet parameter.
	 */
	public void updateTable(ResultSet rs) 
			throws SQLException{

		ResultSetMetaData tableInfo = rs.getMetaData();

		ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
		
		while(rs.next()){
			ObservableList<String> row = FXCollections.observableArrayList();
			for(int i = 1; i <= tableInfo.getColumnCount(); i++){
				row.add(rs.getString(i));
			}
			data.add(row);
		}
		
		setItems(data);
	}
	
	/*
	 * This function returns a ContextMenu with items to rename or delete
	 * the column.
	 */
	private ContextMenu getColumnContextMenu(TableColumn<ObservableList<String>, String> col){
		ContextMenu colMenu = new ContextMenu();
		
		MenuItem rename = new MenuItem("Rename");
		MenuItem delete = new MenuItem("Delete");
		colMenu.getItems().addAll(rename, delete);
		
		rename.setOnAction(e ->
		{
			renameColumn(col);
		});
		
		delete.setOnAction(e ->{
			deleteColumn(col);
		});
		return colMenu;
	}
	
	/*
	 * This function creates a stage with a TextField for the user
	 * to enter the new column name, and passes the input to a function
	 * to rename the MySQL column.
	 */
	private void renameColumn(TableColumn<ObservableList<String>, String> col){
						
		Stage columnRenameStage = new Stage();
		columnRenameStage.setTitle("Column Rename");
						
		Label columnRenameLabel = new Label("New Name: ");
		HBox.setMargin(columnRenameLabel, new Insets(3, 0, 0, 0));
		TextField columnRenameInput = new TextField(col.getText());
		HBox columnRenameRow = new HBox(5, columnRenameLabel, columnRenameInput);
						
		Button okButton = new Button("OK");
		Button cancelButton = new Button("Cancel");
		HBox buttonRow = new HBox(5, okButton, cancelButton);
		buttonRow.setAlignment(Pos.CENTER);

		okButton.setOnAction(v -> {
			renameMySQLColumn(col, columnRenameInput.getText());		
			columnRenameStage.close();
			initTable();
		});
						
		cancelButton.setOnAction(v -> {
			columnRenameStage.close();
		});
						
		VBox tableRenameBox = new VBox(5, columnRenameRow, buttonRow);
		columnRenameStage.setScene(new Scene(tableRenameBox));
		columnRenameStage.show();
	}
	
	/*
	 * This function renames a MySQL column to the name contained in the "newName"
	 * parameter
	 */
	private void renameMySQLColumn(TableColumn<ObservableList<String>, String> col,
			String newName){
		SessionConfig session = SQLEditor.getSession();
		
		try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){

			c.setCatalog(session.getDatabase());
			Statement statement = c.createStatement();
							
			//get type of column to be renamed for new column's definition
			PreparedStatement prepared = c.prepareStatement(
					"SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE "
					+ "TABLE_SCHEMA = ? AND TABLE_NAME = ? and COLUMN_NAME = ?");
							
			prepared.setString(1, c.getCatalog());
			prepared.setString(2, SQLEditor.getTableName());
			prepared.setString(3, col.getText());
			
			ResultSet colType = prepared.executeQuery();
			colType.next();
				
			//rename the column
			statement.execute("ALTER TABLE " + SQLEditor.getTableName() + 
					" CHANGE " + col.getText() + " " + newName + " "
					+ colType.getString(1) + ";");
							
			prepared.close();
			statement.close();
			c.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}

	/*
	 * This function asks the user to confirm they want to delete the
	 * column and then performs the delete and refreshes the table if
	 * they click yes.
	 */
	private void deleteColumn(TableColumn<ObservableList<String>, String> col){
		
		SessionConfig session = SQLEditor.getSession();
		
		Stage confirmDeleteStage = new Stage();
		confirmDeleteStage.setTitle("Confirm Delete");
		
		Label confirmDeleteLabel = new Label("Are you sure you want to delete " 
				+ col.getText() + "?");
		HBox.setMargin(confirmDeleteLabel, new Insets(3, 0, 0, 0));
		confirmDeleteLabel.setAlignment(Pos.CENTER);
		HBox confirmDeleteRow = new HBox(confirmDeleteLabel);
		
		
		Button yesButton = new Button("Yes");
		yesButton.requestFocus();
		Button noButton = new Button("No");
		HBox buttonRow = new HBox(5, yesButton, noButton);
		buttonRow.setAlignment(Pos.CENTER);
		
		yesButton.setOnAction(v -> {
			try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())){

				c.setCatalog(session.getDatabase());
				Statement statement = c.createStatement();

				statement.execute("ALTER TABLE " + SQLEditor.getTableName() 
					+ " DROP COLUMN " + col.getText() + ";");
				statement.close();
				c.close();
				confirmDeleteStage.close();
				//refresh table
				initTable();
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		});
		
		noButton.setOnAction(v -> {
					confirmDeleteStage.close();
		});
		
		VBox confirmDeleteBox = new VBox(5, confirmDeleteRow, buttonRow);
		confirmDeleteStage.setScene(new Scene(confirmDeleteBox));
		confirmDeleteStage.show();			
	}
}
