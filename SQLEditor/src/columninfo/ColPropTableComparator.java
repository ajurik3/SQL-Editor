package columninfo;

import java.util.Comparator;

public class ColPropTableComparator implements Comparator<ColumnProperties> {

	@Override
	public int compare(ColumnProperties c1, ColumnProperties c2) {

			return c1.getTable().compareTo(c2.getTable());
	}
}
