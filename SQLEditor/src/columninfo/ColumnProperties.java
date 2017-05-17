package columninfo;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

public class ColumnProperties {
	

	StringProperty columnName;
	
	//the name of the MySQL table which originally contains the column
	StringProperty tableName;
	
	//indicates the MySQL data type of the column
	StringProperty columnType;
	
	//full name of column represented as: tableName + "." + columnName
	StringProperty fullName;
	
	//true if the column will be part of the tables primary key
	//assumed false if not explicitly set
	BooleanProperty primaryKey;
	
	//contains the table and column of a foreign key of the column
	//represented as tableName.columnName, with "" indicating no
	//constraint
	StringProperty foreignKey;
	
	public ColumnProperties(){
		columnName = new SimpleStringProperty();
		tableName = new SimpleStringProperty();
		columnType = new SimpleStringProperty();
		primaryKey = new SimpleBooleanProperty(false);
		foreignKey = new SimpleStringProperty("");
	}
	
	public ColumnProperties(String name){
		columnName = new SimpleStringProperty(name);
		tableName = new SimpleStringProperty();
		columnType = new SimpleStringProperty();
		primaryKey = new SimpleBooleanProperty(false);
		foreignKey = new SimpleStringProperty("");
	}
	
	public ColumnProperties(String name, String table){
		columnName = new SimpleStringProperty(name);
		tableName = new SimpleStringProperty(table);
		columnType = new SimpleStringProperty();
		primaryKey = new SimpleBooleanProperty(false);
		foreignKey = new SimpleStringProperty("");
		fullName = new SimpleStringProperty(table + "." + name);
	}

	public ColumnProperties(String name, String type, boolean primary, String foreign){
		columnName = new SimpleStringProperty(name);
		tableName = new SimpleStringProperty();
		columnType = new SimpleStringProperty(type);
		primaryKey = new SimpleBooleanProperty(primary);
		foreignKey = new SimpleStringProperty(foreign);
	}
	
	/*
	 * This function returns a ColumnProperties TableView which can be used to
	 * view and edit the columnName, columnType, primaryKey, and foreignKey values.
	 * 
	 * The function sets CellFactory, CellValueFactory, and OnEditCommit properties
	 * for each column, except the primaryKey column OnEditCommit property, which is
	 * unnecessary with the BoolCheckBoxCell.
	 * 
	 * The cells for columnName, columnType, and foreignKey are defined to behave
	 * identically, except that the CellValueFactory retrieves its values from each
	 * different property and the OnEditCommit sets the edit values to each different
	 * property.
	 */
	
	public static TableView<ColumnProperties> getPropertyColumns(){
		TableView<ColumnProperties> table = new TableView<ColumnProperties>();
		table.setEditable(true);
		table.setPlaceholder(new Label("No columns imported"));
		
		//COLUMN DECLARATIONS
		
		TableColumn<ColumnProperties, String> nameColumn = 
				new TableColumn<ColumnProperties, String>("Column");
		
		TableColumn<ColumnProperties, String> typeColumn = 
				new TableColumn<ColumnProperties, String>("Column Type");
		
		TableColumn<ColumnProperties, Boolean> primaryColumn =
				new TableColumn<>("Primary Key");
		
		TableColumn<ColumnProperties, String> foreignColumn =
				new TableColumn<>("Foreign Key");
		
		//CELL FACTORY SETTINGS
		
		nameColumn.setCellFactory(new EditableColPropFactory());
		typeColumn.setCellFactory(new EditableColPropFactory());
		
		primaryColumn.setCellFactory(new Callback<TableColumn<ColumnProperties, Boolean>, 
				TableCell<ColumnProperties, Boolean>>(){
			public BoolCheckBoxCell call(TableColumn<ColumnProperties, Boolean> col){

				return new BoolCheckBoxCell();
			}
		});
		
		foreignColumn.setCellFactory(new EditableColPropFactory());
		
		//CELL VALUE FACTORY SETTINGS
		
		nameColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
			public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
				if(param.getValue()==null)
					return new SimpleStringProperty("");
				
				return new SimpleStringProperty(param.getValue().getName());
			}
		});
		
		typeColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
			public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
				if(param.getValue()==null)
					return new SimpleStringProperty("");
				
				return new SimpleStringProperty(param.getValue().getType());
			}
		});
		
		primaryColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,Boolean>, ObservableValue<Boolean>>(){
			public ObservableValue<Boolean> call(CellDataFeatures<ColumnProperties, Boolean> param){
				if(param.getValue()==null)
					return new SimpleBooleanProperty(false);
				
				return new SimpleBooleanProperty(param.getValue().getPrimary());
			}
		});
			
		foreignColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
			public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
				if(param.getValue()==null)
					return new SimpleStringProperty("");
				
				return new SimpleStringProperty(param.getValue().getForeign());
			}
		});
		
		//ON EDIT COMMIT SETTINGS
		
		nameColumn.setOnEditCommit(t -> {
			t.getRowValue().setName(t.getNewValue());
		});
		
		typeColumn.setOnEditCommit(t -> {
			t.getRowValue().setType(t.getNewValue());
		});
		
		foreignColumn.setOnEditCommit(t -> {
			t.getRowValue().setForeign(t.getNewValue());
		});
		
		typeColumn.setMinWidth(100);
		primaryColumn.setMinWidth(100);
		foreignColumn.setMinWidth(200);
		
		table.getColumns().add(nameColumn);
		table.getColumns().add(typeColumn);
		table.getColumns().add(primaryColumn);
		table.getColumns().add(foreignColumn);
		
		return table;
	}
	
	public String getName(){
		return columnName.getValue();
	}
	
	public String getTable(){
		return tableName.getValue();
	}
	
	public String getFullNameValue(){
		
		if(fullName==null)
			fullName = new SimpleStringProperty(tableName.getValue() + "." 
					+ columnName.getValue());
		
		return fullName.getValue();
		
	}
	
	public String getType(){
		return columnType.getValue();
	}
	
	public boolean getPrimary(){
		return primaryKey.getValue();
	}
	
	public String getForeign(){
		return foreignKey.getValue();
	}
	
	public void setName(String name){
		columnName.setValue(name);
	}
	
	public void setTable(String name){
		tableName.setValue(name);
	}
	
	public void setType(String type){
		columnType.setValue(type);
	}
	
	public void setPrimary(boolean primary){
		primaryKey.setValue(primary);
	}
	
	public void setForeign(String foreign){
		foreignKey.setValue(foreign);
	}
	
	public void setFullName(String full){
		if(fullName==null)
			fullName = new SimpleStringProperty(full);
		else {
			fullName.setValue(full);
		}
	}
	
	public StringProperty getColumnName() {
		return columnName;
	}
	
	public StringProperty getTableName(){
		return tableName;
	}

	public StringProperty getColumnType() {
		return columnType;
	}

	public BooleanProperty getPrimaryKey() {
		return primaryKey;
	}

	public StringProperty getForeignKey() {
		return foreignKey;
	}
	
	public StringProperty getFullName(){
		return fullName;
	}
}