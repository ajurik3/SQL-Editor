package columninfo;

/*
 * This class contains fields holding information about a selection
 * from a source table which will be added to a new table using
 * TableBuilder.
 */

public class SourceTable {
	
	//MySQL name of the source table
	String name;
	
	//number of rows defined in the columns selected from the table
	int rows;
	
	//name of columns selected from table, separated
	//by commas to be inserted into select statement
	String cols;
	
	//the name of the temporary table created for these columns
	//before they are joined into the final table
	String tempTable;
	
	public SourceTable(){
		cols = "";
	}
	
	public SourceTable(String n){
		name = n;
		cols = "";
	}
	
	public SourceTable(String n, String t){
		name = n;
		tempTable = t;
		cols = "";
	}
	
	public SourceTable(String n, String t, String c){
		name = n;
		tempTable = t;
		cols = c + ", ";
	}
	
	public String getName(){
		return name;
	}
	
	public int getRows(){
		return rows;
	}
	
	public String getTemp(){
		return tempTable;
	}
	
	//returns the column string without the comma appended to the final
	//column name for easy insertion into select statements
	public String selectableCols(){
		return cols.substring(0, cols.length()-2);
	}
	
	public String cols(){
		return cols;
	}
	
	public void setName(String n){
		name = n;
	}
	
	public void setRows(int r){
		rows = r;
	}
	
	public void setTemp(String t){
		tempTable = t;
	}
	
	public void appendCol(String c){
		cols += c + ", ";
	}
	
	//c must contain a sequence of column names separated by
	//commas with a comma following the last name
	public void appendCols(String c){
		cols = c + cols;
	}
}
