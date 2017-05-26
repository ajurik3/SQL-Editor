package editor;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import columninfo.ColumnProperties;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

/*
 * This class creates a form for the user to specify
 * a file from which to import a new table.  When the path
 * and field delimiter are provided by the user, the class
 * attempts to load the table's column names and suggested
 * MySQL data types into a TableView, which the user can edit.
 */
public class FileImporter {
	
	//name of the new table, bound to TextField input
	private StringProperty tableName = new SimpleStringProperty();
	
	//local file containing data and column names of new table
	private File selectedFile;
	
	//path of the local file, bound to TextField input
	private StringProperty pathName = new SimpleStringProperty();
	
	//delimiter used to separate fields in the file, bound to TextField input
	private StringProperty delimiter = new SimpleStringProperty();
	
	//database to add the imported table to, bound to ComboBox input
	private StringProperty database = new SimpleStringProperty();

	//list of column names
	private ObservableList<String> columns = FXCollections.observableArrayList();
	
	//list of column types
	private ObservableList<String> columnTypes = FXCollections.observableArrayList();
	
	//name of optional generated primary key
	private String generatedKey;
	
	//TableView of ColumnProperties of columns to be imported from file
	private TableView<ColumnProperties> columnMeta = ColumnProperties.getPropertyColumns();

	//current connection information
	private SessionConfig session;
	
