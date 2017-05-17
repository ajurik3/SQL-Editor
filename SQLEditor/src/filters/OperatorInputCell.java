package filters;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;


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
