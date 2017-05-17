package columninfo;

import java.util.Comparator;

/*
	This class's function compares ColumnProperties based on the lexographical
	ordering of their table field.
	
	A negative result indicates c1 < c2
	0 indicates c1 == c2
	A positive result indicates c1 > c2
*/
public class ColPropTableComparator implements Comparator<ColumnProperties> {

	@Override
	public int compare(ColumnProperties c1, ColumnProperties c2) {

			return c1.getTable().compareTo(c2.getTable());
	}
}
