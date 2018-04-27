/*
 * Hopefully this file wont stay long in the dev branch
 */
package cruisecontroller;

import CarSimulator.CarSimulator;

/**
 *
 * @author Jonas Diesbach
 */
public class CruiseController implements Runnable {

    /*working variables*/
    double lastTime = System.currentTimeMillis();
    static double Input, Output, Setpoint;
    static double errSum, lastErr;
    static double kp, ki, kd;

    @Override
    public void run() {
//        System.out.println("Hello from a thread!");
//        Compute(50);
//        System.out.println();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here
        CruiseController cc = new CruiseController();
        int speed = 50;
        SetTunings(0.3, 0.2, 0.2);
//        CarSimulator carSim1 = new CarSimulator();

//        while (carSim.getSpeed() < 20) {
//            carSim.setAcceleration(10);
//            System.out.println(carSim.getSpeed());
//        }
//        carSim.setAcceleration((double) 20.0);
//        Thread.sleep(1000);
        CarSimulator carSim = new CarSimulator();
        (new Thread(carSim)).start();
//        carSim.setAcceleration(20.0);

//        System.out.println(carSim.getSpeed());
        while (true) {
            Thread.sleep(100);
            cc.Compute(carSim, speed);
//            carSim.setAcceleration(10);
            System.out.println("Current Speed:" + carSim.getSpeed());
        }
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
