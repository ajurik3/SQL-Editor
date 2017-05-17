package columninfo;

import java.util.Comparator;
import javafx.util.Pair;

public class MaxTableSizeComparator implements Comparator<SourceTable> {

	@Override
	public int compare(SourceTable t1, SourceTable t2) {

			return t2.getRows() - t1.getRows();
	}
}