package editor;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class EditableTableCell extends TableCell<ObservableList<String>, String> {

	TextField editInput = new TextField();
	TablePosition<ObservableList<String>, String> tablePos;
	
	public EditableTableCell(){
		super();
		
		setOnMouseClicked(e -> {
			if(e.getClickCount()==2)
				startEdit();
		});
		
		setEditable(true);
		
		setOnKeyPressed(e ->{

			if(e.getCode().toString().equals("ENTER")){
				if(editInput!=null){
					if(editInput.getText()!=null){
						commitEdit(editInput.getText());
					}
				}
				e.consume();
			}
			else if (e.getCode().toString().equals("ESCAPE")){
				cancelEdit();
				e.consume();
			}
		});
	}
	
	void createTextField(){
		editInput = new TextField((String)getItem());
	
		editInput.focusedProperty().addListener(new InvalidationListener(){
			@Override public void invalidated(Observable arg0){

					if(!((ReadOnlyBooleanProperty)arg0).getValue())
						commitEdit(editInput.getText());
				}
		});
	}
	
	@Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        
        if(empty){
        	setText(null);
        	setGraphic(null);
        }
        else{
        	if(isEditing()){
        		if(editInput!=null){
        		 editInput.setText(getItem()==null ? "" : getItem().toString());
        		}
        		 setText("");
        		 setGraphic(editInput);
        	}
        	else{
        		setText(getItem()==null ? "" : getItem().toString());
        		setGraphic(null);
        	}
        }
	}
	
	@Override public void startEdit(){
		if(!isEditable()||!getTableView().isEditable()||!getTableColumn().isEditable())
			return;
		
		super.startEdit();
		createTextField();
		setText("");
		setGraphic(editInput);
		editInput.requestFocus();
		
		final TableView<ObservableList<String>> table = getTableView();
		tablePos = (TablePosition<ObservableList<String>, String>) table.getEditingCell();
	}
	
	@Override public void cancelEdit(){
		super.cancelEdit();
		setText((String)getItem());
		setGraphic(null);
	}
	

	@Override public void commitEdit(String edit){
		
		setGraphic(null);

		int rowIndex = getTableRow().getIndex();
		int columnIndex = getTableView().getColumns().indexOf(getTableColumn());
		getTableView().getItems().get(rowIndex).set(columnIndex, edit);
		setItem(edit);
		setText((String)getItem());
		
		final TableView<ObservableList<String>> table = getTableView();
		
		if(table!= null&&tablePos!=null){
			
			CellEditEvent<ObservableList<String>,String> commitEvent 
			= new CellEditEvent<ObservableList<String>,String>(table, tablePos, 
					TableColumn.editCommitEvent(), edit);
		
		Event.fireEvent(getTableColumn(), commitEvent);
		}
		
	}
}