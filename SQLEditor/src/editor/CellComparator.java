package editor;

import java.util.Comparator;

public class CellComparator implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		try{
			double d1 = Double.parseDouble(s1.replace("%", "").replace("$", ""));
			double d2 = Double.parseDouble(s2.replace("%", "").replace("$", ""));
			if(d1==d2)
				return 0;
			else
				return (d1-d2 < 0) ? -1 : 1;
		}
		catch(NumberFormatException ex){
			return s1.compareTo(s2);
		}
	}

}
