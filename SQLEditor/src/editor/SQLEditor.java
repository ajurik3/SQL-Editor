package editor;

import java.sql.*;
import java.util.ArrayList;
import filters.*;

import javafx.stage.*;
import javafx.geometry.*;
import javafx.application.Application;
import javafx.util.Callback;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;

public class SQLEditor extends Application{

	private Stage stage;
	private MenuBar mainMenu;
	private BorderPane border = new BorderPane();
	
	private static ListView<String> tables = new ListView<String>();
	private static TableView<ObservableList<String>> tableview = new TableView<ObservableList<String>>();
	private static String tableName;
	
	private static TableView<Filter> filters = new TableView<Filter>();
	private static TableView<Filter> searches = new TableView<Filter>();
	
	private static SessionConfig session;
	
	public void start(Stage primaryStage) throws Exception{
		
		stage = primaryStage;
	
		initMenuBar();
		HBox connectionBar = getConnectionBar();
		VBox configBars = new VBox(5, mainMenu, connectionBar);
		
		border.setTop(configBars);
		
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		Scene scene = new Scene(border);
		stage.setScene(scene);
		stage.setTitle("SQL Editor");
		stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
		stage.show();		
		
		tables.setPlaceholder(new Label("Database Tables"));
		tables.setMinWidth(border.getWidth()/5.7);
		tables.setMaxHeight(border.getHeight()/3);
		
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
		
		TabPane tabs = new TabPane(filterTab, searchTab);
		
		VBox leftPane = new VBox(5, tables, tabs);
		
		border.setLeft(leftPane);
		
		tableview.setPlaceholder(new Label("Current Table"));
		tableview.setMinWidth(border.getWidth()*6/7);
		tableview.setEditable(true);
		border.setCenter(tableview);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void initSearches(){
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
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void initFilters(){
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
		copyColumn.addEventHandler(QueryEvent.QUERY, new EventHandler<QueryEvent>(){
			@Override
			public void handle(QueryEvent e){
				Filter f = filters.getItems().get(e.getCell().getIndex());
				filters.getItems().add(e.getCell().getIndex()+1, new Filter(f.getColumn()));
			}
		});
		filters.getColumns().addAll(columnName, operatorColumn, operandColumn, copyColumn);
	}
	
	private static void executeFilterQuery(){
		
		ArrayList<Filter> validFilters = new ArrayList<Filter>();
		ArrayList<Filter> validSearches = new ArrayList<Filter>();
		
		try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			Statement statement = connection.createStatement();
			connection.setCatalog(session.getDatabase());
			
			for(Filter f : filters.getItems()){
				if((f.getOperation().getValue()!=null)&&
						(!f.getOperand().getText().equals(""))){
					
					String queryString = "SELECT COLUMN_TYPE FROM "
							+ "information_schema.COLUMNS WHERE TABLE_SCHEMA = \'" 
							+ connection.getCatalog() + "\' AND TABLE_NAME = \'" 
							+ tableName + "\' AND COLUMN_NAME = \'" + f.getColumn() + "\';";
					
					ResultSet columnType = statement.executeQuery(queryString);
					
					if(columnType.next()){
						String fType = columnType.getString(1);
						if (isNumeric(fType)){
							try{
								Double.parseDouble(f.getOperand().getText());
								validFilters.add(f);
							}
							catch(Exception ex){
								f.getOperand().setText("");
								ex.printStackTrace();
								return;
							}
						}
						else{
							validFilters.add(f);
						}
					}
					
				}
			}
			statement.close();
			
			for(Filter f : searches.getItems()){
				if(!f.getOperand().getText().equals(""))
					validSearches.add(f);
			}
			
			String queryString = new String("SELECT * FROM " + tableName);
			
			if((!validFilters.isEmpty())||(!validSearches.isEmpty())){
			
				queryString += " WHERE ";
				
				for(Filter f : validFilters){
					queryString += f.getColumn() + " " + f.getOperation().getValue() + " ? AND ";
				}
				
				for(Filter f : validSearches){
					queryString += f.getColumn() + " LIKE ? AND ";
				}
			
				queryString = queryString.substring(0, queryString.length()-5) + ";";
			}
			
			PreparedStatement filterQuery = connection.prepareStatement(queryString);
			
			if((!validFilters.isEmpty())||(!validSearches.isEmpty())){
				for(int i = 0; i < validFilters.size(); i++){
					filterQuery.setString(i+1, validFilters.get(i).getOperand().getText());
				}

				
				for(int i = 0; i < validSearches.size(); i++){

						filterQuery.setString(i + 1 + validFilters.size(), 
								validSearches.get(i).getOperand().getText() + "%");
				}
			}
			else
				queryString += ";";
			
			ResultSet queryResult = filterQuery.executeQuery();
			ResultSetMetaData tableInfo = queryResult.getMetaData();

			tableview.getItems().clear();
			ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
			
			while(queryResult.next()){
				ObservableList<String> row = FXCollections.observableArrayList();
				for(int i = 1; i <= tableInfo.getColumnCount(); i++){
					row.add(queryResult.getString(i));
				}
				data.add(row);
			}
			
			System.out.println(queryString);
			
			tableview.setItems(data);
			
			filterQuery.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	private static boolean isNumeric(String type){
		return type.contains("int")||type.contains("float")
				||type.contains("double")||type.contains("DECIMAL");
	}
	
	public static void initTable(){
		tableview.getColumns().clear();
		filters.getItems().clear();
		searches.getItems().clear();
		
		if(tableName==null){
			return;
		}
		
		ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
		ObservableList<Filter> filterList = FXCollections.observableArrayList();
		ObservableList<Filter> searchList = FXCollections.observableArrayList();
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			
			connection.setCatalog(session.getDatabase());
			
			Statement statement = connection.createStatement();
			
			ResultSet tableData = statement.executeQuery("select * from " + tableName);
			ResultSetMetaData tableInfo = tableData.getMetaData();
			
			for(int i = 0; i < tableInfo.getColumnCount(); i++){
				final int j = i;
				TableColumn<ObservableList<String>, String> col = 
						new TableColumn<ObservableList<String>, String>(tableInfo.getColumnName(i+1));
				
				filterList.add(new Filter(tableInfo.getColumnName(i+1)));
				searchList.add(new Filter(tableInfo.getColumnName(i+1)));
				
				col.setComparator(new CellComparator());
				col.setEditable(true);
				col.setCellFactory(new Callback<TableColumn<ObservableList<String>, String>, 
						TableCell<ObservableList<String>, String>>(){
					public EditableTableCell call (TableColumn<ObservableList<String>, String> colParam){
						EditableTableCell cell = new EditableTableCell();
						
						cell.setOnContextMenuRequested(e ->{
							final ContextMenu cellMenu = getCellContextMenu((EditableTableCell)e.getSource());
							cell.setContextMenu(cellMenu);
							
							cellMenu.show((Node)e.getSource(), e.getScreenX(), e.getScreenY());
							
						});
						
						return cell;
					}
				});
				
				col.addEventHandler(TableColumn.editCommitEvent(), new EventHandler<CellEditEvent<ObservableList<String>,String>>(){
					@Override public void handle(CellEditEvent<ObservableList<String>,String> t){
						/*System.out.println("Value: " + t.getNewValue());
						System.out.println("ROW: " + t.getTablePosition().getRow());
						System.out.println("COLUMN: " + t.getTablePosition().getColumn());*/
					}
				});
				
				col.setCellValueFactory( new Callback<CellDataFeatures<ObservableList<String>, String>, ObservableValue<String>>()
				{
					public ObservableValue<String> call(CellDataFeatures<ObservableList<String>, String> param){
						if(param.getValue().get(j)==null)
							return new SimpleStringProperty("");

						return new SimpleStringProperty(param.getValue().get(j).toString());
					}
				});
				col.setContextMenu(getColumnContextMenu(col));
				
				tableview.getColumns().add(col);
			}
			
			while(tableData.next()){
				ObservableList<String> row = FXCollections.observableArrayList();
				for(int i = 1; i <= tableInfo.getColumnCount(); i++){
					row.add(tableData.getString(i));
				}
				data.add(row);
			}
			
			tableview.setItems(data);
			filters.setItems(filterList);
			searches.setItems(searchList);
			
			connection.close();
			statement.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	public static void updateTable(){
		ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
		tableview.getColumns().clear();
		filters.getItems().clear();
		searches.getItems().clear();
		
		ObservableList<Filter> filterList = FXCollections.observableArrayList();
		ObservableList<Filter> searchList = FXCollections.observableArrayList();
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			
			connection.setCatalog(session.getDatabase());
			
			Statement statement = connection.createStatement();
			
			ResultSet tableData = statement.executeQuery("select * from " + tableName);
			ResultSetMetaData tableInfo = tableData.getMetaData();
			
			for(int i = 0; i < tableInfo.getColumnCount(); i++){
				filterList.add(new Filter(tableInfo.getColumnName(i+1)));
				searchList.add(new Filter(tableInfo.getColumnName(i+1)));
			}
			
			while(tableData.next()){
				ObservableList<String> row = FXCollections.observableArrayList();
				for(int i = 1; i <= tableInfo.getColumnCount(); i++){
					row.add(tableData.getString(i));
				}
				data.add(row);
			}
			
			tableview.setItems(data);
			filters.setItems(filterList);
			searches.setItems(searchList);
			
			connection.close();
			statement.close();
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	public static ContextMenu getCellContextMenu(EditableTableCell cell){
		ContextMenu cellMenu = new ContextMenu();
		
		TableView<ObservableList<String>> currentTable = cell.getTableView(); 
		
		MenuItem update = new MenuItem("Update");
		
		cellMenu.getItems().addAll(update);
		
		update.setOnAction(e ->{
			try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())){
				connection.setCatalog(session.getDatabase());
				Statement statement = connection.createStatement();

				ArrayList<Integer> keyIndices = getKeyIndices();
				
				int rowIndex = cell.getIndex();
				int columnIndex = currentTable.getColumns().indexOf(cell.getTableColumn());
				
				
				String queryString = "UPDATE " + tableName + " SET " + cell.getTableColumn().getText() 
						+ " = \'" + currentTable.getItems().get(rowIndex).get(columnIndex) +"\' WHERE ";
				
				for(int i = 0; i < keyIndices.size(); i++){
					int keyIndex = keyIndices.get(i);
					queryString += currentTable.getColumns().get(keyIndex).getText()
							+ " = \'" + currentTable.getItems().get(rowIndex).get(keyIndex) +"\' AND ";
				}
				
				queryString = queryString.substring(0, queryString.length()-4) + ";";
				
				statement.execute(queryString);
			}
			catch(SQLException ex){
				ex.printStackTrace();
			}
		});
		
		return cellMenu;
	}
	
	@SuppressWarnings("rawtypes")
	private static ArrayList<Integer> getKeyIndices(){
		
		ArrayList<Integer> keyIndices = null;
		
		try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
			connection.setCatalog(session.getDatabase());
			Statement statement = connection.createStatement();
			String queryString = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE "
					+ "TABLE_NAME = \'" + tableName + "\' AND COLUMN_KEY = \'PRI\';";
			
			ResultSet tableKeyResultSet = statement.executeQuery(queryString);
			
			keyIndices = new ArrayList<>();
			
			//consider changing foreach to for loop to eliminate index search
			while(tableKeyResultSet.next()){
				for(TableColumn c : tableview.getColumns()){
					if(c.getText().equals(tableKeyResultSet.getString(1)))
						keyIndices.add(tableview.getColumns().indexOf(c));
				}
			}
			statement.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
			return keyIndices;
	}
	
	public static void initTableList(){
	
		if(tables.getItems().size()>0){
			tables.getItems().clear();
		}
		
		tables.setCellFactory(new Callback<ListView<String>, ListCell<String>>(){
			public ListCell<String> call(ListView<String> listParam){
				
				ListCell<String> lc = new ListCell<String>(){
					@Override
					protected void updateItem(String item, boolean empty) {
					     super.updateItem(item, empty);

					     if (empty || item == null) {
					         setText(null);
					         setGraphic(null);
					     } else {
					         setText(item.toString());
					     }
					 }
				};
				lc.setOnContextMenuRequested(e ->{
					@SuppressWarnings("unchecked")
					final ContextMenu tableListCellMenu = getTableListMenu(
							((ListCell<String>)e.getSource()));
					lc.setContextMenu(tableListCellMenu);
					
					tableListCellMenu.show((Node)e.getSource(), e.getScreenX(), e.getScreenY());
				});
				
				return lc;
			}
		});
		
		
		ObservableList<String> data = FXCollections.observableArrayList();
		
		try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())) 
		{
			connection.setCatalog(session.getDatabase());

			Statement statement = connection.createStatement();
			ResultSet tableQuery = statement.executeQuery("show tables;");
		
			while(tableQuery.next()){
				data.add(tableQuery.getString(1));
			}
			
			tables.setItems(data);
			
			tables.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>(){
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
						if(newValue!=null)
							tableName = newValue;
							initTable();   
				}
			});
			
			statement.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	static ContextMenu getColumnContextMenu(TableColumn<ObservableList<String>, String> col){
		ContextMenu colMenu = new ContextMenu();
		MenuItem rename = new MenuItem("Rename");
		MenuItem delete = new MenuItem("Delete");
		colMenu.getItems().addAll(rename, delete);
		
		rename.setOnAction(e ->
		{
			if(session!=null){
				try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
					connection.setCatalog(session.getDatabase());
					
					Stage columnRenameStage = new Stage();
					columnRenameStage.setTitle("Column Rename");
					
					Label columnRenameLabel = new Label("New Name: ");
					HBox.setMargin(columnRenameLabel, new Insets(3, 0, 0, 0));
					TextField columnRenameInput = new TextField(col.getText());
					
					HBox columnRenameRow = new HBox(5, columnRenameLabel, columnRenameInput);
					
					Button okButton = new Button("OK");
					Button cancelButton = new Button("Cancel");
					HBox buttonRow = new HBox(5, okButton, cancelButton);
					buttonRow.setAlignment(Pos.CENTER);

					okButton.setOnAction(v -> {
						try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
								session.getPassword())){

							c.setCatalog(session.getDatabase());
							Statement statement = c.createStatement();
							
							PreparedStatement prepared = c.prepareStatement(
									"SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE "
									+ "TABLE_SCHEMA = ? AND TABLE_NAME = ? and COLUMN_NAME = ?");
							
							prepared.setString(1, c.getCatalog());
							prepared.setString(2, tableName);
							prepared.setString(3, col.getText());
							
							ResultSet colType = prepared.executeQuery();
							colType.next();
							
							statement.execute("ALTER TABLE " + tableName + 
									" CHANGE " + col.getText() + " " + columnRenameInput.getText() + " "
									+ colType.getString(1) + ";");
							
							prepared.close();
							statement.close();
							c.close();
							columnRenameStage.close();
							initTable();
						}
						catch(SQLException ex){
							ex.printStackTrace();
						}
					});
					
					cancelButton.setOnAction(v -> {
						columnRenameStage.close();
					});
					
					VBox tableRenameBox = new VBox(5, columnRenameRow, buttonRow);
					columnRenameStage.setScene(new Scene(tableRenameBox));
					columnRenameStage.show();
					connection.close();
				}
				catch(SQLException ex){
					ex.printStackTrace();
				}
			}
		});
		
		delete.setOnAction(e ->{
			Stage confirmDeleteStage = new Stage();
			confirmDeleteStage.setTitle("Confirm Delete");
			
			Label confirmDeleteLabel = new Label("Are you sure you want to delete " 
					+ col.getText() + "?");
			HBox.setMargin(confirmDeleteLabel, new Insets(3, 0, 0, 0));
			confirmDeleteLabel.setAlignment(Pos.CENTER);
			HBox confirmDeleteRow = new HBox(confirmDeleteLabel);
			
			
			Button yesButton = new Button("Yes");
			yesButton.requestFocus();
			Button noButton = new Button("No");
			HBox buttonRow = new HBox(5, yesButton, noButton);
			buttonRow.setAlignment(Pos.CENTER);
			
			yesButton.setOnAction(v -> {
				try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
						session.getPassword())){

					c.setCatalog(session.getDatabase());
					Statement statement = c.createStatement();

					statement.execute("ALTER TABLE " + tableName + " DROP COLUMN " + 
							col.getText() + ";");
					statement.close();
					c.close();
					confirmDeleteStage.close();
					initTableList();
				}
				catch(SQLException ex){
					ex.printStackTrace();
				}
			});
			
			noButton.setOnAction(v -> {
						confirmDeleteStage.close();
			});
			
			VBox confirmDeleteBox = new VBox(5, confirmDeleteRow, buttonRow);
			confirmDeleteStage.setScene(new Scene(confirmDeleteBox));
			confirmDeleteStage.show();	
		});
		return colMenu;
	}
	
	static ContextMenu getTableListMenu(ListCell<String> menuSource){
		ContextMenu tableListCellMenu = new ContextMenu();
		
		MenuItem rename = new MenuItem("Rename");
		MenuItem delete = new MenuItem("Delete");
		
		tableListCellMenu.getItems().addAll(rename, delete);
		
		rename.setOnAction(e ->
		{
			if(session!=null){
				try(Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
				session.getPassword())){
					connection.setCatalog(session.getDatabase());
					
					Stage tableRenameStage = new Stage();
					tableRenameStage.setTitle("Table Rename");
					
					Label tableRenameLabel = new Label("New Name: ");
					HBox.setMargin(tableRenameLabel, new Insets(3, 0, 0, 0));
					TextField tableRenameInput = new TextField(menuSource.getItem());
					
					HBox tableRenameRow = new HBox(5, tableRenameLabel, tableRenameInput);
					
					Button okButton = new Button("OK");
					Button cancelButton = new Button("Cancel");
					HBox buttonRow = new HBox(5, okButton, cancelButton);
					buttonRow.setAlignment(Pos.CENTER);

					okButton.setOnAction(v -> {
						try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
								session.getPassword())){

							c.setCatalog(session.getDatabase());
							Statement statement = c.createStatement();

							statement.execute("ALTER TABLE " + menuSource.getItem().toString() + 
									" RENAME TO " + tableRenameInput.getText() + ";");
							statement.close();
							c.close();
							tableRenameStage.close();
							initTableList();
						}
						catch(SQLException ex){
							ex.printStackTrace();
						}
					});
					
					cancelButton.setOnAction(v -> {
						tableRenameStage.close();
					});
					
					VBox tableRenameBox = new VBox(5, tableRenameRow, buttonRow);
					tableRenameStage.setScene(new Scene(tableRenameBox));
					tableRenameStage.show();
					connection.close();
				}
				catch(SQLException ex){
					ex.printStackTrace();
				}
			}
		});
		
		delete.setOnAction(e ->{
			Stage confirmDeleteStage = new Stage();
			confirmDeleteStage.setTitle("Confirm Delete");
			
			Label confirmDeleteLabel = new Label("Are you sure you want to delete " 
					+ menuSource.getItem() + "?");
			HBox.setMargin(confirmDeleteLabel, new Insets(3, 0, 0, 0));
			confirmDeleteLabel.setAlignment(Pos.CENTER);
			HBox confirmDeleteRow = new HBox(confirmDeleteLabel);
			
			
			Button yesButton = new Button("Yes");
			yesButton.requestFocus();
			Button noButton = new Button("No");
			HBox buttonRow = new HBox(5, yesButton, noButton);
			buttonRow.setAlignment(Pos.CENTER);
			
			yesButton.setOnAction(v -> {
				try(Connection c = DriverManager.getConnection(session.getURL(), session.getUserName(), 
						session.getPassword())){

					c.setCatalog(session.getDatabase());
					Statement statement = c.createStatement();

					statement.execute("DROP TABLE " + menuSource.getItem().toString() + ";");
					statement.close();
					c.close();
					confirmDeleteStage.close();
					tableName = null;
					initTableList();
				}
				catch(SQLException ex){
					ex.printStackTrace();
				}
			});
			
			noButton.setOnAction(v -> {
						confirmDeleteStage.close();
			});
			
			VBox confirmDeleteBox = new VBox(5, confirmDeleteRow, buttonRow);
			confirmDeleteStage.setScene(new Scene(confirmDeleteBox));
			confirmDeleteStage.show();	
		});
		
		return tableListCellMenu;
	}
	
	private void initMenuBar(){
		
		Menu file = new Menu("File");
		MenuItem imp = new MenuItem("Import");
		MenuItem connect = new MenuItem("Connect");
		MenuItem save = new MenuItem("Save");
		MenuItem saveAs = new MenuItem("Save As");
		MenuItem quit = new MenuItem("Quit");
		
		file.getItems().addAll(connect, imp, save, saveAs, quit);
		
		mainMenu = new MenuBar(file);
		
		connect.setOnAction(e -> {
			Stage connectWindow = new Stage();
			connectWindow.initOwner(stage);
			connectWindow.setTitle("Connect");
			
			TextField urlInput = new TextField("localhost:3306");
			TextField accountInput = new TextField();
			PasswordField passwordInput = new PasswordField();
			
			HBox urlRow = new HBox(5, new Label("Domain: "), urlInput);
			urlInput.setAlignment(Pos.CENTER);
			HBox accountRow = new HBox(5, new Label("Account: "), accountInput);
			HBox passwordRow = new HBox(5, new Label("Password: "), passwordInput);
			
			Button ok = new Button("OK");
			Button cancel = new Button("Cancel");
			
			cancel.setOnAction(v ->{
				connectWindow.close();
			});
			
			ok.setOnAction(v -> {
				
				session = new SessionConfig("jdbc:mysql://" + urlInput.getText() + 
						"/java?verifyServerCertificate=false&useSSL=true",
						accountInput.getText(),
						passwordInput.getText());

				try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
						session.getPassword())) 
				{
					Statement statement = connection.createStatement();
					ResultSet databases = statement.executeQuery("show databases;");
				    if(mainMenu.getMenus().size()==1){
				    	Menu databaseMenu = new Menu("Databases");
				    
				    	while(databases.next()){
				    		databaseMenu.getItems().add(new MenuItem(databases.getString(1)));
				    	}

				    	for(MenuItem m : databaseMenu.getItems()){
				    		m.setOnAction( ev->{
				    			session.setDatabase(m.getText());
				    			initTableList();
				    		});
				    	}
				    
				    	initTableList();
				    
				    	mainMenu.getMenus().add(databaseMenu);
				    }
				    
				    databases.close();
				    statement.close();
				    connection.close();
				    
				} catch (SQLException ex) {
				    throw new IllegalStateException("Cannot connect the database!", ex);
				}
				
				connectWindow.close();
			});
			
			HBox buttons = new HBox(5, ok, cancel);
			buttons.setAlignment(Pos.CENTER);
			
			VBox vBox = new VBox(10, urlRow, accountRow, passwordRow, buttons);
			
			Scene scene = new Scene(vBox);
			connectWindow.setScene(scene);
			connectWindow.show();
		});
		
		imp.setOnAction(e -> {
			FileImporter importer = new FileImporter(session, tables, tableview);
			importer.getFile();
		});
	}
	
	private HBox getConnectionBar(){
		
		Label urlLabel = new Label("Server URL:");
		HBox.setMargin(urlLabel, new Insets(3, 0, 0, 0));
		TextField urlInput = new TextField("localhost:3306");
		
		Label usernameLabel = new Label("Username:");
		HBox.setMargin(usernameLabel, new Insets(3, 0, 0, 0));
		TextField usernameInput = new TextField();

		Label passwordLabel = new Label("Password: ");
		HBox.setMargin(passwordLabel, new Insets(3, 0, 0, 0));
		PasswordField passwordInput = new PasswordField();
		
		Button connectButton = new Button("Connect");
		
		connectButton.setOnAction(e ->{
			session = new SessionConfig("jdbc:mysql://" + urlInput.getText() + 
					"/java?verifyServerCertificate=false&useSSL=true",
					usernameInput.getText(),
					passwordInput.getText());

			try (Connection connection = DriverManager.getConnection(session.getURL(), session.getUserName(), 
					session.getPassword())) 
			{
				Statement statement = connection.createStatement();
				ResultSet databases = statement.executeQuery("show databases;");
			    if(mainMenu.getMenus().size()==1){
			    	Menu databaseMenu = new Menu("Databases");
			    
			    	while(databases.next()){
			    		databaseMenu.getItems().add(new MenuItem(databases.getString(1)));
			    	}

			    	for(MenuItem m : databaseMenu.getItems()){
			    		m.setOnAction( ev->{
			    			session.setDatabase(m.getText());
			    			initTableList();
			    		});
			    	}
			    
			    	initTableList();
			    
			    	mainMenu.getMenus().add(databaseMenu);
			    }
			    
			    statement.close();
			    connection.close();
			    
			} catch (SQLException ex) {
			    throw new IllegalStateException("Cannot connect the database!", ex);
			}
		});
		
		Button addColumnButton = new Button("Add Column");
		HBox.setMargin(addColumnButton, new Insets(0, 65, 0, 0));
		addColumnButton.setOnAction(e -> {
			ColumnBuilder builder = new ColumnBuilder(session);
			builder.buildColumn();
		});
		
		Button addTableButton = new Button("Add Table");
		addTableButton.setOnAction(e ->{
			TableBuilder builder = new TableBuilder(session);
			builder.buildTable();
		});

		Region space = new Region();
		HBox.setHgrow(space, Priority.ALWAYS);
		
		return new HBox(5, urlLabel, urlInput, usernameLabel, 
				usernameInput, passwordLabel, passwordInput, 
				connectButton, space, addTableButton, addColumnButton);
	}
	
	public static String getTableName(){
		return (tableName==null) ? "" : tableName;
	}
	
	public static void main(String[] args){
		Application.launch(args);
	}
}