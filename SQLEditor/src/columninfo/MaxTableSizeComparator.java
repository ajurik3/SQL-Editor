package columninfo;

import java.util.Comparator;

/*
	This class's function compares two SourceTable instances and indicates
	the one with more defined rows as less than the other.
	
	A negative result indicates t1 < t2, or that t1 has more rows.
	0 indicates t1==t2
	A positive result indicates t1 > t2
*/
public class MaxTableSizeComparator implements Comparator<SourceTable> {

	@Override
	public int compare(SourceTable t1, SourceTable t2) {
			return t2.getRows() - t1.getRows();
	}
}