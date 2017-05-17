package columninfo;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/*
 * This class's function returns a default EditableColPropCell when it's
 * called to create a cell for its TableColumn.
 */

public class EditableColPropFactory implements 
	Callback<TableColumn<ColumnProperties, String>, TableCell<ColumnProperties, String>>{
	
	public EditableColPropCell call(TableColumn<ColumnProperties, String> param){
		return new EditableColPropCell();
	}
}
