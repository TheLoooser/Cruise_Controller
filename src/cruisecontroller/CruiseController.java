/*
 * Hopefully this file wont stay long in the test branch
 */
package cruisecontroller;

import CarSimulator.CarSimulator;
import static cruisecontroller.SimplePID.*;
import java.awt.Dimension;
import java.awt.Toolkit;
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

    /* line chart variables */
    private static final int MAX_DATA_POINTS = 500;
    private int xSeriesData = 0;
    private XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
    private XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
    private XYChart.Series<Number, Number> series3 = new XYChart.Series<>();
    private ExecutorService executor;
    private ConcurrentLinkedQueue<Number> dataQ1 = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Number> dataQ2 = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Number> dataQ3 = new ConcurrentLinkedQueue<>();

    private NumberAxis xAxis;

    BorderPane root = new BorderPane();
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
//        SetOutputLimits(-26.226, 11.78);
        SetOutputLimits(-26.226, 26.226);

        //-----------LEFT PART-----------------
        VBox vb = new VBox();
        vb.setAlignment(Pos.TOP_CENTER);
        Label lbl = new Label();
        Button btn = new Button();
        Slider sl = new Slider();
        sl.setMin(0);
        sl.setMax(124); //https://en.wikipedia.org/wiki/Production_car_speed_record even though the simulation can only go up to about 115m/s
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
        sl.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                lbl.setText(String.valueOf(newValue));
                speed = (double) newValue;
                tf.setText(String.valueOf(speed));
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
                speed = d;
            }
        });

        //Textfield for constant kp
        tfp.setText(String.valueOf(kp));
        tfp.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
            lbl.setText(newValue);
            if (!isInteger || newValue.equals("")) {
                kp = 0;
            } else {
                double d = Double.valueOf(newValue);
                kp = d;
            }
        });
        //Textfield for constant ki
        tfi.setText(String.valueOf(ki));
        tfi.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
            lbl.setText(newValue);
            if (!isInteger || newValue.equals("")) {
                ki = 0;
            } else {
                double d = Double.valueOf(newValue);
                ki = d;
            }
        });
        //Textfield for constant kd
        tfd.setText(String.valueOf(kd));
        tfd.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isInteger = Pattern.matches("[0-9]*(\\.[0-9]*)?", newValue);
            lbl.setText(newValue);
            if (!isInteger || newValue.equals("")) {
                kd = 0;
            } else {
                double d = Double.valueOf(newValue);
                kd = d;
            }
        });

//        lbl.setText("blub");
        btn.setText("Button");
        vb.getChildren().add(lbl);
        vb.getChildren().add(btn);
        vb.getChildren().add(sl);
        vb.getChildren().add(tf);
        vb.getChildren().add(tfp);
        vb.getChildren().add(tfi);
        vb.getChildren().add(tfd);
        root.setLeft(vb);
        //---------END OF LEFT PART-------------
        //---------REMAINING PART--------------
        stage.setTitle("Car Simulation using a PID");
        init(stage);
        Scene scene = new Scene(root, monitor.width * 0.9, monitor.height * 0.9);
        stage.setScene(scene);
        stage.show();
        //--------END OF REMAINING-----------
        //--------START OF PID-----------
        //best values so far: 11, 0.0015, 0.3
        //best values for acc = break = 26.266: 25, 0.5, 2
        SetTunings(10, 0.1, 0);
        CarSimulator carSim = new CarSimulator();
        (new Thread(carSim)).start();

        //add timeline instead of while true
        Timeline timeline;
        timeline = new Timeline(new KeyFrame(Duration.millis(25), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //                System.out.println("this is called every 5 seconds on UI thread");
                Compute(carSim, speed);
                lbl.setText(String.valueOf(carSim.getSpeed()));
                System.out.println("Current Speed:" + carSim.getSpeed());
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
        // TODO code application logic here
        launch(args);
    }

    //----------------------LINE CHART---------------------
    private void init(Stage primaryStage) {

        xAxis = new NumberAxis(0, MAX_DATA_POINTS, MAX_DATA_POINTS / 10);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);

        NumberAxis yAxis = new NumberAxis();

        // Create a LineChart
        final LineChart<Number, Number> lineChart = new LineChart<Number, Number>(xAxis, yAxis) {
            // Override to remove symbols on each data point
            @Override
            protected void dataItemAdded(XYChart.Series<Number, Number> series, int itemIndex, XYChart.Data<Number, Number> item) {
            }
        };

        lineChart.setAnimated(false);
        lineChart.setTitle("PID in Action");
        lineChart.setHorizontalGridLinesVisible(true);

        // Set Name for Series
        series1.setName("Desired Speed");
        series2.setName("Actual Speed");
        series3.setName("Error");

        // Add Chart Series
        lineChart.getData().addAll(series1, series2, series3);

//        primaryStage.setScene(new Scene(lineChart));
        root.setCenter(lineChart);
    }

    private class AddToQueue implements Runnable {

        public void run() {
            try {
                // add a item of random data to queue
                dataQ1.add(speed);
                dataQ2.add(Input);
//                dataQ3.add(Setpoint - Input);
                dataQ3.add(Output);

                Thread.sleep(50);
                executor.execute(this);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    //-- Timeline gets called in the JavaFX Main thread
    private void prepareTimeline() {
        // Every frame to take any data from queue and add to chart
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                addDataToSeries();
            }
        }.start();
    }

    private void addDataToSeries() {
        for (int i = 0; i < 20; i++) { //-- add 20 numbers to the plot+
            if (dataQ1.isEmpty()) {
                break;
            }
            series1.getData().add(new XYChart.Data<>(xSeriesData++, dataQ1.remove()));
            series2.getData().add(new XYChart.Data<>(xSeriesData++, dataQ2.remove()));
            series3.getData().add(new XYChart.Data<>(xSeriesData++, dataQ3.remove()));
        }
        // remove points to keep us at no more than MAX_DATA_POINTS
        if (series1.getData().size() > MAX_DATA_POINTS) {
            series1.getData().remove(0, series1.getData().size() - MAX_DATA_POINTS);
        }
        if (series2.getData().size() > MAX_DATA_POINTS) {
            series2.getData().remove(0, series2.getData().size() - MAX_DATA_POINTS);
        }
        if (series3.getData().size() > MAX_DATA_POINTS) {
            series3.getData().remove(0, series3.getData().size() - MAX_DATA_POINTS);
        }
        // update
        xAxis.setLowerBound(xSeriesData - MAX_DATA_POINTS);
        xAxis.setUpperBound(xSeriesData - 1);
    }
    //----------------------END OF LINE CHART--------------

}
