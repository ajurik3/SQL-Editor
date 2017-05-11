package filters;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class Filter {
	StringProperty column;
	ObjectProperty<ComboBox<String>> operation;
	ObjectProperty<TextField> operand;
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
	
	private void initOperation(){
		operation = new SimpleObjectProperty<ComboBox<String>>();
		ComboBox<String> operations = new ComboBox<String>();
		operations.getItems().addAll("<", ">", "=", "<=", ">=");
		operations.setMinWidth(75);
		operation.setValue(operations);
	}
	
	private void initOperand(){
		operand = new SimpleObjectProperty<TextField>();
		TextField operandInput = new TextField();
		operandInput.setPrefColumnCount(5);
		operand.setValue(operandInput);
		
	}
	
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
