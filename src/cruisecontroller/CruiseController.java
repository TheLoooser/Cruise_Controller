/*
 * Hopefully this file wont stay long in the dev branch
 */
package cruisecontroller;

import CarSimulator.CarSimulator;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * @author Jonas Diesbach
 */
public class CruiseController extends Application implements Runnable {

    /*working variables*/
    double lastTime = System.currentTimeMillis();
    static double Input, Output, Setpoint;
    static double errSum, lastErr;
    static double kp = 0.3;
    static double ki = 0.2;
    static double kd = 0.2;

    HBox hbox = new HBox();
    BorderPane root = new BorderPane();
    //make speed a double???
    static int speed = 0;

    @Override
    public void run() {
    }

    @Override
    public void start(Stage stage) throws Exception {
        //-----------LEFT PART-----------------
        VBox vb = new VBox();
        vb.setAlignment(Pos.TOP_CENTER);
        Label lbl = new Label();
        Button btn = new Button();
        TextField tf = new TextField();
        TextField tfp = new TextField();
        TextField tfi = new TextField();
        TextField tfd = new TextField();
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                lbl.setText("SUCCESS");
            }
        });
        //Textfield for the desired speed
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
//            if(isInteger){
//                System.out.println("Success");
//            }
            lbl.setText(newValue);
            if (!isInteger || newValue.equals("")) {
                //the speed doesn't necessarily need to be set back to zero
                speed = 0;
                lbl.setText("NAN");
            } else {
                double d = Double.valueOf(newValue);
                speed = (int) d;
            }
        });
        //Textfield for constant kp
        tfp.setText(String.valueOf(0.3));
        tfp.textProperty().addListener((observable, oldValue, newValue) -> {
            lbl.setText(newValue);
            if (newValue.equals("")) {
                kp = 0;
            } else {
                kp = Double.valueOf(newValue);
            }
        });
        //Textfield for constant ki
        tfi.setText(String.valueOf(0.2));
        tfi.textProperty().addListener((observable, oldValue, newValue) -> {
            lbl.setText(newValue);
            if (newValue.equals("")) {
                ki = 0;
            } else {
                ki = Double.valueOf(newValue);
            }
        });
        //Textfield for constant kd
        tfd.setText(String.valueOf(0.2));
        tfd.textProperty().addListener((observable, oldValue, newValue) -> {
            lbl.setText(newValue);
            if (newValue.equals("")) {
                kd = 0;
            } else {
                kd = Double.valueOf(newValue);
            }
        });

//        lbl.setText("blub");
        btn.setText("Button");
        vb.getChildren().add(lbl);
        vb.getChildren().add(btn);
        vb.getChildren().add(tf);
        vb.getChildren().add(tfp);
        vb.getChildren().add(tfi);
        vb.getChildren().add(tfd);
        root.setLeft(vb);
        //---------END OF LEFT PART-------------
        //---------REMAINING PART--------------
        stage.setTitle("Animated Line Chart Sample");
        Scene scene = new Scene(root, 600, 300);
        stage.setScene(scene);
        stage.show();
        //--------END OF REMAINING-----------
        
        CruiseController cc = new CruiseController();
        CarSimulator carSim = new CarSimulator();
        (new Thread(carSim)).start();

        //add timeline instead of while true
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.1), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
//                System.out.println("this is called every 5 seconds on UI thread");
                cc.Compute(carSim, speed);
                System.out.println("Current Speed:" + carSim.getSpeed());
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        //handles the window closing event, stops simulation
        scene.getWindow().setOnCloseRequest(new EventHandler<javafx.stage.WindowEvent>() {
            @Override
            public void handle(javafx.stage.WindowEvent event) {
                carSim.stop();
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here
        launch(args);
    }

    /*
    http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/
     */
    public void Compute(CarSimulator carSim, double speed) {
        /*How long since we last calculated*/
        long now = System.currentTimeMillis();
        double timeChange = (double) (now - lastTime) / 1000;
//        System.out.println("timechange:" + timeChange);

        /*Compute all the working error variables*/
        Input = carSim.getSpeed();
        Setpoint = speed;
        double error = Setpoint - Input;
//        System.out.println("Error: " + error);
        errSum += (error * timeChange);
//        System.out.println("ErrSum: " + errSum);
        double dErr = (error - lastErr) / timeChange;

        /*Compute PID Output*/
        Output = kp * error + ki * errSum + kd * dErr;
//        System.out.println("Output: " + Output);
        carSim.setAcceleration(Output);

        /*Remember some variables for next time*/
        lastErr = error;
        lastTime = now;
    }
    
    //this function shouldn't even be necessary
    public static void SetTunings(double Kp, double Ki, double Kd) {
        kp = Kp;
        ki = Ki;
        kd = Kd;
    }

}
