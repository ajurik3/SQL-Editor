package columninfo;

public class SourceTable {
	
	String name;
	int rows;
	String cols;
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
	
	public void appendCols(String c){
		cols = c + cols;
	}
	
}
