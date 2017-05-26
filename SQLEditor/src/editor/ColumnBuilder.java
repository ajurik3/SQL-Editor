package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/*
 * This class creates a stage with controls for the user to add a column
 * to the table currently selected on the main screen.  The user inputs
 * the new column name in a TextField, and there is a TextArea for the user
 * to enter a expression with which to generate its values.  The stage also
 * contains a ComboBox and ListView, which can be used to select a table and
 * column within that table respectively.  The selected column's path in 
 * the database is added to the TextArea generation expression.  A CheckBox
 * is used to set the new column's type as numeric (double) or non-numeric
 * (VarChar).
 */
public class ColumnBuilder {

	//name of the new column being created
	private StringProperty newColumnName;
	
	//current connection information
	private SessionConfig session;
	
	//currently selected table, bound to a ComboBox
	private StringProperty table;
	
	//view of column names within currently selected table, refreshed when
	//table changes
	private ListView<String> columnNames = new ListView<String>();
	
	//user input containing the expression used to define values for new column
	private TextArea columnDefinition = new TextArea();
	
	//filters non-numeric columns from view if true
	private BooleanProperty numericColumn = new SimpleBooleanProperty(false);
	
	public ColumnBuilder(SessionConfig currentSession){
		newColumnName = new SimpleStringProperty();
		table = new SimpleStringProperty();
		session = currentSession;
		
		//refresh column list if non-numeric filter is turned on or off
		numericColumn.addListener(new InvalidationListener(){
			public void invalidated(Observable arg0){
				if(table!=null&&!table.getValue().isEmpty())
					fillColumnList(table.getValue());
		}
		});
		
	}
	/*
	 * Creates a form used to specify column definition, including
	 * the following:
	 * 
	 * New Column Name (TextField)
	 * Optional Source Table(s) and Column(s) (ComboBox and ListView)
	 * Whether column is numeric (CheckBox)
	 * Column Definition Expression (TextArea)
	 */
	public void buildColumn(){
		Stage builderWindow = new Stage();
		
		//new column name input
		Label columnNameLabel = new Label("Column Name: ");
		HBox.setMargin(columnNameLabel, new Insets(3, 0, 0, 0));
		TextField columnNameInput = new TextField();
		columnNameInput.textProperty().bindBidirectional(newColumnName);
		columnNameInput.setText("column1");
		HBox columnNameRow = new HBox(5, columnNameLabel, columnNameInput);
		columnNameRow.setAlignment(Pos.CENTER);
		
		//select table of column
		Label tableSelectLabel = new Label("Table: ");
		HBox.setMargin(tableSelectLabel, new Insets(3, 0, 0, 0));
		ComboBox<String> tableSelect = new ComboBox<String>();
			
		if(session!=null){
			fillTableComboBox(tableSelect);
		}
		
		//fill view of columns in currently selected table whenever table changes
		tableSelect.valueProperty().addListener(new ChangeListener<String>(){
			@SuppressWarnings("rawtypes")
			public void changed(ObservableValue arg0, String oldValue, String newValue){
				fillColumnList(newValue);
			}
		});
		
		table.bind(tableSelect.valueProperty());
		HBox tableSelectRow = new HBox(5, tableSelectLabel, tableSelect);
		
		CheckBox showNumericTypes = new CheckBox("Show Numeric Types Only");
		numericColumn.bind(showNumericTypes.selectedProperty());

		VBox columnMetaDataVBox = new VBox(5, tableSelectRow, showNumericTypes);
		
		HBox columnSelectRow = new HBox(5, columnMetaDataVBox, columnNames);
		columnSelectRow.setAlignment(Pos.CENTER);
		
		//column definition row
		Label columnDefinitionLabel = new Label("Column Definition:");
		HBox.setMargin(columnDefinitionLabel, new Insets(3, 0, 0, 0));
		
		columnDefinition.setPrefColumnCount(30);
		columnDefinition.setPrefRowCount(3);
		columnDefinition.setWrapText(true);
		
		HBox columnDefinitionRow = new HBox(5, columnDefinitionLabel, columnDefinition);
		
		//Action buttons
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		
		ok.setOnAction(e -> {
			if(!SQLEditor.getTableName().isEmpty()){
				createColumn();
				builderWindow.close();
			}
			else
				new NotificationWindow("Undefined Table",
						"Please select a table to add the column to on the main screen.");
		});
		
		cancel.setOnAction(e -> {
			builderWindow.close();
		});
		
		HBox buttonRow = new HBox(5, ok, cancel);
		buttonRow.setAlignment(Pos.CENTER);
		
		columnNames.setMaxHeight(300);
		columnNames.setMaxWidth(200);
		
		//when a column in ListView is clicked 
		//add its full name to the column definition
		columnNames.getSelectionModel().selectedItemProperty().addListener(
				new ChangeListener<String>(){
			@SuppressWarnings("rawtypes")
			public void changed(ObservableValue arg0, String oldValue,
					String newValue){
				
				if(newValue!=null){
					columnDefinition.setText(columnDefinition.getText() 
						+ table.getValue() + "." + newValue + " ");
					columnDefinition.requestFocus();
				}
			}
		});
		
		VBox columnForm = new VBox(5, columnNameRow, columnSelectRow, columnDefinitionRow, 
				buttonRow);
		VBox.setMargin(columnSelectRow, new Insets(10, 0, 10, 0));
		VBox.setMargin(buttonRow, new Insets(10, 0, 0, 0));
		
		Scene scene = new Scene(columnForm);
		
		builderWindow.setMinWidth(400);
		
		builderWindow.setScene(scene);
		builderWindow.setTitle("Define Column");
		builderWindow.show();
	}
	
