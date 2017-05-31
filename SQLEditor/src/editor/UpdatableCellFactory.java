package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/*
 * This callback returns an EditableCell with a ContextMenu with an item 
 * to submit its edits to the MySQL data that the cell represents.  The table
 * must have a primary key defined for the update to complete successfully.
 */
public class UpdatableCellFactory implements 
	Callback<TableColumn<ObservableList<String>, String>, TableCell<ObservableList<String>, String>>{
	
	public EditableTableCell call(TableColumn<ObservableList<String>, String> param){
		
		EditableTableCell cell = new EditableTableCell();
		
		cell.setOnContextMenuRequested(e ->{
			final ContextMenu cellMenu = getCellContextMenu((EditableTableCell)e.getSource());
			cell.setContextMenu(cellMenu);
			
			//set context menu location to where ContextMenuEvent occurred
			cellMenu.show((Node)e.getSource(), e.getScreenX(), e.getScreenY());
			
		});
		
		return cell;
	}
	
	/*
	 * This function returns a ContextMenu with the item "Update", which updates
	 * the MySQL data if it can be found with its primary key values.
	 */
	public static ContextMenu getCellContextMenu(EditableTableCell cell){
		ContextMenu cellMenu = new ContextMenu();
		
		TableView<ObservableList<String>> currentTable = cell.getTableView(); 
		
		MenuItem update = new MenuItem("Update");
		
		cellMenu.getItems().addAll(update);
		
		SessionConfig session = SQLEditor.getSession();
		
		update.setOnAction(e ->{
			try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())){
				connection.setCatalog(session.getDatabase());
				Statement statement = connection.createStatement();
				
				//indices of primary key
				ArrayList<Integer> keyIndices = getKeyIndices();
				
				if(keyIndices.isEmpty())
					return;
				
				//cell row in TableView
				int rowIndex = cell.getIndex();
				
				//begin update statement
				String queryString = "UPDATE " + SQLEditor.getTableName() + " SET " + cell.getTableColumn().getText() 
						+ " = \'" + cell.getItem() +"\' WHERE ";
				
				//add row's primary key values to where clause
				for(int i = 0; i < keyIndices.size(); i++){
					int keyIndex = keyIndices.get(i);
					queryString += currentTable.getColumns().get(keyIndex).getText()
							+ " = \'" + currentTable.getItems().get(rowIndex).get(keyIndex) +"\' AND ";
				}
				
				queryString = queryString.substring(0, queryString.length()-4) + ";";
				
				statement.execute(queryString);
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		});
		
		return cellMenu;
	}
	
	/*
	 * This function returns an array of all the indices of all columns in
	 * the TableView which are a part of the table's MySQL primary key.
	 */
	@SuppressWarnings("rawtypes")
	private static ArrayList<Integer> getKeyIndices(){
		
		ArrayList<Integer> keyIndices = null;
		
		SessionConfig session = SQLEditor.getSession();
		TableView<ObservableList<String>> tableview = SQLEditor.getTableView();
		
		try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			connection.setCatalog(session.getDatabase());
			Statement statement = connection.createStatement();
			
			//get name of all columns that make up the primary key
			String queryString = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE "
					+ "TABLE_NAME = \'" + SQLEditor.getTableName() + 
					"\' AND COLUMN_KEY = \'PRI\';";
			
			ResultSet primaryKeyNames = statement.executeQuery(queryString);
			
			keyIndices = new ArrayList<>();
			
			//find indices of all columns in TableView for all columns in primary key
			while(primaryKeyNames.next()){
				for(int i = 0; i < tableview.getColumns().size(); i++){
					String columnName = tableview.getColumns().get(i).getText();
					if(columnName.equals(primaryKeyNames.getString(1)))
						keyIndices.add(i);
				}
			}
			statement.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
			return keyIndices;
	}
}