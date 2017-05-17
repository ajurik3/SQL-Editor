package editor;

import java.util.Comparator;

/*
 * This cell's function attempts to compare its arguments numerically, and
 * if the arguments are not formatted as a double, the function compares
 * them lexographically.  The function attempts to sort percentages and monetary
 * values numerically.
 */


public class CellComparator implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		try{
			//remove % and $ before attempting conversion so that 
			//percentages and monetary values are sorted numerically
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
