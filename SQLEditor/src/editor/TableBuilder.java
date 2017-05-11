package editor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import columninfo.*;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

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
	private TableView<ColumnProperties> columnMeta;
	
	public TableBuilder(SessionConfig newSession){
		session = newSession;
		newTableName = new SimpleStringProperty();
		table = new SimpleStringProperty();
	}
	
	@SuppressWarnings("unchecked")
	public void buildTable(){
		
		columnMeta = ColumnProperties.getEditableColumns();
		
		for(TableColumn<ColumnProperties, ?> tc : columnMeta.getColumns()){
			if(tc.getText().equals("Column")){
							
				((TableColumn<ColumnProperties, String>)tc).setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
					public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
						if(param.getValue()==null)
							return new SimpleStringProperty("");
						
						return new SimpleStringProperty(param.getValue().getFullName());
					}
				});
				
				tc.setMinWidth(200);
			}
		}
		
		
		Stage builderWindow = new Stage();
		builderWindow.setTitle("Define Table");
		
		//Imported Table Name
		Label tableNameLabel = new Label("Table Name: ");
		HBox.setMargin(tableNameLabel, new Insets(3, 0, 0, 0));
		TextField tableNameInput = new TextField();
		tableNameInput.textProperty().bindBidirectional(newTableName);
		tableNameInput.setText("Table1");
		HBox tableNameRow = new HBox(5, tableNameLabel, tableNameInput);
		
		//select table of column
		Label tableSelectLabel = new Label("Table: ");
		HBox.setMargin(tableSelectLabel, new Insets(3, 0, 0, 0));
		ComboBox<String> tableSelect = new ComboBox<String>();
					
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				connection.setCatalog(session.getDatabase());
				
				Statement statement = connection.createStatement();
				ResultSet tables = statement.executeQuery("show tables;");
					
				while(tables.next()){
					tableSelect.getItems().add(tables.getString(1));
				}
						
				tableSelect.setValue(SQLEditor.getTableName());
						
				if(!tableSelect.getValue().equals(""))
					fillColumnList(SQLEditor.getTableName());
					
				statement.close();
				connection.close();
			}
			catch (SQLException ex) {
				//not connected, no tables
				ex.printStackTrace();
			}
		}
				
		tableSelect.valueProperty().addListener(new ChangeListener<String>(){
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
			boolean legalPrimary = true;
			
			if(toggleGeneratePrimary.isSelected()){
				for (ColumnProperties col : columnMeta.getItems()){
					if (col.getPrimary())
						legalPrimary = false;
				}
			}
			
			if(legalPrimary){
				if(!columnMeta.getItems().isEmpty()){
				
					if(toggleGeneratePrimary.selectedProperty().getValue())
					generatedKey = generatedKeyName.getText();
				
					createTable();
				}
				builderWindow.close();
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
			builderWindow.close();
		});
		
		columnNames.setMaxHeight(300);
		columnNames.setMaxWidth(200);
		
		columnNames.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue arg0, String oldValue, String newValue){
				
				if(newValue!=null){
					//add column meta data to tableview
					try(Connection connection = DriverManager.getConnection(session.getURL(), 
							session.getUserName(), session.getPassword())){
						
						
						ColumnProperties newColumn = new ColumnProperties(newValue, table.getValue());
						connection.setCatalog("information_schema");
						
						String queryString = new String("SELECT COLUMN_TYPE, COLUMN_KEY FROM "
								+ "COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
								+ "AND COLUMN_NAME = ?;");
						
						PreparedStatement statement = connection.prepareStatement(queryString);
						statement.setString(1, session.getDatabase());
						statement.setString(2, table.getValue());
						statement.setString(3, newValue);
						
						ResultSet query = statement.executeQuery();
						
						if(query.next()){
							newColumn.setType(query.getString(1));
							newColumn.setPrimary(query.getString(2).equals("PRI"));
						}
						
						queryString = "SELECT CONSTRAINT_NAME, REFERENCED_TABLE_SCHEMA, "
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
								newColumn.setPrimary(true);
							else if(!query.getString(2).equals("NULL"))
								newColumn.setForeign(query.getString(2) + "." 
							         + query.getString(3) + "." + query.getString(4));
						}
						
						columnMeta.getItems().add(newColumn);
						
						statement.close();
						connection.close();
					}
					catch(SQLException ex){
						ex.printStackTrace();
					}
				}
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
	
	private void fillColumnList(String tableName){
		
		columnNames.getItems().clear();
		
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				connection.setCatalog(session.getDatabase());
				
				Statement statement = connection.createStatement();
				ResultSet columnData = statement.executeQuery("SHOW COLUMNS FROM " 
						+ tableName + ";");
			
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
	
	private void createTable(){		
		if(session!=null){
			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				//shorthand reference to ColumnProperties
				ObservableList<ColumnProperties> cols = columnMeta.getItems();			
				
				connection.setCatalog(session.getDatabase());
				Statement statement = connection.createStatement();
				
				String queryString = new String("CREATE TABLE " + newTableName.getValue());
				String primaryKey;
				if(cols.size()>0){
					queryString += " (";
					
					for(ColumnProperties c : cols){
						
						primaryKey = (c.getPrimary()) ? " PRIMARY KEY" : "";
						
						queryString += " " + c.getName() + " " + 
								c.getType() + primaryKey + ",";
					}
					
					queryString = queryString.substring(0, queryString.length()-1) + ");";
				}
				
				statement.execute(queryString);
				
				if(cols.size()>0){
								
					for(int i = 0; i < cols.size(); i++){
						queryString = "INSERT INTO " + newTableName.getValue() + "("
								+ cols.get(i).getName() + ") SELECT " + cols.get(i).getName()
								+ " FROM " + cols.get(i).getTable() + ";";
						statement.execute(queryString);
					}
				}
				
				
				if(generatedKey!=null){					
					queryString = "ALTER TABLE " + newTableName.getValue() + " ADD " + generatedKey + 
							" INT AUTO_INCREMENT NOT NULL PRIMARY KEY FIRST;";
					
					statement.execute(queryString);
				}
				
				statement.close();
				connection.close();
				
				SQLEditor.initTableList();
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		}
	}
}