package filters;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/*
 * This class contains properties which can be used to apply a filter
 * based on user input.  The filter is applied to the data specified
 * by its column property and the filter applied is determined by any
 * input within the operation and operand controls.  
 */

public class Filter {
	
	//name of the column being filtered
	StringProperty column;
	
	//contains operations which can be performed to filter the data
	//in column
	ObjectProperty<ComboBox<String>> operation;
	
	//the value with which some operation is performed on the data
	ObjectProperty<TextField> operand;
	
	//When clicked, creates a Filter with the same column property 
	//so that additional filters can be added for the same data
	ObjectProperty<Button> copy;
	
	public Filter(){
		column = new SimpleStringProperty("");
		initOperation();
		initOperand();
		initCopy();
	}
	
	public Filter(String columnName){
		column = new SimpleStringProperty(columnName);
		initOperation();
		initOperand();
		initCopy();
	}
	
	//adds supported operations to ComboBox for user to select one
	private void initOperation(){
		operation = new SimpleObjectProperty<ComboBox<String>>();
		ComboBox<String> operations = new ComboBox<String>();
		operations.getItems().addAll("<", ">", "=", "<=", ">=");
		operations.setMinWidth(75);
		operation.setValue(operations);
	}
	
	//creates a TextField for operand input
	private void initOperand(){
		operand = new SimpleObjectProperty<TextField>();
		TextField operandInput = new TextField();
		operandInput.setPrefColumnCount(5);
		operand.setValue(operandInput);
	}
	
	//onAction property of the copy button is defined in FilterCopyCell
	private void initCopy(){
		copy = new SimpleObjectProperty<Button>();
		Button duplicateButton = new Button("Copy");
		copy.setValue(duplicateButton);
	}

	public String getColumn() {
		return column.get();
	}

	public void setColumn(String columnName) {
		column.set(columnName);
	}

	public ComboBox<String> getOperation() {
		return operation.get();
	}

	public void setOperation(ComboBox<String> op) {
		operation.set(op);
	}

	public TextField getOperand() {
		return operand.get();
	}

	public void setOperand(TextField op) {
		operand.set(op);
	}

	public Button getCopy() {
		return copy.get();
	}

	public void setCopy(Button dup) {
		copy.set(dup);
	}
	
	public StringProperty columnProperty(){
		return column;
	}
	
	public ObjectProperty<ComboBox<String>> operationProperty(){
		return operation;
	}
	
	public ObjectProperty<TextField> operandProperty(){
		return operand;
	}
	
	public ObjectProperty<Button> copyProperty(){
		return copy;
	}
}
