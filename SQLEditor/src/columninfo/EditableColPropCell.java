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

/*
 * This cell contains a string value which can be edited by double clicking on
 * the cell.  When editing, if the user presses enter and clicks on a screen
 * location which takes away focus from the edit input, an edit event is fired
 * to the cell's TableColumn, which must determine how to process the edit.
 */


public class EditableColPropCell extends TableCell<ColumnProperties, String> {

	//receives user input when editing
	TextField editInput = new TextField();
	
	//TablePosition where edit occurs, necessary for CellEditEvent
	TablePosition<ColumnProperties, String> tablePos;
	
	public EditableColPropCell(){
		super();
		
		//start edit and display editInput textfield if cell is
		//double clicked
		setOnMouseClicked(e -> {
			if(e.getClickCount()==2)
				startEdit();
		});
		
		setEditable(true);
		
		setOnKeyPressed(e ->{

			if(e.getCode().toString().equals("ENTER")){
				//submit edit if ENTER is pressed
				if(editInput!=null){
					if(editInput.getText()!=null){
						commitEdit(editInput.getText());
					}
				}
				e.consume();
			}
			else if (e.getCode().toString().equals("ESCAPE")){
				//cancel edit if ESC is pressed
				cancelEdit();
				e.consume();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	@Override public void startEdit(){
		if(!isEditable()||!getTableView().isEditable()||!getTableColumn().isEditable())
			return;
		super.startEdit();
		
		//replace text with dynamically created textfield and give it focus
		createTextField();
		setText("");
		setGraphic(editInput);
		editInput.requestFocus();
		
		//save TablePosition of this edit
		final TableView<ColumnProperties> table = getTableView();
		tablePos = (TablePosition<ColumnProperties, String>) table.getEditingCell();
	}
	
	//this function dynamically creates a new textfield to ensure that it does not
	//contain side effects from when the cell displays other data in the model
	void createTextField(){
		editInput = new TextField((String)getItem());
	
		//commit edit when the textfield loses focus
		editInput.focusedProperty().addListener(new InvalidationListener(){
			@Override public void invalidated(Observable arg0){

					if(!((ReadOnlyBooleanProperty)arg0).getValue())
						commitEdit(editInput.getText());
				}
		});
	}
	
	
	
	@Override public void cancelEdit(){
		super.cancelEdit();
		setText((String)getItem());
		setGraphic(null);
	}
	
	//this function sets the text to a new item and removes any
	//textfield which may be visible, which is necessary if the
	//cell is being used to display a different item from the data model
	@Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        
        if(empty){
        	setText(null);
        	setGraphic(null);
        }
        else{
        	setText(getItem()==null ? "" : getItem().toString());
        	setGraphic(null);
        }
	}

	//update cell and fire CellEditEvent to TableColumn
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