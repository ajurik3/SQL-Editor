package columninfo;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

/*
 * This cell's item represents the primaryKey property of a ColumnProperty.
 * The item's value is represented by a CheckBox, which is checked when
 * the column contains a primary key.  Clicking the CheckBox changes the
 * value of the corresponding ColumnProperty objects primaryKey value. 
 */


public class BoolCheckBoxCell extends TableCell<ColumnProperties, Boolean> {

		CheckBox checked = new CheckBox();
		
		
		public BoolCheckBoxCell(){
			
			//set the value of ColumnProperties object's primaryKey to
			//reflect changes in whether CheckBox is checked
			checked.selectedProperty().addListener(new ChangeListener<Boolean>(){
				@SuppressWarnings("rawtypes")
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
	        	//display nothing if the item is empty or undefined
	        	setText(null);
	        	setGraphic(null);
	        }
	        else{
	        	//display a CheckBox representing the value of the item
	        	setGraphic(checked);    	
	        	checked.setSelected(item);
	        }
		}
}
