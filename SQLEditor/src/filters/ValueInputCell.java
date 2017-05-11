package filters;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

public class ValueInputCell extends TableCell<Filter, TextField>{
	
	public ValueInputCell(){
		super();
	}
	
	@Override public void updateItem(TextField item, boolean empty){
		super.updateItem(item, empty);

	     if (empty || item == null) {
	         setText(null);
	         setGraphic(null);
	     } 
	     else{
	    	 
	    	 item.textProperty().addListener(new ChangeListener<String>(){
	    		 public void changed(ObservableValue arg0, String oldValue, String newValue){
	    			 sendQueryEvent();
	    		 }
	    	 });
	    	
	         setGraphic(item);
	     }
	 }
	
	private void sendQueryEvent(){
		if(getItem()!=null){
			QueryEvent<TextField> event = new QueryEvent<TextField>(this, getTableColumn());
			Event.fireEvent(event.getTarget(), event);
		}
	}
}