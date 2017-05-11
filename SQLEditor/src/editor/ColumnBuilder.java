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

public class ColumnBuilder {

	private StringProperty newColumnName;
	private SessionConfig session;
	private StringProperty table;
	private TextArea columnDefinition = new TextArea();
	private BooleanProperty numericColumn = new SimpleBooleanProperty(false);

	private ListView<String> columnNames = new ListView<String>();
	
	public ColumnBuilder(SessionConfig currentSession){
		newColumnName = new SimpleStringProperty();
		table = new SimpleStringProperty();
		session = currentSession;
		
		numericColumn.addListener(new InvalidationListener(){
			public void invalidated(Observable arg0){
				if(table!=null&&!table.getValue().equals(""))
					fillColumnList(table.getValue());
		}
		});
		
	}
	
	public void buildColumn(){
		Stage builderWindow = new Stage();
		
		//Imported column Name
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
		HBox tableSelectRow = new HBox(5, tableSelectLabel, tableSelect);
		
		CheckBox showNumericTypes = new CheckBox("Show Numeric Types Only");
		numericColumn.bind(showNumericTypes.selectedProperty());

		VBox columnMetaDataVBox = new VBox(5, tableSelectRow, showNumericTypes);
		
		HBox columnSelectRow = new HBox(5, columnMetaDataVBox, columnNames);
		columnSelectRow.setAlignment(Pos.CENTER);
		
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
			createColumn();
			builderWindow.close();
		});
		
		cancel.setOnAction(e -> {
			builderWindow.close();
		});
		
		HBox buttonRow = new HBox(5, ok, cancel);
		buttonRow.setAlignment(Pos.CENTER);
		
		columnNames.setMaxHeight(300);
		columnNames.setMaxWidth(200);
		
		columnNames.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue arg0, String oldValue, String newValue){
				
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
			
				while(columnData.next()){
					if(numericColumn.getValue()){
						String columnName = columnData.getString(1);
						if(isNumeric(columnData.getString(2)))
							columnNames.getItems().add(columnName);
					}
					else
						columnNames.getItems().add(columnData.getString(1));
				}
							
				statement.close();
				connection.close();
			}
			catch (SQLException ex) {
				//not connected, no tables
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
				
				String expression = columnDefinition.getText().replace("\n", "");
				
				String type = numericColumn.getValue() ? "DOUBLE PRECISION(10, 2)" : "VARCHAR(50)";
				
				String queryString = new String("ALTER TABLE " + table.getValue()
				     + " ADD COLUMN " + newColumnName.getValue() + " " + type + " AS ("
				     + expression + ");");
				
				statement.execute(queryString);
				
				if(SQLEditor.getTableName().equals(table.getValue())){
					SQLEditor.initTable();
				}
				
				statement.close();
				connection.close();
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		}
	}
	
	private static boolean isNumeric(String type){
		return type.contains("int")||type.contains("float")||type.contains("double")||type.contains("DECIMAL");
	}
}