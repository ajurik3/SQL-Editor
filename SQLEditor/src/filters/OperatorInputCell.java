package filters;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;

/*
 * This cell contains a ComboBox of strings.  When a string is clicked, the
 * cell sends a QueryEvent to its column, which may cause the column's event
 * handler to execute a query.
 */


public class OperatorInputCell extends TableCell<Filter, ComboBox<String>>{
		
		public OperatorInputCell(){
			super();
		}
		
		@Override public void updateItem(ComboBox<String> item, boolean empty){
			super.updateItem(item, empty);

		     if (empty || item == null) {
		         setText(null);
		         setGraphic(null);
		     } 
		     else{
		    	 //fire a QueryEvent whenever a different operator is clicked
		    	 item.valueProperty().addListener(new ChangeListener<String>(){
		    		@SuppressWarnings("rawtypes")
					@Override 
					public void changed(ObservableValue arg0, String oldValue, String newValue ){
		    			if(newValue!=null)
							sendQueryEvent();
					}
				});
		    	
		         setGraphic(item);
		     }
		 }
		
		private void sendQueryEvent(){
			if(getItem()!=null){
				QueryEvent<ComboBox<String>> event 
					= new QueryEvent<ComboBox<String>>(this, getTableColumn());
				Event.fireEvent(event.getTarget(), event);
			}
		}
}
