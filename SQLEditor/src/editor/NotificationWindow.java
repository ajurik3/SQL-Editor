package editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


/*
 * This class creates a window which displays Label(s) containing a
 * message and a button labeled "OK".  Optionally, titles and window
 * dimensions may be passed to the constructor.
 */
public class NotificationWindow extends Stage{

	String title;
	
	//displayed as a Label on the window
	String message;
	String message2;
	
	//dimensions of the window
	int width = 0;
	int height = 0;
	
	NotificationWindow(String m){
		message = m;
		createNotification();
	}
	
	NotificationWindow(String t, String m){
		title = t;
		message = m;
		createNotification();
	}
	
	NotificationWindow(String t, String m, String m2){
		title = t;
		message = m;
		message2 = m2;
		createNotification();
	}
	
	NotificationWindow(String t, String m, int w, int h){
		title = t;
		message = m;
		width = w;
		height = h;
		
		createNotification();
	}
	
	NotificationWindow(String t, String m, String m2, int w, int h){
		title = t;
		message = m;
		message2 = m2;
		width = w;
		height = h;
		
		createNotification();
	}
	
	private void createNotification(){
		if(title!=null)
			setTitle(title);
		else
			setTitle("Notification");
		
		if(width>0)
			setMinWidth(width);
		else
			setMinWidth(350);
		
		if(height>0)
			setMinHeight(height);
		
		Label notification = new Label(message);
		Label notification2 = new Label();
		
		if(message2!=null){
			notification2 = new Label(message2);
		}
		
		Button okButton = new Button("OK");
		
		okButton.setOnAction( e-> close());
		
		VBox notificationForm = new VBox(notification);
		
		if(notification2.getText()!=null)
			notificationForm.getChildren().add(notification2);
		
		notificationForm.getChildren().add(okButton);
		VBox.setMargin(okButton, new Insets(3, 0, 0, 0));
		
		notificationForm.setAlignment(Pos.CENTER);
		setScene(new Scene(notificationForm));
		show();
	}
}
