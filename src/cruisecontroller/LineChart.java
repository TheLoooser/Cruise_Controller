package cruisecontroller;

import static cruisecontroller.CruiseController.*;
import static cruisecontroller.SimplePID.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import javafx.animation.AnimationTimer;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

/**
 *
 * @author Dizzy
 */
public class LineChart {

    /* line chart variables */
    static final int MAX_DATA_POINTS = 500;
    static int xSeriesData = 0;
    static XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
    static XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
    static XYChart.Series<Number, Number> series3 = new XYChart.Series<>();
    static ExecutorService executor;
    static ConcurrentLinkedQueue<Number> dataQ1 = new ConcurrentLinkedQueue<>();
    static ConcurrentLinkedQueue<Number> dataQ2 = new ConcurrentLinkedQueue<>();
    static ConcurrentLinkedQueue<Number> dataQ3 = new ConcurrentLinkedQueue<>();

    static NumberAxis xAxis;

    //----------------------LINE CHART---------------------
    public static void initil(Stage primaryStage) {

        xAxis = new NumberAxis(0, MAX_DATA_POINTS, MAX_DATA_POINTS / 10);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);

        NumberAxis yAxis = new NumberAxis();

        // Create a LineChart
        final javafx.scene.chart.LineChart<Number, Number> lineChart = new javafx.scene.chart.LineChart<Number, Number>(xAxis, yAxis) {
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

    public static class AddToQueue implements Runnable {

        public void run() {
            try {
                // add a item of random data to queue
                dataQ1.add(speed);
                dataQ2.add(Input);
//                dataQ3.add(Setpoint - Input);
                dataQ3.add(Setpoint - Input);

                Thread.sleep(25);
                executor.execute(this);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    //-- Timeline gets called in the JavaFX Main thread
    public static void prepareTimeline() {
        // Every frame to take any data from queue and add to chart
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                addDataToSeries();
            }
        }.start();
    }

    public static void addDataToSeries() {
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
