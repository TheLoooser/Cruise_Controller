/*
 * Hopefully this file wont stay long in the test branch
 */
package cruisecontroller;

import CarSimulator.CarSimulator;
import static cruisecontroller.SimplePID.*;
import static cruisecontroller.LineChart.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import static javafx.application.Application.launch;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author Jonas Diesbach
 */
public class CruiseController extends Application {

    static BorderPane root = new BorderPane();
    Dimension monitor = Toolkit.getDefaultToolkit().getScreenSize();
    static double speed = 13.9; //equals 50km/h

    @Override
    public void start(Stage stage) throws Exception {
        SetSampleTime(25);
        kp = 3.0;
        ki = 0.35;
        kd = 0.3;
        SetControllerDirection(0);
        SetMode(1);
//        SetTunings(3.0, 0.35, 0.3);

        /*
        Acceleration: https://www.engadget.com/2017/02/07/tesla-model-s-ludicrous-acceleration-record/
        Breaking: http://www.motortrend.com/news/20-best-60-to-0-distances-recorded/
         */
        SetOutputLimits(-26.226, 11.78);
//        SetOutputLimits(-26.226, 26.226);

        //-----------LEFT PART-----------------
        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER_LEFT);
        vb.setPadding(new Insets(15, 12, 15, 12));
        vb.setSpacing(15);
        DecimalFormat df = new DecimalFormat("#0.00000");
        Label lbl_err = new Label();
        Label lbl_desspeed = new Label();
        Label lbl_actspeed = new Label();
        Label lbl_kp = new Label();
        Label lbl_ki = new Label();
        Label lbl_kd = new Label();
        Label lbl_setSpeed = new Label();
        Button btn = new Button();
        Slider sl = new Slider();
        sl.setMin(0);
        sl.setMax(124); //https://en.wikipedia.org/wiki/Production_car_speed_record even though the simulation can only go up to about 115m/s tops
        TextField tf = new TextField();
        TextField tfp = new TextField();
        TextField tfi = new TextField();
        TextField tfd = new TextField();

        sl.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                speed = (double) newValue;
                tf.setText(String.valueOf(df.format(speed)));
            }
        });

        //Textfield for the desired speed
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);

            if (!isInteger || newValue.equals("")) {
                lbl_desspeed.setText("Desired Speed: NAN");
            } else {
                double d = Double.valueOf(newValue);
                speed = d;
                lbl_desspeed.setText("Desired Speed: " + df.format(Double.valueOf(newValue)));
            }
        });

        //Textfield for constant kp
        tfp.setText(String.valueOf(kp));
        tfp.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
            if (!isInteger || newValue.equals("")) {
                //do nothing
            } else {
                double d = Double.valueOf(newValue);
                kp = d;
            }
        });
        //Textfield for constant ki
        tfi.setText(String.valueOf(ki));
        tfi.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
            if (!isInteger || newValue.equals("")) {
                //do nothing
            } else {
                double d = Double.valueOf(newValue);
                ki = d;
            }
        });
        //Textfield for constant kd
        tfd.setText(String.valueOf(kd));
        tfd.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
            if (!isInteger || newValue.equals("")) {
                //do nothing
            } else {
                double d = Double.valueOf(newValue);
                kd = d;
            }
        });
        lbl_desspeed.setText("Desired Speed: " + df.format(speed));
        vb.getChildren().add(lbl_desspeed);
        lbl_actspeed.setText("Actual Speed: ");
        vb.getChildren().add(lbl_actspeed);
        lbl_err.setText("Error: " + df.format(speed));
        vb.getChildren().add(lbl_err);
        lbl_setSpeed.setText("Choose your Speed (m/s): ");
        vb.getChildren().add(lbl_setSpeed);
        vb.getChildren().add(sl);
        vb.getChildren().add(tf);
        lbl_kp.setText("Kp: ");
        vb.getChildren().add(lbl_kp);
        vb.getChildren().add(tfp);
        lbl_ki.setText("Ki: ");
        vb.getChildren().add(lbl_ki);
        vb.getChildren().add(tfi);
        lbl_kd.setText("Kd: ");
        lbl_kd.setAlignment(Pos.CENTER_LEFT);
        vb.getChildren().add(lbl_kd);
        vb.getChildren().add(tfd);
        vb.setPrefWidth(monitor.width * 0.1);
        root.setLeft(vb);
        //---------END OF LEFT PART-------------
        //---------REMAINING PART--------------
        stage.setTitle("Car Simulation using a PID");
        initil(stage);
        Scene scene = new Scene(root, monitor.width * 0.9, monitor.height * 0.9);
        stage.setScene(scene);
        stage.show();

        //--------END OF REMAINING-----------
        //--------START OF PID-----------
        //good values: 30 8 0.5, 15 2 0.1
//        SetTunings(30, 5, 0.01);
        //best values so far: 11, 0.0015, 0.3
        //best values for acc = break = 26.266: 25, 0.5, 2
//        SetTunings(10, 0.15, 0.01);
        SetTunings(10.5, 0.165, 0.02);
        CarSimulator carSim = new CarSimulator();
        (new Thread(carSim)).start();

        //add timeline instead of while true
        Timeline timeline;
        timeline = new Timeline(new KeyFrame(Duration.millis(25), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //                System.out.println("this is called every 5 seconds on UI thread");
                Compute(carSim, speed);
                lbl_actspeed.setText("Actual Speed: " + df.format(carSim.getSpeed()));
                lbl_err.setText("Error: " + df.format(Setpoint - Input));
//                System.out.println("Current Speed:" + carSim.getSpeed());
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        //---------END OF PID------------

        //-------LINE CHART----------------
        executor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });

        AddToQueue addToQueue = new AddToQueue();
        executor.execute(addToQueue);
        //-- Prepare Timeline
        prepareTimeline();
        //--------END OF LINE CHART----------

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
        launch(args);
    }
}
