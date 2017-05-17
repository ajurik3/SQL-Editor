package editor;

import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/*
 * This class's function returns a default EditableTableCell when it's
 * called to create a cell for its TableColumn.
 */

public class EditableCellFactory implements 
	Callback<TableColumn<ObservableList<String>, String>, TableCell<ObservableList<String>, String>>{
	
	public EditableTableCell call(TableColumn<ObservableList<String>, String> param){
		return new EditableTableCell();
	}
}