package columninfo;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class EditableColPropCell extends TableCell<ColumnProperties, String> {

	TextField editInput = new TextField();
	TablePosition<ColumnProperties, String> tablePos;
	
	public EditableColPropCell(){
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
		
		final TableView<ColumnProperties> table = getTableView();
		tablePos = (TablePosition<ColumnProperties, String>) table.getEditingCell();
	}
	
	@Override public void cancelEdit(){
		super.cancelEdit();
		setText((String)getItem());
		setGraphic(null);
	}
	

	@Override public void commitEdit(String edit){
		
		setGraphic(null);

		setItem(edit);
		setText((String)getItem());
		
		final TableView<ColumnProperties> table = getTableView();
		
		if(table!= null&&tablePos!=null){
			
			CellEditEvent<ColumnProperties,String> commitEvent 
				= new CellEditEvent<ColumnProperties,String>(table, tablePos, 
						TableColumn.editCommitEvent(), edit);
		
			Event.fireEvent(getTableColumn(), commitEvent);
		}
		
	}
}