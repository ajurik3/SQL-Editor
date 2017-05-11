package columninfo;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

public class BoolCheckBoxCell extends TableCell<ColumnProperties, Boolean> {

		CheckBox checked = new CheckBox();
		
		public BoolCheckBoxCell(){
		
			setGraphic(checked);
			setText(null);
			checked.selectedProperty().addListener(new ChangeListener<Boolean>(){
				public void changed(ObservableValue obv, Boolean oldValue, Boolean newValue){
					setItem(newValue);
					
					int rowIndex = getTableRow().getIndex();
					getTableView().getItems().get(rowIndex).setPrimary(newValue);
				}
			});
		}
		
		@Override
	    public void updateItem(Boolean item, boolean empty) {
	        super.updateItem(item, empty);
	        
	        if(empty||item==null){
	        	setText(null);
	        	setGraphic(null);
	        }
	        else{
	        	setGraphic(checked);
	        	
	        	checked.setSelected(item);
	        }
		}
}
