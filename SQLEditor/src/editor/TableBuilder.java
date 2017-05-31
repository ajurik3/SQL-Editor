package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import columninfo.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

/*
 * This class creates a form which allows the user to create a
 * table including any column from any table in the current
 * database.  These columns can also be designated as a primary
 * or foreign constraint in the new table, or an auto incremented
 * primary key can be added.
 */
public class TableBuilder {

	//table to be created
	private StringProperty newTableName;
	
	private SessionConfig session;
	
	//currently selected table of potential source columns
	private StringProperty table;
	
	//view of potential source columns of currently selected table
	private ListView<String> columnNames = new ListView<String>();
	
	//name of optional auto-incremented primary key
	private String generatedKey;
	
	//view of information for columns selected to be added to new table
	private TableView<ColumnProperties> columnMeta 
				= ColumnProperties.getPropertyColumns();
	
	public TableBuilder(SessionConfig newSession){
		session = newSession;
		newTableName = new SimpleStringProperty();
		table = new SimpleStringProperty();
		
		changeColToFullName();
	}
/*
 * Creates the form used to specify table information, including
 * 
 * New Table Name (TextField)
 * Column Properties (TableView)
 * Source Table(s) and Column(s) (ComboBox and ListView)
 * Optional Auto-Incremented Primary Key Name (CheckBox and TextField)
 */
	public void buildTable(TableList tableList){		
		
		if(session==null){
			new NotificationWindow("Connection required", 
					"You must connect to a database server to use this feature");
			return;
		}
		
		if(session.getDatabase()==null){
			new NotificationWindow("Database Required", 
					"You must select a database from the menu to add this table to.");
			return;
		}
		
		Stage builderWindow = new Stage();
		builderWindow.setTitle("Define Table");
		
		//new table name
		Label tableNameLabel = new Label("Table Name: ");
		HBox.setMargin(tableNameLabel, new Insets(3, 0, 0, 0));
		TextField tableNameInput = new TextField();
		tableNameInput.textProperty().bindBidirectional(newTableName);
		tableNameInput.setText("t");
		HBox tableNameRow = new HBox(5, tableNameLabel, tableNameInput);
		
		//select table of column
		Label tableSelectLabel = new Label("Table: ");
		HBox.setMargin(tableSelectLabel, new Insets(3, 0, 0, 0));
		ComboBox<String> tableSelect = new ComboBox<String>();
					
		if(session!=null){
			fillTableComboBox(tableSelect);
		}
				
		tableSelect.valueProperty().addListener(new ChangeListener<String>(){
			@SuppressWarnings("rawtypes")
			public void changed(ObservableValue arg0, String oldValue, String newValue){
				fillColumnList(newValue);
			}
		});
						
		table.bind(tableSelect.valueProperty());
		HBox columnSelectRow = new HBox(5, tableSelectLabel, tableSelect, columnNames);
		
		//optional generated primary key
		CheckBox toggleGeneratePrimary = new CheckBox("Generate Primary Key");
		TextField generatedKeyName = new TextField("RowID");
		toggleGeneratePrimary.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@SuppressWarnings("rawtypes")
			public void changed(ObservableValue obv, Boolean oldValue, Boolean newValue){
					generatedKeyName.setVisible(newValue);
			}
		});
				
		HBox generatePrimaryKeyRow = new HBox(5, toggleGeneratePrimary, generatedKeyName);
		VBox.setMargin(generatePrimaryKeyRow, new Insets(5, 0, 5, 0));
		
		//Action buttons
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		HBox buttonRow = new HBox(5, ok, cancel);
		buttonRow.setAlignment(Pos.CENTER);
		
		ok.setOnAction(e->{
			if(checkLegalPrimary(toggleGeneratePrimary, generatedKeyName.getText())){
				if(createTable()){
					builderWindow.close();
					tableList.updateList();
				}
			}
			else{
				
				new NotificationWindow("Multiple Primary Key Definitions",
						"Key cannot be simultaneously generated ",
						"and imported from existing columns.", 450, 0);
			}
		});
		
		cancel.setOnAction(e->{
			builderWindow.close();
		});
		
		columnNames.setMaxHeight(300);
		columnNames.setMaxWidth(200);
		
		columnNames.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
			@SuppressWarnings("rawtypes")
			public void changed(ObservableValue arg0, String oldValue, String newValue){
				if(newValue!=null)
					addColumn(newValue);
			}
		});
		
		VBox tableForm = new VBox(5, tableNameRow, columnSelectRow, generatePrimaryKeyRow, 
				columnMeta, buttonRow);
		VBox.setMargin(columnSelectRow, new Insets(10, 0, 10, 0));
		VBox.setMargin(buttonRow, new Insets(10, 0, 0, 0));
		
		Scene scene = new Scene(tableForm);
		
		builderWindow.setMinWidth(600);
		
		builderWindow.setScene(scene);
		builderWindow.show();
	}
	
	/*
	 * This function checks that the user did not select both a 
	 * generated primary key and a key from the imported columns.
	 */
	private boolean checkLegalPrimary(CheckBox toggleGeneratePrimary, String keyName){
		
		if(columnMeta.getItems().size()==0)
			return false;
		
		//false if primary was both set from existing column and generated, 
		//resulting in multiple definitions
		boolean legalPrimary = true;
		
		if(toggleGeneratePrimary.isSelected()){
			for (ColumnProperties col : columnMeta.getItems()){
				if (col.getPrimary())
					legalPrimary = false;
			}
		}
		
		if(legalPrimary){
			//set generated primary key name if the option was selected
			if(toggleGeneratePrimary.selectedProperty().getValue())
					generatedKey = keyName;
		}
		
		return legalPrimary;
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
	 * Adds a ColumnProperties object to the TableView for the selected table
	 * using queries from information_schema.
	 */
	private void addColumn(String newValue){
		//add column meta data to TableView
		try(Connection connection = DriverManager.getConnection(session.getURL(), 
				session.getUserName(), session.getPassword())){
					
			ColumnProperties newColumn = new ColumnProperties(newValue, table.getValue());
				connection.setCatalog("information_schema");
				
			//query for the name and data type of only the column being added
			String queryString = new String("SELECT COLUMN_TYPE FROM "
						+ "COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
						+ "AND COLUMN_NAME = ?;");
			
			PreparedStatement statement = connection.prepareStatement(queryString);
			statement.setString(1, session.getDatabase());
			statement.setString(2, table.getValue());
			statement.setString(3, newValue);
			
			//data type of the column to be added
			ResultSet query = statement.executeQuery();
				
			if(query.next()){
				newColumn.setType(query.getString(1));
			}
				
			//query for any primary or foreign key constraints of the column
			queryString = "SELECT CONSTRAINT_NAME, "
					+ "REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME "
					+ "FROM KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? AND "
					+ "TABLE_NAME = ? AND COLUMN_NAME = ?;";
				
			statement = connection.prepareStatement(queryString);
			statement.setString(1, session.getDatabase());
			statement.setString(2, table.getValue());
			statement.setString(3, newValue);
				
			query = statement.executeQuery();
				
			if(query.next()){
				if(query.getString(1).equals("PRIMARY"))
					//primary constraint exists
					newColumn.setPrimary(true);
				else if(!query.getString(2).equals("NULL"))
					//foreign constraint exists
					newColumn.setForeign(query.getString(2) + "." 
				         + query.getString(3));
			}
				
			columnMeta.getItems().add(newColumn);
			statement.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	/*
	 * This function fills the column ListView with all columns from the table
	 * specified by the ComboBox input.
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
				while(columnData.next())
						columnNames.getItems().add(columnData.getString(1));
							
				statement.close();
				connection.close();
			}
			catch (SQLException ex) {
				//not connected, no tables
				ex.printStackTrace();
			}
		}
	}
	
	
	/*
	 * This function creates the MySQL table with all columns added by the user,
	 * with the columns with the most rows appearing first.  Returns true if the
	 * table is successfully created.
	 */
	private boolean createTable(){
		if(session==null)
			return false;
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())) 
		{
				
			connection.setCatalog(session.getDatabase());
			Statement statement = connection.createStatement();
				
			String statementString;
								
			if(!columnMeta.getItems().isEmpty()){
				
				//all tables which columns are copied from
				ArrayList<SourceTable> tables 
							= new ArrayList<SourceTable>();
				
				createTempTables(columnMeta.getItems(), tables, statement);
					
				joinTables(statement, tables);
			}
				
			//add optional generated key or check for other primary constraints
			if(generatedKey!=null){					
				statementString = "ALTER TABLE " + newTableName.getValue() + " ADD " 
						+ generatedKey + " INT AUTO_INCREMENT NOT NULL PRIMARY "
						+ "KEY FIRST;";
					
				statement.execute(statementString);
			}
			else
				setPrimaryConstraints(statement);
			
			if(!setForeignConstraints(statement))
				return false;
				
			statement.close();
			connection.close();	
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
		return true;
	}
	
	/*
	 * This function creates temporary MySQL tables for each source table
	 * containing all columns the new table will contain from the source table,
	 * adding a SourceTable to the input "tables" for each such table created.
	 */
	private void createTempTables(ObservableList<ColumnProperties> cols, 
								ArrayList<SourceTable> tables, Statement statement)
	throws SQLException{
			
		//sort columns by name of table
		Collections.sort(cols, new ColPropTableComparator());
			
		int tableIndex = 0;
		
		//name of current table
		String currentTable = cols.get(tableIndex).getTable();
			
		//name of the temp table created with all columns added from
		//current table
		String tempTable = "t" + tableIndex;
		
		//add first table
		tables.add(new SourceTable(currentTable, tempTable));
		
		//primary key column definition for all temp tables
		String priString = " (id INT AUTO_INCREMENT PRIMARY KEY) ";
			
		//begin create temp table statement
		String statementString = "CREATE TEMPORARY TABLE " + tempTable + priString + "SELECT ";
			
		for(ColumnProperties c : cols){
			boolean sameTable = c.getTable().equals(currentTable);
			boolean lastColumn = cols.indexOf(c)==cols.size()-1;
			
			if(sameTable && !lastColumn){
				//add column to current table
				tables.get(tableIndex).appendCol(c.getName());
			}
			else{
				if(sameTable)
					tables.get(tableIndex).appendCol(c.getName());
				
				//create table and set rows
				statementString += tables.get(tableIndex).selectableCols();
				statementString += " FROM " + currentTable + ";";
				statement.execute(statementString);
				setTableRows(statement, tables.get(tableIndex));
					
				//prepare control and reference variables for another table
				currentTable = c.getTable();
				tableIndex++;
				tempTable = "t" + tableIndex;
			
				if(!sameTable && lastColumn){
					//create additional temp table for last column
					tables.add(new SourceTable(currentTable, tempTable, c.getName()));
					statementString = "CREATE TEMPORARY TABLE " + tempTable + priString + "SELECT ";
					
					statementString += tables.get(tableIndex).selectableCols();
					statementString += " FROM " + currentTable + ";";
					statement.execute(statementString);
					
					setTableRows(statement, tables.get(tableIndex));
				}
				else if(!lastColumn){
					//begin creating next table
					tables.add(new SourceTable(currentTable, tempTable, c.getName()));
					statementString = "CREATE TEMPORARY TABLE " + tempTable + priString + "SELECT ";
				}
			}
		}
		
	}
	
	private void setTableRows(Statement statement, SourceTable table) 
			throws SQLException{
		
		String statementString = "SELECT COUNT(*) FROM " +
				table.getTemp() + ";"; 					
		ResultSet rs = statement.executeQuery(statementString);
		
		if(rs.next())
			table.setRows(rs.getInt(1));
			
		rs.close();
	}
	
	/*
	 * This function joins the temporary tables created for each SourceTable.
	 * The SourceTable with the most rows is joined with the next largest
	 * table until there are no tables remaining.
	 */
	private void joinTables(Statement statement, ArrayList<SourceTable> tables) 
			throws SQLException{
		//sort SourceTables in order of descending number of rows
		Collections.sort(tables, new MaxTableSizeComparator());
		
		//only one source table
		if(tables.size()==1){
			
			statement.execute("ALTER TABLE " + tables.get(0).getTemp() 
					+ " DROP id;");
			
			statement.execute("CREATE TABLE " + newTableName.getValue() 
			+ " AS SELECT * FROM " + tables.get(0).getTemp() + ";");
			
			return;
		}

		//table use in last iteration of loop
		SourceTable prevTable = tables.get(0);
		tables.remove(0);

		//create first temporary table
		String statementString = "CREATE TEMPORARY TABLE " + prevTable.getTemp() + "Sum "
				 + "AS SELECT * FROM " + prevTable.getTemp();	
		statement.execute(statementString);
			
		for(SourceTable t : tables){

			//current sum table name
			String tableSumName = prevTable.getTemp() + "Sum";
				
			//add all column list from previous table to current table's
			//column list
			t.appendCols(prevTable.cols());
				
			//join previous sum table with current table
			statementString = "CREATE TEMPORARY TABLE " + t.getTemp() + "Sum AS SELECT " +
					tableSumName + ".id, " + t.selectableCols() + " FROM " + tableSumName 
					+ " LEFT OUTER JOIN " + t.getTemp()	+ " ON " + tableSumName + 
					".id = " + t.getTemp() + ".id;";
			statement.execute(statementString);
				
			if(tables.indexOf(t)==tables.size()-1){
				//on last SourceTable, create final table
				
				//current sum table name
				tableSumName = t.getTemp() + "Sum";

				statement.execute("ALTER TABLE " + tableSumName + " DROP id;");
				
				statementString = "CREATE TABLE " + newTableName.getValue() + " AS SELECT * FROM "
							+ tableSumName;
				statement.execute(statementString);
			}
			
			prevTable = t;
		}
	}
	
	/*
	 * This function finds the column name of the primary key of
	 * the MySQL with the name of the parameter "tableName".
	 */
	void removePrimaryKey(String tableName, Statement statement)
		throws SQLException{
		
		//find column name of primary key
		String queryString = "SELECT COLUMN_NAME FROM "
				+ "INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ?"
				+ " AND TABLE_NAME = ? and CONSTRAINT_NAME = \'PRIMARY\'";
		
		String primary = new String();
				
		PreparedStatement prepStatement = 
			statement.getConnection().prepareStatement(queryString);
		
		prepStatement.setString(1, session.getDatabase());
		prepStatement.setString(2, tableName);
		
		//primary key
		ResultSet rs = prepStatement.executeQuery();
		
		if(rs.next()){
			primary = rs.getString(1);
		}
		
		prepStatement.close();
		
		//drop primary key column
		String statementString = "ALTER TABLE " + tableName + " DROP "
				+ primary + ";";
		
		statement.execute(statementString);
	}
	
	
	/*
	 * This function adds any primary constraints defined to the new
	 * table.
	 */
	private void setPrimaryConstraints(Statement statement)
			throws SQLException{
		
		ObservableList<ColumnProperties> cols = columnMeta.getItems();
		
		//true if any column is a primary key
		boolean primary = false;
		
		for(ColumnProperties c : cols){
			if(c.getPrimary())
				primary = true;
		}
		
		if(primary){
			String statementString = "ALTER TABLE " + newTableName.getValue()
				+ " ADD PRIMARY KEY(";
			
			//add all columns selected as a primary key to the primary key
			//definition
			for(ColumnProperties c : cols){
				if(c.getPrimary()){
					statementString += c.getName() + ", ";
				}
			}
			
			statementString = statementString.substring(0, 
					statementString.length()-2) + " );";
			
			statement.execute(statementString);
		}
		
	}
	
	/*
	 * This function adds any primary constraints defined to the new
	 * table.
	 */
	private boolean setForeignConstraints(Statement statement)
			throws SQLException{
		
		ObservableList<ColumnProperties> cols = columnMeta.getItems();
		
		//true if any column is a foreign key
		boolean foreign = false;
		
		for(ColumnProperties c : cols){
			if(!c.getForeign().isEmpty())
				foreign = true;
		}
		
		if(foreign)
			for(ColumnProperties c : cols){
				String foreignInput = c.getForeign();
				
				//table and column referenced by input
				String referencedTable;
				String referencedColumn;
				
				//attempt to extract table and column from input
				if(foreignInput.contains(".")){
					//assumes input is referencedTable.referencedColumn
					referencedTable = foreignInput.substring(0, foreignInput.indexOf("."));
					referencedColumn = foreignInput.substring(foreignInput.indexOf(".")+1,
							foreignInput.length());
				}
				
				else if(foreignInput.contains("(")
						&& (foreignInput.indexOf( "(" ) < foreignInput.indexOf( ")" ))){
					//assumes input is referencedTable(referencedColumn)
					referencedTable = foreignInput.substring(0, foreignInput.indexOf( "(" ));
					referencedColumn = foreignInput.substring(foreignInput.indexOf( "(" )+1,
							foreignInput.indexOf( ")" ));
				}
				else{
					//unable to process input
					new NotificationWindow("Invalid Foreign Key",
							"Enter as TABLENAME.COLUMNNAME or",
							"TABLENAME(COLUMNNAME)");
					return false;
				}
				
				
				String statementString = "ALTER TABLE " + newTableName.getValue()
						+ " ADD FOREIGN KEY (" + c.getName() + ") REFERENCES "
						+ referencedTable + " (" + referencedColumn + ");";
				
				statement.execute(statementString);
			}
		return true;
	}
	
	
	/*
	 * Changes properties in the name column to display the column's
	 * full name (includes table and column name)
	 */
	@SuppressWarnings("unchecked")
	private void changeColToFullName(){
		for(TableColumn<ColumnProperties, ?> tc : columnMeta.getColumns()){
			if(tc.getText().equals("Column")){
							
				((TableColumn<ColumnProperties, String>)tc).setCellValueFactory(
						new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
					public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
						if(param.getValue()==null)
							return new SimpleStringProperty("");
						
						return new SimpleStringProperty(param.getValue().getFullNameValue());
					}
				});
				
				tc.setMinWidth(200);
				
				tc.setOnEditCommit(t -> {
					t.getRowValue().setFullName(t.getNewValue().toString());
				});
			}
		}
	}
}