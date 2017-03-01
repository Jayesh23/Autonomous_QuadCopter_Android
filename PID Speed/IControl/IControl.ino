#include <Servo.h>

#define MAX_SIGNAL 2200
#define MIN_SIGNAL 800
#define MOTOR_PIN1  9
#define MOTOR_PIN2  10
#define MOTOR_PIN3  11
#define MOTOR_PIN4  12

#define START_CMD_CHAR '>'

String inText;
double fl,fr,br,bl;
/*
double sp_roll = 0;
double sp_pitch = 0;
double sp_yaw = 0;
double pKp = 2;
double pKi = 5;
double pKd = 1;
double rKp = 2;
double rKi = 5;
double rKd = 1;
*/
double outMax = 200;
double throttle = 1000;

Servo motor1;
Servo motor2;
Servo motor3;
Servo motor4;


Servo front_left_ESC, front_right_ESC, back_right_ESC, back_left_ESC;
//PID rPID(&roll, &roll_speed, &sp_roll, rKp,rKi,rKd, DIRECT);
//PID r_PID(outMin, outMax);
//PID pPID(&pitch, &pitch_speed, &sp_pitch, pKp,pKi,pKd, DIRECT);
//PID p_PID(outMin, outMax);

  void calibrate()
  {
  Serial.begin(9600);
  Serial.println("Program begin...");
  Serial.println("This program will calibrate the ESC.");

  Serial.println("Now writing maximum output.");
  Serial.println("Turn on power source, then wait 2 seconds and press any key.");
  front_left_ESC.writeMicroseconds(MAX_SIGNAL);
  front_right_ESC.writeMicroseconds(MAX_SIGNAL);
  back_right_ESC.writeMicroseconds(MAX_SIGNAL);
  back_left_ESC.writeMicroseconds(MAX_SIGNAL);

  delay(3000);
  // Send min output
  Serial.println("Sending minimum output");
  front_left_ESC.writeMicroseconds(MIN_SIGNAL);
  front_right_ESC.writeMicroseconds(MIN_SIGNAL);
  back_right_ESC.writeMicroseconds(MIN_SIGNAL);
  back_left_ESC.writeMicroseconds(MIN_SIGNAL);

}

void setup() {
  Serial.begin(9600);
  Serial.flush();
  front_left_ESC.attach(9);
  front_right_ESC.attach(10);
  back_right_ESC.attach(11);
  back_left_ESC.attach(12); 
  //rPID.SetOutputLimits(0, outMax);
  //pPID.SetOutputLimits(0, outMax);
  //rPID.SetMode(AUTOMATIC);
  //pPID.SetMode(AUTOMATIC);
  calibrate();
  Serial.flush();
}

void loop()
{

  Serial.flush();
  int inCommand = 0;
  int sensorType = 0;
  unsigned long logCount = 0L;

  char getChar = ' ';  //read serial

  // wait for incoming data
  if (Serial.available() < 1) return; // if serial empty, return to loop().

  // parse incoming command start flag 
  getChar = Serial.read();
  if (getChar != START_CMD_CHAR) return; // if no command start flag, return to loop().

  // parse incoming pin# and value  
  
  fl = Serial.parseFloat();    // 1st sensor value
  fr = Serial.parseFloat();  // 2rd sensor value if exists
  br = Serial.parseFloat();   // 3rd sensor value if exists
  bl = Serial.parseFloat();
   
  /*Serial.println(sensorType);
  Serial.print("Angles = ");
  Serial.print(roll);
  Serial.print(",  ");
   Serial.print(pitch);
   Serial.print(",  ");
    Serial.println(yaw);
  */
 
  //rPID.Compute();
  //pPID.Compute();   
  //Serial.print("roll_speed : ");
  //Serial.print(roll_speed);
  //Serial.print("pitch_speed : ");
  //Serial.println(pitch_speed);
  
  double front_left  = map(fl ,0,250, 1000, 2000); // front left
  double front_right = map(fr ,0,250, 1000, 2000);//front right
  double back_right  = map(br ,0,250, 1000, 2000);//back right
  double back_left   = map(bl ,0,250, 1000, 2000);//back left

  /*
  double front_left=(throttle+roll_speed+pitch_speed); // front left
  double front_right=(throttle-roll_speed+pitch_speed);//front right
  double back_right=(throttle-roll_speed-pitch_speed);//back right
  double back_left=(throttle+roll_speed-pitch_speed);//back left
  */
  
  front_left_ESC.writeMicroseconds(front_left);
  front_right_ESC.writeMicroseconds(front_right);
  back_right_ESC.writeMicroseconds(back_right);
  back_left_ESC.writeMicroseconds(back_left);
 
  Serial.print(front_left);
  Serial.print("' ");
  Serial.print(front_right);
  Serial.print("' ");
  Serial.print(back_right);
  Serial.print("' ");
  Serial.println(back_left);
    //delay(1000);
  
}
