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

/*
 * This cell contains a string value which can be edited by double clicking on
 * the cell.  When editing, if the user presses enter and clicks on a screen
 * location which takes away focus from the edit input, the cell and data model 
 * are updated and an edit event is fired to the cell's TableColumn.
 */

public class EditableTableCell extends TableCell<ObservableList<String>, String> {

	//receives user input when editing
	TextField editInput = new TextField();
	
	//TablePosition where edit occurs, necessary for CellEditEvent
	TablePosition<ObservableList<String>, String> tablePos;
	
	public EditableTableCell(){
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
		
		//replace text with dynamically created textfield and give it focus
		super.startEdit();
		createTextField();
		setText("");
		setGraphic(editInput);
		editInput.requestFocus();
		
		//save TablePosition of this edit
		final TableView<ObservableList<String>> table = getTableView();
		tablePos = (TablePosition<ObservableList<String>, String>) table.getEditingCell();
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
        	setText(item==null ? "" : item);
        	setGraphic(null);
        }
	}
	
	@Override public void cancelEdit(){
		super.cancelEdit();
		setText((String)getItem());
		setGraphic(null);
	}
	
	//this function updates the data model and fires a CellEditEvent
	//to the cell's TableColumn
	@Override public void commitEdit(String edit){
		
		setGraphic(null);

		int rowIndex = getTableRow().getIndex();
		int columnIndex = tablePos.getColumn();
		
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