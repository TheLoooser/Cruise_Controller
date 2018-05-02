package cruisecontroller;

import CarSimulator.CarSimulator;

/**
 *
 * @author Dizzy
 */
public class SimplePID {

    /*working variables*/
    static double lastTime = System.currentTimeMillis();
    static double Input, Output, Setpoint;
    static double ITerm, lastInput;
    static double kp, ki, kd;
    static int SampleTime = 1000; //1 sec
    static double outMin, outMax;
    private static boolean inAuto = false;

    private static final int MANUAL = 0;
    private static final int AUTOMATIC = 1;
    private static final int DIRECT = 0;
    private static final int REVERSE = 1;
    private static int controllerDirection = DIRECT;

    public static void Compute(CarSimulator carSim, int speed) {
        if (!inAuto) {
            return;
        }
        long now = System.currentTimeMillis();
        double timeChange = (now - lastTime);
        if (timeChange >= SampleTime) {
            /*Compute all the working error variables*/
            Input = carSim.getSpeed();
            Setpoint = speed;
            double error = Setpoint - Input;
            ITerm += (ki * error);
            if (ITerm > outMax) {
                ITerm = outMax;
            } else if (ITerm < outMin) {
                ITerm = outMin;
            }
            double dInput = (Input - lastInput);

            /*Compute PID Output*/
            Output = kp * error + ITerm - kd * dInput;
            if (Output > outMax) {
                Output = outMax;
            } else if (Output < outMin) {
                Output = outMin;
            }
            carSim.setAcceleration(Output);
            System.out.println(carSim.getSpeed());

            /*Remember some variables for next time*/
            lastInput = Input;
            lastTime = now;
        }
    }

    public static void SetTunings(double Kp, double Ki, double Kd) {
        if (Kp < 0 || Ki < 0 || Kd < 0) {
            return;
        }

        double SampleTimeInSec = ((double) SampleTime) / 1000;
        kp = Kp;
        ki = Ki * SampleTimeInSec;
        kd = Kd / SampleTimeInSec;

        if (controllerDirection == REVERSE) {
            kp = (0 - kp);
            ki = (0 - ki);
            kd = (0 - kd);
        }
    }

    public static void SetSampleTime(int NewSampleTime) {
        if (NewSampleTime > 0) {
            double ratio = (double) NewSampleTime
                    / (double) SampleTime;
            ki *= ratio;
            kd /= ratio;
            SampleTime = NewSampleTime;
        }
    }

    public static void SetOutputLimits(double Min, double Max) {
        if (Min > Max) {
            return;
        }
        outMin = Min;
        outMax = Max;

        if (Output > outMax) {
            Output = outMax;
        } else if (Output < outMin) {
            Output = outMin;
        }

        if (ITerm > outMax) {
            ITerm = outMax;
        } else if (ITerm < outMin) {
            ITerm = outMin;
        }
    }

    public static void SetMode(int Mode) {
        boolean newAuto = (Mode == AUTOMATIC);
        if (newAuto == !inAuto) {
            /*we just went from manual to auto*/
            Initialize();
        }
        inAuto = newAuto;
    }

    private static void Initialize() {
        lastInput = Input;
        ITerm = Output;
        if (ITerm > outMax) {
            ITerm = outMax;
        } else if (ITerm < outMin) {
            ITerm = outMin;
        }
    }

    public static void SetControllerDirection(int Direction) {
        controllerDirection = Direction;
    }
}

