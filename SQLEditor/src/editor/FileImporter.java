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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;

public class FileImporter {

private File selectedFile;
	
	private StringProperty tableName = new SimpleStringProperty();
	private StringProperty pathName = new SimpleStringProperty();
	private StringProperty delimiter = new SimpleStringProperty();
	private StringProperty database = new SimpleStringProperty();

	private ObservableList<String> columns = FXCollections.observableArrayList();
	private ObservableList<String> columnTypes = FXCollections.observableArrayList();
	private String generatedKey;
	private TableView<ColumnProperties> columnMeta = ColumnProperties.getEditableColumns();

	private SessionConfig session;
	
	FileImporter(SessionConfig currentSession, ListView<String> tables, 
			TableView<ObservableList<String>> currentTableReference){
		session = currentSession;
		
		pathName.addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue obv, String oldValue, String newValue){
				selectedFile = new File(newValue);
				columns.clear();
				columnTypes.clear();
				
				if(delimiter.getValue()!=null&&delimiter.getValue()!=""&&(!newValue.isEmpty())){
					initializeColumnsFromFile();
					updateMetaTable();
				}
			}
		});
		
		delimiter.addListener(new ChangeListener<String>(){
				public void changed(ObservableValue obv, String oldValue, String newValue){
					
					columns.clear();
					columnTypes.clear();
					
					if(pathName.getValue()!=null&&pathName.getValue()!=""&&(!newValue.isEmpty())){
						initializeColumnsFromFile();
						updateMetaTable();
					}
				}
		});
	}
	
	public void getFile(){
		
		Stage importWindow = new Stage();
		
		//Imported Table Name
		Label tableNameLabel = new Label("Table Name: ");
		HBox.setMargin(tableNameLabel, new Insets(3, 0, 0, 0));
		TextField tableNameInput = new TextField();
		tableNameInput.textProperty().bindBidirectional(tableName);
		tableNameInput.setText("Table1");
		HBox tableNameRow = new HBox(5, tableNameLabel, tableNameInput);
		
		//Imported Local File Location
		TextField pathInput = new TextField();
		pathInput.textProperty().bindBidirectional(pathName);
		pathInput.setPrefColumnCount(20);
		Button browse = new Button("Browse");
		HBox fileInput = new HBox(5, pathInput, browse);
		
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
		
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				Statement statement = connection.createStatement();
				ResultSet databases = statement.executeQuery("show databases;");
			
				while(databases.next()){
					dbSelect.getItems().add(databases.getString(1));
				}
				
				dbSelect.setValue(connection.getCatalog());
			
				statement.close();
				connection.close();
			}
			catch (SQLException ex) {
			//not connected, no databases
				ex.printStackTrace();
			}
		}
		
		database.bind(dbSelect.valueProperty());
		
		HBox dbSelectRow = new HBox(5, dbSelectLabel, dbSelect);
		
		//Label for imported columns table
		Label columnSelectLabel = new Label("Columns imported from first line (click to edit)");
		HBox columnSelectRow = new HBox(5, columnSelectLabel);
		VBox.setMargin(columnSelectRow, new Insets(20, 0, 0, 0));
		
		//optional generated primary key
		CheckBox toggleGeneratePrimary = new CheckBox("Generate Primary Key");
		TextField generatedKeyName = new TextField("RowID");
		toggleGeneratePrimary.selectedProperty().addListener(new ChangeListener<Boolean>(){
			public void changed(ObservableValue obv, Boolean oldValue, Boolean newValue){
					generatedKeyName.setVisible(newValue);
			}
		});
		//toggleGeneratePrimary.setSelected(true);
		
		HBox generatePrimaryKeyRow = new HBox(5, toggleGeneratePrimary, generatedKeyName);
		VBox.setMargin(generatePrimaryKeyRow, new Insets(5, 0, 5, 0));
		
		//Action buttons
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		HBox buttonRow = new HBox(5, ok, cancel);
		buttonRow.setAlignment(Pos.CENTER);
		
		ok.setOnAction(e ->{
			
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
				if(!columns.isEmpty()){
				
					if(toggleGeneratePrimary.selectedProperty().getValue())
					generatedKey = generatedKeyName.getText();
				
					importTable();
				}
				importWindow.close();
			}
			else{
				Stage keyErrorWindow = new Stage();
				keyErrorWindow.setTitle("Multiple Primary Key Definitions");
				Button okButton = new Button("OK");

				okButton.setOnAction(v ->{
					keyErrorWindow.close();
				});
				
				HBox okButtonRow = new HBox(okButton);
				okButtonRow.setAlignment(Pos.CENTER);
				
				VBox keyErrorContainer = new VBox(5,
						new Label("Key cannot be simultaneously generated "
								+ "and imported from existing columns."),
						okButtonRow);
				
				keyErrorWindow.setScene(new Scene(keyErrorContainer));
				keyErrorWindow.show();
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
	
	void importTable(){
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				
				connection.setCatalog(session.getDatabase());
				Statement statement = connection.createStatement();
				
				String queryString = new String("CREATE TABLE " + tableName.getValue());
				String primaryKey;
				if(columns.size()>0){
					queryString += " (";
					
					for(int i = 0; i < columns.size(); i++){
						
						primaryKey = (columnMeta.getItems().get(i).getPrimary()) ? " PRIMARY KEY" : "";
						
						queryString += " " + columnMeta.getItems().get(i).getName() + " " + 
								columnMeta.getItems().get(i).getType() + primaryKey + ",";
					}
					
					queryString = queryString.substring(0, queryString.length()-1) + ");";
				}
				
				statement.execute(queryString);
				
				String path = pathName.get().replace("\\", "\\\\");
				
				queryString = "LOAD DATA LOCAL INFILE \'" + path + "\' INTO TABLE " + tableName.getValue() +
						" FIELDS TERMINATED BY \'" + delimiter.getValue() + "\' " +
						" LINES TERMINATED BY \'\\r\\n\' IGNORE 1 LINES;";

				statement.execute(queryString);
				
				if(generatedKey!=null){					
					queryString = "ALTER TABLE " + tableName.getValue() + " ADD " + generatedKey + 
							" INT AUTO_INCREMENT NOT NULL PRIMARY KEY FIRST;";
					
					statement.execute(queryString);
				}
				
				SQLEditor.initTableList();
				
				statement.close();
				connection.close();
			}
			catch(SQLException e){
				e.printStackTrace();
			}
		}
	}

	void updateMetaTable(){
		
		for(int i = 0; i < columns.size(); i++){			
			columnMeta.getItems().add(new ColumnProperties(columns.get(i), columnTypes.get(i),
					false, ""));
		}
	}
	
	
	void initializeColumnsFromFile(){

		columns.clear();
		columnTypes.clear();
		
		try{
			
			Scanner scanner = new Scanner(selectedFile);
			String columnNames = scanner.nextLine();
			String firstRow = scanner.nextLine();
			scanner.close();
			
			scanner = new Scanner(columnNames);
			scanner.useDelimiter(delimiter.getValue());
			
			while(scanner.hasNext()){
				
				String columnName = getColumnName(scanner.next());
				columns.add(columnName);
			}

			scanner.close();
			
			scanner = new Scanner(firstRow);
			scanner.useDelimiter(delimiter.getValue());
			
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
						columnTypes.add("DOUBLE");
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
	}
	
	String getColumnName(String name){
		name = name.replace("%", "_PER");
		name = name.replace("\\", "");
		name = name.replace("\'", "");
		name = name.replace("\"", "");
		name = name.replace("_", "");
		name = name.replace(" ", "");
		return name;	
	}
	
	
	void setSelectedFile(File newFile){
		selectedFile = newFile;
	}
	
	File getSelectedFile(){
		return selectedFile;
	}
	
}