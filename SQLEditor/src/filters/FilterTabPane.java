package filters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import editor.SQLEditor;
import editor.SessionConfig;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
/*
 * This class has a tab where a user can define filters (expressions in
 * a WHERE clause) and a tab where a user can define search phrases for
 * each column.  Matches for search phrases occur only if the phrase
 * appears at the beginning of the data.
 */
public class FilterTabPane extends TabPane{

	//filters to apply to the primary TableView's data
	private TableView<Filter> filters = new TableView<Filter>();
	
	//phrases to search the primary TableView's data for
	private TableView<Filter> searches = new TableView<Filter>();
	
	public FilterTabPane(){
		super();
		
		initFilters();
		filters.setPlaceholder(new Label("Table Filters"));
		
		initSearches();
		searches.setPlaceholder(new Label("Table Searches"));
		
		Tab filterTab = new Tab("Filters");
		filterTab.setClosable(false);
		filterTab.setContent(filters);
		
		Tab searchTab = new Tab("Search");
		searchTab.setClosable(false);
		searchTab.setContent(searches);
		
		getTabs().addAll(filterTab, searchTab);
	}
	
	/*
	 * This function defines TableColumns for a column name and for 
	 * a TextField for the user to enter a search phrase for the column.
	 * When the TextFields in the search phrase column are edited
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSearches(){
		TableColumn<Filter, String> columnName = new TableColumn<Filter, String>("Column");
		columnName.setCellValueFactory(new PropertyValueFactory<>("column"));
		
		TableColumn<Filter, TextField> searchTerm = new TableColumn<Filter, TextField>("Search");
		searchTerm.setCellFactory(new Callback<TableColumn<Filter,TextField>,TableCell<Filter,TextField>>(){
			public TableCell<Filter,TextField> call(TableColumn<Filter,TextField> param){
				return new ValueInputCell();
			}
		});
		searchTerm.setCellValueFactory(new PropertyValueFactory<>("operand"));
		searchTerm.addEventHandler(QueryEvent.QUERY, new EventHandler<QueryEvent>(){
			@Override
			public void handle(QueryEvent e){
				executeFilterQuery();
			}
		});
		searchTerm.setMinWidth(200);
		
		searches.getColumns().addAll(columnName, searchTerm);
	}
	
	/*
	 * This function sets TableColumn names and factories for each of these 
	 * Filter properties: column, operation, operand, and copy.  For all 
	 * properties but column, a QueryEvent handler is also defined.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void initFilters(){
		TableColumn<Filter, String> columnName = new TableColumn<Filter, String>("Column");
		columnName.setCellValueFactory(new PropertyValueFactory<>("column"));
		
		TableColumn<Filter, ComboBox<String>> operatorColumn 
			= new TableColumn<Filter, ComboBox<String>>("Operation");
		operatorColumn.setCellFactory(new Callback<TableColumn<Filter,ComboBox<String>>,TableCell<Filter,ComboBox<String>>>(){
			public OperatorInputCell call(TableColumn<Filter,ComboBox<String>> param){
				return new OperatorInputCell();
			}
		});
		operatorColumn.setCellValueFactory(new PropertyValueFactory<>("operation"));
		operatorColumn.addEventHandler(QueryEvent.QUERY, new EventHandler<QueryEvent>(){
			@Override
			public void handle(QueryEvent e){
				executeFilterQuery();
			}
		});
		operatorColumn.setMinWidth(75);
		
		TableColumn<Filter, TextField> operandColumn = new TableColumn<Filter, TextField>("Value");
		operandColumn.setCellFactory(new Callback<TableColumn<Filter,TextField>,TableCell<Filter,TextField>>(){
			public TableCell<Filter,TextField> call(TableColumn<Filter,TextField> param){
				return new ValueInputCell();
			}
		});
		operandColumn.setCellValueFactory(new PropertyValueFactory<>("operand"));
		operandColumn.addEventHandler(QueryEvent.QUERY, new EventHandler<QueryEvent>(){
			@Override
			public void handle(QueryEvent e){
				executeFilterQuery();
			}
		});
		
		TableColumn<Filter, Button> copyColumn = new TableColumn<Filter, Button>("Add");
		copyColumn.setCellFactory(new Callback<TableColumn<Filter,Button>,TableCell<Filter,Button>>(){
			public TableCell<Filter,Button> call(TableColumn<Filter,Button> param){
				return new FilterCopyCell();
			}
		});
		copyColumn.setCellValueFactory(new PropertyValueFactory<>("copy"));
		copyColumn.setMinWidth(80);
		
		//
		copyColumn.addEventHandler(QueryEvent.QUERY, new EventHandler<QueryEvent>(){
			@Override
			public void handle(QueryEvent e){
				Filter f = filters.getItems().get(e.getCell().getIndex());
				filters.getItems().add(e.getCell().getIndex()+1, new Filter(f.getColumn()));
			}
		});
		filters.getColumns().addAll(columnName, operatorColumn, operandColumn, copyColumn);
	}
	
	/*
	 * This function searches the filter arrays for valid filters, executes a
	 * query with the filters applied, and passes the result to the primary
	 * TableView.
	 */
	private void executeFilterQuery(){
		
		SessionConfig session = SQLEditor.getSession();
		
		try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			connection.setCatalog(session.getDatabase());
			
			ArrayList<Filter> validFilters = findValidFilters(connection);
			ArrayList<Filter> validSearches = new ArrayList<Filter>();
			
			for(Filter f : searches.getItems()){
				if(!f.getOperand().getText().isEmpty())
					validSearches.add(f);
			}
			PreparedStatement filterQuery = getQuery(connection, 
					validFilters, validSearches);
			
			ResultSet queryResult = filterQuery.executeQuery();
			SQLEditor.getTableView().updateTable(queryResult);;
			
			filterQuery.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	/*
	 * This function returns an array of Filter objects where both the 
	 * operation and operand properties are defined. If the column being
	 * filtered is a numeric column, the Filter's operand must also be
	 * able to be parsed as a double.
	 */
	private ArrayList<Filter> findValidFilters(Connection connection) 
			throws SQLException{
		
		ArrayList<Filter> validFilters = new ArrayList<Filter>();
		
		for(Filter f : filters.getItems()){
			if((f.getOperation().getValue()!=null)&&
					(!f.getOperand().getText().isEmpty())){
				
				//query for column type of current column
				String queryString = "SELECT COLUMN_TYPE FROM "
						+ "information_schema.COLUMNS WHERE TABLE_SCHEMA ? "
						+ "AND TABLE_NAME = ? AND COLUMN_NAME = ?;";
				
				PreparedStatement statement = connection.prepareStatement(queryString);
				statement.setString(1, connection.getCatalog());
				statement.setString(2, SQLEditor.getTableName());
				statement.setString(3, f.getColumn());
				
				ResultSet columnType = statement.executeQuery(queryString);
				
				//if column type is numeric, attempt to parse as a double
				//before adding to validFilters
				if(columnType.next()){
					String fType = columnType.getString(1);
					if (isNumeric(fType)){
						try{
							Double.parseDouble(f.getOperand().getText());
							validFilters.add(f);
						}
						catch(Exception ex){
							ex.printStackTrace();
						}
					}
					else{
						validFilters.add(f);
					}
				}
				
				statement.close();
				
			}
		}
		
		return validFilters;
	}
	
	/*
	 * This function creates a MySQL query with a WHERE clause containing
	 * all valid filters if any exist and a LIKE clause for each valid
	 * column search.
	 */
	private PreparedStatement getQuery(Connection connection, 
			ArrayList<Filter> validFilters,	ArrayList<Filter> validSearches)
					throws SQLException
	{
		
		String queryString = new String("SELECT * FROM " + SQLEditor.getTableName());
		
		if((!validFilters.isEmpty())||(!validSearches.isEmpty())){
		
			//begin WHERE clause if any valid filters exist
			if(!validFilters.isEmpty())
				queryString += " WHERE ";
			
			//add all valid filters
			for(Filter f : validFilters){
				queryString += f.getColumn() + " " + f.getOperation().getValue() + " ? AND ";
			}
			
			//add LIKE clause for all valid searches
			for(Filter f : validSearches){
				queryString += f.getColumn() + " LIKE ? AND ";
			}
		
			queryString = queryString.substring(0, queryString.length()-5) + ";";
		}
		
		PreparedStatement filterQuery = connection.prepareStatement(queryString);
		
		//set PreparedStatement parameters
		if((!validFilters.isEmpty())||(!validSearches.isEmpty())){
			for(int i = 0; i < validFilters.size(); i++){
				filterQuery.setString(i+1, validFilters.get(i).getOperand().getText());
			}

			for(int i = 0; i < validSearches.size(); i++){

					filterQuery.setString(i + 1 + validFilters.size(), 
							validSearches.get(i).getOperand().getText() + "%");
			}
		}
		
		return filterQuery;
	}
	
	public void clearAll(){
		filters.getItems().clear();
		searches.getItems().clear();
	}
	
	public void setAll(ObservableList<Filter> filterList, 
			ObservableList<Filter> searchList){
		filters.setItems(filterList);
		searches.setItems(searchList);
	}
	
	//returns true if the string represents a common MySQL numeric type
	private static boolean isNumeric(String type){
		return type.contains("int")||type.contains("float")
				||type.contains("double")||type.contains("DECIMAL");
	}
}
