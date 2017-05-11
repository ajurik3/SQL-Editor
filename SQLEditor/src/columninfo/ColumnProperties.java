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
	StringProperty tableName;
	StringProperty columnType;
	BooleanProperty primaryKey;
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
	}
	
	public ColumnProperties(String name, String type, boolean primary, String foreign){
		columnName = new SimpleStringProperty(name);
		tableName = new SimpleStringProperty();
		columnType = new SimpleStringProperty(type);
		primaryKey = new SimpleBooleanProperty(primary);
		foreignKey = new SimpleStringProperty(foreign);
	}
	
	@SuppressWarnings("rawtypes")
	public static TableView<ColumnProperties> getEditableColumns(){
		TableView<ColumnProperties> table = new TableView<ColumnProperties>();
		table.setEditable(true);
		table.setPlaceholder(new Label("No columns imported"));
		
		TableColumn<ColumnProperties, String> nameColumn = 
				new TableColumn<ColumnProperties, String>("Column");
		
		nameColumn.setEditable(true);
		
		nameColumn.setCellFactory(new EditableColPropFactory());
		
		nameColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
			public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
				if(param.getValue()==null)
					return new SimpleStringProperty("");
				
				return new SimpleStringProperty(param.getValue().getName());
			}
		});
		
		nameColumn.setOnEditCommit(t -> {
			t.getRowValue().setName(t.getNewValue());
		});
		
		TableColumn<ColumnProperties, String> typeColumn = 
				new TableColumn<ColumnProperties, String>("Column Type");
		
		typeColumn.setCellFactory(new EditableColPropFactory());
		
		typeColumn.setEditable(true);
		typeColumn.setMinWidth(100);
		
		typeColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
			public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
				if(param.getValue()==null)
					return new SimpleStringProperty("");
				
				return new SimpleStringProperty(param.getValue().getType());
			}
		});
		
		typeColumn.setOnEditCommit(t -> {
			t.getRowValue().setType(t.getNewValue());
		});
		
		TableColumn<ColumnProperties, Boolean> primaryColumn =
				new TableColumn<>("Primary Key");
		
		primaryColumn.setCellFactory(new Callback<TableColumn<ColumnProperties, Boolean>, 
				TableCell<ColumnProperties, Boolean>>(){
			public BoolCheckBoxCell call(TableColumn<ColumnProperties, Boolean> col){

				return new BoolCheckBoxCell();
			}
		});
		
		primaryColumn.setMinWidth(100);
		
		primaryColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,Boolean>, ObservableValue<Boolean>>(){
			public ObservableValue<Boolean> call(CellDataFeatures<ColumnProperties, Boolean> param){
				if(param.getValue()==null)
					return new SimpleBooleanProperty(false);
				
				return new SimpleBooleanProperty(param.getValue().getPrimary());
			}
		});
		
		TableColumn<ColumnProperties, String> foreignColumn =
				new TableColumn<>("Foreign Key");
		
		foreignColumn.setCellFactory(new EditableColPropFactory());
			
		foreignColumn.setCellValueFactory(new Callback<CellDataFeatures<ColumnProperties,String>, ObservableValue<String>>(){
			public ObservableValue<String> call(CellDataFeatures<ColumnProperties, String> param){
				if(param.getValue()==null)
					return new SimpleStringProperty("");
				
				return new SimpleStringProperty(param.getValue().getForeign());
			}
		});
		
		foreignColumn.setOnEditCommit(t -> {
			t.getRowValue().setForeign(t.getNewValue());
		});
		
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
	
	public String getFullName(){
		return tableName.getValue() + "." + columnName.getValue();
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
}