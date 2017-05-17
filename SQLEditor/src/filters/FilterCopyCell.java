package filters;

import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

/*
 * This cell contains a button which sends a QueryEvent to its column
 * when fired, which the column's event handler can use to create a copy
 * of this cell's filter.
 */


public class FilterCopyCell extends TableCell<Filter, Button>{
	
	public FilterCopyCell(){
		super();
	}
	
	@Override public void updateItem(Button item, boolean empty){
		super.updateItem(item, empty);

	     if (empty || item == null) {
	         setText(null);
	         setGraphic(null);
	     } 
	     else{
	    	 //set button to send QueryEvent when clicked
	    	 item.setOnAction(e -> sendQueryEvent());	    	
	         setGraphic(item);
	     }
	 }
	
	private void sendQueryEvent(){
		if(getItem()!=null){
			QueryEvent<Button> event = new QueryEvent<Button>(this, getTableColumn());
			Event.fireEvent(event.getTarget(), event);
		}
	}
}
