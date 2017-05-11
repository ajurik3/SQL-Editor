package filters;

import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

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
