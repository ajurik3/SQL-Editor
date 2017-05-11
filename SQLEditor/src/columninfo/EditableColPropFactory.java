package columninfo;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class EditableColPropFactory implements 
	Callback<TableColumn<ColumnProperties, String>, TableCell<ColumnProperties, String>>{
	
	public EditableColPropCell call(TableColumn<ColumnProperties, String> param){
		return new EditableColPropCell();
	}
}
