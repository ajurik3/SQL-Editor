package filters;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class QueryEvent<T> extends Event{

	private static final long serialVersionUID = 4610685314810190482L;
	@SuppressWarnings("rawtypes")
	public static final EventType<QueryEvent> QUERY 
		= new EventType<QueryEvent>(Event.ANY, "QUERY");
	
	private TableCell<Filter, T> cell;
	
	public QueryEvent(){
		super(QUERY);
	}
	
	
	public QueryEvent(TableCell<Filter, T> s, TableColumn<Filter, T> t){
		super(s, t, QUERY);
		cell = s;
	}
	
	public TableCell<Filter, T> getCell(){
		return cell;
	}
}