	/*
	 * This function fills the ComboBox with all tables in the current database.
	 */
	private void fillTableComboBox(ComboBox<String> tableSelect){
		try (Connection connection = DriverManager.getConnection(session.getURL(), 
				session.getUserName(), session.getPassword())) 
		{
			connection.setCatalog(session.getDatabase());
			
			Statement statement = connection.createStatement();
			
			//all tables for current database
			ResultSet tables = statement.executeQuery("show tables;");
		
			//populate ComboBox with tables in database
			while(tables.next()){
				tableSelect.getItems().add(tables.getString(1));
			}
			
			//set default value to column currently viewed in main TableView
			tableSelect.setValue(SQLEditor.getTableName());
			
			//fill column list with default selected table if not null
			if(!tableSelect.getValue().isEmpty())
				fillColumnList(SQLEditor.getTableName());
		
			statement.close();
			connection.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	
	/*
	 * This function fills the column ListView with all columns from the table
	 * specified by the ComboBox input, if the non-numeric filter is disabled.  
	 * If it's enabled, the function only adds columns with numeric types.
	 */
	private void fillColumnList(String tableName){
		
		columnNames.getItems().clear();
		
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				connection.setCatalog(session.getDatabase());
				
				Statement statement = connection.createStatement();
				
				//all columns for selected table
				ResultSet columnData = statement.executeQuery("SHOW COLUMNS FROM " 
						+ tableName + ";");
			
				//populate the ListView with columns from the selected table
				while(columnData.next()){
					if(numericColumn.getValue()){
						//only add numeric columns to the ListView
						String columnName = columnData.getString(1);
						if(isNumeric(columnData.getString(2)))
							columnNames.getItems().add(columnName);
					}
					else
						//add all columns to the list view
						columnNames.getItems().add(columnData.getString(1));
				}
							
				statement.close();
				connection.close();
			}
			catch (SQLException ex) {
				//connection lost/failed, session database invalid, table deleted
				ex.printStackTrace();
			}
		}
		
	}
	
	
	private void createColumn(){
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				connection.setCatalog(session.getDatabase());
				Statement statement = connection.createStatement();
				
				//remove spaces and text wrapping
				String expression = columnDefinition.getText().replace("\n", "");
				
				//set type as a large double or VarChar
				String type = numericColumn.getValue() ? "DOUBLE PRECISION(12, 2)" : "VARCHAR(50)";
				
				String queryString = new String();
				
				//add column to MySQL table
				if(expression!=null&&!expression.isEmpty())
					queryString = "ALTER TABLE " + SQLEditor.getTableName()
				     + " ADD COLUMN " + newColumnName.getValue() + " " + type + " AS ("
				     + expression + ");";
				else
					queryString = "ALTER TABLE " + table.getValue()
				     + " ADD COLUMN " + newColumnName.getValue() + " VARCHAR(50);";
				
				statement.execute(queryString);
				
				statement.close();
				connection.close();
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		}
	}
	
	//returns true if input represents a common numeric MySQL type
	private static boolean isNumeric(String type){
		
		String t = type.toLowerCase();
		
		return t.contains("int")||t.contains("float")||t.contains("double")||t.contains("decimal");
	}
}