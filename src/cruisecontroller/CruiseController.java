/*
 * Hopefully this file wont stay long in the dev branch
 */
package cruisecontroller;

import CarSimulator.CarSimulator;
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
    static double kp, ki, kd;

    HBox hbox = new HBox();
    BorderPane root = new BorderPane();

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
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                lbl.setText("SUCCESS");
            }
        });

        lbl.setText("blub");
        vb.getChildren().add(lbl);
        vb.getChildren().add(btn);
        root.setLeft(vb);
        //---------END OF LEFT PART-------------
        //---------REMAINING PART--------------
        stage.setTitle("Animated Line Chart Sample");
//        init(stage);
        Scene scene = new Scene(root, 600, 300);
        stage.setScene(scene);
        stage.show();
        //--------END OF REMAINING-----------
        CruiseController cc = new CruiseController();
        int speed = 50;
        SetTunings(0.3, 0.2, 0.2);

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

//        while (true) {
//            Thread.sleep(100);
//            cc.Compute(carSim, speed);
//            System.out.println("Current Speed:" + carSim.getSpeed());
//        }
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
        System.out.println("timechange:" + timeChange);

        /*Compute all the working error variables*/
        Input = carSim.getSpeed();
        Setpoint = speed;
        double error = Setpoint - Input;
        System.out.println("Error: " + error);
        errSum += (error * timeChange);
        System.out.println("ErrSum: " + errSum);
        double dErr = (error - lastErr) / timeChange;

        /*Compute PID Output*/
        Output = kp * error + ki * errSum + kd * dErr;
//        System.out.println("error: " + error + " errSum: " + errSum + " dErr: " + dErr);
//        System.out.println("Kp: " + kp + " Ki: " + ki + " Kd: " + kd);
//        System.out.println(ki * errSum);
        System.out.println("Output: " + Output);
        carSim.setAcceleration(Output);

        /*Remember some variables for next time*/
        lastErr = error;
        lastTime = now;
    }

    public static void SetTunings(double Kp, double Ki, double Kd) {
        kp = Kp;
        ki = Ki;
        kd = Kd;
    }

}