	FileImporter(SessionConfig currentSession, ListView<String> tables, 
			TableView<ObservableList<String>> currentTableReference){
		session = currentSession;
		
		//when pathName is changed, check if column 
		//properties should be loaded to table
		pathName.addListener(new ChangeListener<String>(){
			@SuppressWarnings("rawtypes")
			@Override
			public void changed(ObservableValue obv, String oldValue, String newValue){
				selectedFile = new File(newValue);
				//load if delimiter and pathname are both defined
				if(delimiter.getValue()!=null&&!delimiter.getValue().isEmpty()
						&&(!newValue.isEmpty())){
					initializeColumnsFromFile();
				}
			}
		});
		
		//when delimiter is changed, check if column 
		//properties should be loaded to table
		delimiter.addListener(new ChangeListener<String>(){
				@SuppressWarnings("rawtypes")
				public void changed(ObservableValue obv, String oldValue, String newValue){
					
					//load if delimiter and pathname are both defined
					if(pathName.getValue()!=null&&!pathName.getValue().isEmpty()
							&&(!newValue.isEmpty())){
						initializeColumnsFromFile();
					}
				}
		});
	}
	/*
	 * Creates the form used to specify table information, including
	 * 
	 * New Table Name (TextField)
	 * Path Name (TextField and FileChooser)
	 * Delimiter (TextField)
	 * Database (ComboBox)
	 * Column Properties (TableView)
	 * Optional Auto-Incremented Primary Key Name (CheckBox and TextField)
	 */
	public void getFile(){
		Stage importWindow = new Stage();
		
		//imported table name input
		Label tableNameLabel = new Label("Table Name: ");
		HBox.setMargin(tableNameLabel, new Insets(3, 0, 0, 0));
		TextField tableNameInput = new TextField();
		tableNameInput.textProperty().bindBidirectional(tableName);
		tableNameInput.setText("Table1");
		HBox tableNameRow = new HBox(5, tableNameLabel, tableNameInput);
		
		//imported local file location input
		TextField pathInput = new TextField();
		pathInput.textProperty().bindBidirectional(pathName);
		pathInput.setPrefColumnCount(20);
		Button browse = new Button("Browse");
		HBox fileInput = new HBox(5, pathInput, browse);
		
		//open a browser for user to enter file path graphically
		browse.setOnAction(e ->{
			FileChooser chooser = new FileChooser();
			String[] textFiles = {"*.txt", "*.csv"};

			chooser.getExtensionFilters().addAll(
						new ExtensionFilter("Text Files", textFiles),
						new ExtensionFilter("All Files", "*.*"));
			
			pathName.set(chooser.showOpenDialog(importWindow).getAbsolutePath());
		});
		
		//File delimiter
		Label delim = new Label("Delimiter:");
		HBox.setMargin(delim, new Insets(3, 0, 0, 0));
		TextField delimInput = new TextField();
		delimInput.textProperty().bindBidirectional(delimiter);
		delimInput.setPrefColumnCount(5);
		HBox delimRow = new HBox(5, delim, delimInput);
		
		//database to import table
		Label dbSelectLabel = new Label("Database: ");
		HBox.setMargin(dbSelectLabel, new Insets(3, 0, 0, 0));
		ComboBox<String> dbSelect = new ComboBox<String>();
		
		//populate ComboBox with all databases in catalog
		if(session!=null){
			fillDatabaseComboBox(dbSelect);
		}
		
		database.bind(dbSelect.valueProperty());
		
		HBox dbSelectRow = new HBox(5, dbSelectLabel, dbSelect);
		
		//label for imported columns table
		Label columnSelectLabel = new Label("Columns imported from first line (click to edit)");
		HBox columnSelectRow = new HBox(5, columnSelectLabel);
		VBox.setMargin(columnSelectRow, new Insets(20, 0, 0, 0));
		
		//optional generated primary key
		CheckBox toggleGeneratePrimary = new CheckBox("Generate Primary Key");
		TextField generatedKeyName = new TextField("RowID");
		toggleGeneratePrimary.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@SuppressWarnings("rawtypes")
			public void changed(ObservableValue obv, Boolean oldValue, Boolean newValue){
					generatedKeyName.setVisible(newValue);
			}
		});
		toggleGeneratePrimary.setSelected(true);
		
		HBox generatePrimaryKeyRow = new HBox(5, toggleGeneratePrimary, generatedKeyName);
		VBox.setMargin(generatePrimaryKeyRow, new Insets(5, 0, 5, 0));
		
		//action buttons
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		HBox buttonRow = new HBox(5, ok, cancel);
		buttonRow.setAlignment(Pos.CENTER);
		
		ok.setOnAction(e ->{
			if(checkLegalPrimary(toggleGeneratePrimary, generatedKeyName.getText())){
				if(importTable())
					importWindow.close();
			}
			else{
				new NotificationWindow("Multiple Primary Key Definitions",
						"Key cannot be simultaneously generated ",
						"and imported from existing columns.", 450, 0);
			}
		});
		
		cancel.setOnAction(e->{
			importWindow.close();
		});
		
		VBox importForm = new VBox(5, fileInput, tableNameRow, delimRow, dbSelectRow, 
				columnSelectRow, generatePrimaryKeyRow, columnMeta, buttonRow);
		
		Scene scene = new Scene(importForm);
		importWindow.setScene(scene);
		importWindow.setTitle("Import Table");
		importWindow.show();	
	}
	
	/*
	 * This function fills the ComboBox with all databases in the 
	 * current catalog.
	 */
	private void fillDatabaseComboBox(ComboBox<String> dbSelect){
		try (Connection connection = DriverManager.getConnection(session.getURL(), 
				session.getUserName(), session.getPassword())) 
		{
			Statement statement = connection.createStatement();
			ResultSet databases = statement.executeQuery("show databases;");
		
			while(databases.next()){
				dbSelect.getItems().add(databases.getString(1));
			}
			
			//default value is database being used in main screen
			if(connection.getCatalog()!=null)
				dbSelect.setValue(connection.getCatalog());
		
			statement.close();
			connection.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
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
	 * This function creates a table with the ColumnProperties objects from
	 * user input and loads them with data from the specified local file.
	 * Returns true if the table is successfully created.
	 */
	private boolean importTable(){
		
		if(session==null)
			return false;

		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())) 
		{
			connection.setCatalog(session.getDatabase());
			Statement statement = connection.createStatement();
				
			//CREATE TABLE statement string
			String statementString = getStatementString();
				
			if(!statementString.isEmpty()){
				statement.execute(statementString);
			}
			else{
				return false;
			}
				
			//change all "\" to "\\" for MySQL escape character
			String path = pathName.get().replace("\\", "\\\\");
				
			//load MySQL table with data from local file
			statementString = "LOAD DATA LOCAL INFILE \'" + path + "\' INTO TABLE " + tableName.getValue() +
				" FIELDS TERMINATED BY \'" + delimiter.getValue() + "\' " +
				" LINES TERMINATED BY \'\\r\\n\' IGNORE 1 LINES;";
			statement.execute(statementString);
			
			//generate auto increment primary key if one was specified 
			if(generatedKey!=null){					
				statementString = "ALTER TABLE " + tableName.getValue() + " ADD " + generatedKey + 
						" INT AUTO_INCREMENT NOT NULL PRIMARY KEY FIRST;";
				statement.execute(statementString);
			}
				
			//refresh table list on main screen
			SQLEditor.initTableList();
				
			statement.close();
			connection.close();
		}
		catch(SQLException e){
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

/*
 * This function creates a CREATE TABLE statement with the column definitions and
 * constraints specified by the imported ColumnProperties objects.
 */
	String getStatementString(){
		//begin create table statement
		String statementString = new String("CREATE TABLE " + tableName.getValue()) + " (";
		
		//specifies whether each column is part of the primary key
		String primaryKey;

		//append each column definition to statementString
		for(ColumnProperties c : columnMeta.getItems()){
			
			primaryKey = (c.getPrimary()) ? " PRIMARY KEY" : "";
			
			statementString += " " + c.getName() + " " + 
					c.getType() + primaryKey + ",";
		}
		
		//check if any foreign key constraints are defined
		boolean foreignKey = false;
		for(ColumnProperties c : columnMeta.getItems()){
			if(!c.getForeign().isEmpty())
				foreignKey = true;
		}
		
		//append any foreignKey constraints and complete statement
		if(foreignKey){
			statementString = appendConstraints(statementString);
		}
		else
			statementString = statementString.substring(0, 
					statementString.length()-1) + ");";
		
		return statementString;
	}
	
	/*
	 * This function appends constraints to the create table statement
	 * for each imported column which specifies a foreign key constraint.
	 */
	private String appendConstraints(String statementString){
		for(ColumnProperties c : columnMeta.getItems()){
			if(!c.getForeign().isEmpty()){
				
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
					return "";
				}
				//append constraint to statement
				statementString +=
					"FOREIGN KEY (" + c.getName() + ") REFERENCES "
					+ referencedTable + " (" + referencedColumn + " ), ";
			}
		}
		
		
		statementString = statementString.substring(0, statementString.length()-2) + ");";
		
		return statementString;
	}
	
	/*
	 * This function loads the ColumnProperties table by reading the first two
	 * lines of the file into two separate scanners.  The first row's scanner
	 * is used to read in the names, and the second row's scanner tries to 
	 * suggest data types for the column based on the fields in the first data
	 * row. The lists generated by these two scanners are used to fill the table
	 * suggested names and types for the column, with the primary and foreign
	 * key fields blank by default.
	 */
	void initializeColumnsFromFile(){

		//clear any existing data from previously selected files
		columns.clear();
		columnTypes.clear();
		columnMeta.getItems().clear();
		
		try{
			
			Scanner scanner = new Scanner(selectedFile);
			
			//read in first line of scanner to retrieve column names
			String columnNames = scanner.nextLine();
			
			//read in second line of scanner to attempt to recognize data types
			String firstRow = scanner.nextLine();
			scanner.close();
			
			//set scanner to row of column names
			scanner = new Scanner(columnNames);
			scanner.useDelimiter(delimiter.getValue());
			
			//add all column names to column list
			while(scanner.hasNext()){
				
				String columnName = getValidName(scanner.next());
				columns.add(columnName);
			}

			scanner.close();
			
			//set scanner to first row of data
			scanner = new Scanner(firstRow);
			scanner.useDelimiter(delimiter.getValue());
			
			//attempt to identify each field as an int, double, or VarChar
			while(scanner.hasNext()){
				String entry = scanner.next();
				entry = entry.trim();
				
				try{
					Integer.parseInt(entry);
					columnTypes.add("INT");
				}
				catch(NumberFormatException e){
					try{
						Double.parseDouble(entry);
						columnTypes.add("DOUBLE(12, 2)");
					}
					catch(NumberFormatException ex){
						columnTypes.add("VARCHAR(20)");
					}
				}
			}

			scanner.close();
		}
		catch(FileNotFoundException f){
			f.printStackTrace();
		}
		
		//load suggested column names and types into column table with false primary key
		//property and empty foreign key property
		for(int i = 0; i < columns.size(); i++){			
			columnMeta.getItems().add(new ColumnProperties(columns.get(i), columnTypes.get(i),
					false, ""));
		}
	}
	
	//removes some problematic characters from imported column names
	String getValidName(String name){
		name = name.replace("%", "_PER");
		name = name.replace("\\", "");
		name = name.replace("\'", "");
		name = name.replace("\"", "");
		name = name.replace(" ", "");
		return name;	
	}
}