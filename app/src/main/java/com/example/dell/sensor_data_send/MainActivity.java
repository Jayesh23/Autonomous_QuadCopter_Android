package com.example.dell.sensor_data_send;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

import static android.R.attr.constantSize;
import static android.R.attr.port;
import static android.R.attr.sessionService;
import static android.provider.ContactsContract.CommonDataKinds.Website.URL;
import static com.example.dell.sensor_data_send.R.id.constants;

import com.example.dell.sensor_data_send.MainController;

public class MainActivity extends Activity
{

    private static final int REQUEST_ENABLE_BT = 1;
    public int PORT = 15000;

    private Socket socket;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    TextView ang, speed;
    Button calibButton, constButton,sendButton;
    private boolean connected = false;
    MainController.MotorsPowers powers;
    TextView textInfo, textStatus;
    ListView listViewPairedDevice;

    //MainController Thrust = new MainController(new MainActivity());
    //float meanThrust;

    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;



    public MyServer server;
    private String text1, text2,serverIpAddress = "192.168.43.31";
    float kp, ki, kd;
    EditText c11, c12, c13, c21, c22, c23, c31, c32, c33,ipAddr;
    PrintWriter out;
    public Handler mHandler = new Handler();
    public int ne[] = new int[10] ;
    public int nw[] = new int[10] ;
    public int se[] = new int[10] ;
    public int sw[] = new int[10] ;
    public int ane, anw, ase,  asw ;
    public static String valuesn ;

    public Runnable mWaitRunnable = new Runnable() {
        public void run() {
            text1 = PosRotSensors.text;
            ang.setText(text1);
            MainController.MotorsPowers m = mainController.getMotorsPowers() ;

            //shift array values
            for(int i=0; i<9 ; i++){
                ne[i] = ne[i+1];
                se[i] = se[i+1];
                nw[i] = nw[i+1];
                sw[i] = sw[i+1];
            }

            ne[9] = m.ne ;
            se[9] = m.se ;
            nw[9] = m.nw ;
            sw[9] = m.sw ;

            ane = (ne[0]+ne[1]+ne[2]+ne[3]+ne[4]+ne[5]+ne[6]+ne[7]+ne[8]+ne[9])/10;
            anw = (nw[0]+nw[1]+nw[2]+nw[3]+nw[4]+nw[5]+nw[6]+nw[7]+nw[8]+nw[9])/10;
            ase = (se[0]+se[1]+se[2]+se[3]+se[4]+se[5]+se[6]+se[7]+se[8]+se[9])/10;
            asw = (sw[0]+sw[1]+sw[2]+sw[3]+sw[4]+sw[5]+sw[6]+sw[7]+sw[8]+sw[9])/10;

            valuesn = (">" + Float.toString(anw) + ", " + Float.toString(ane) +
                    ", " + Float.toString(ase) + ", " + Float.toString(asw) + "\n");

            text2 = valuesn;
            speed.setText(text2);


            try {
                    byte[] b = text2.getBytes();
                    if (b != null) {
                    myThreadConnected.write(b);
                        Log.i("TExt2 bytes", text2);
                        // Log.i(TAG,"SENT!!!");
                    } else {
                        Log.e("NULL", "NULL getBytes");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("string", "error");
                } finally {
                    // Log.i(TAG,"DONE!!");
                }


            mHandler.postDelayed(mWaitRunnable, 100);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ne[0]=ne[1]=ne[2]=ne[3]=ne[4]=ne[5]=ne[6]=ne[7]=ne[8]=ne[9]=0;
        nw[0]=nw[1]=nw[2]=nw[3]=nw[4]=nw[5]=nw[6]=nw[7]=nw[8]=nw[9]=0;
        se[0]=se[1]=se[2]=se[3]=se[4]=se[5]=se[6]=se[7]=se[8]=se[9]=0;
        sw[0]=sw[1]=sw[2]=sw[3]=sw[4]=sw[5]=sw[6]=sw[7]=sw[8]=sw[9]=0;
        textInfo = (TextView)findViewById(R.id.info);
        textStatus = (TextView)findViewById(R.id.status);
        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + "\n" +
                bluetoothAdapter.getAddress();
        textInfo.setText(stInfo);
        mainController = new MainController(this);
        c11 =(EditText)findViewById(R.id.editText1);
        c12 =(EditText)findViewById(R.id.editText2);
        c13 =(EditText)findViewById(R.id.editText3);
        c21 =(EditText)findViewById(R.id.editText11);
        c22 =(EditText)findViewById(R.id.editText12);
        c23 =(EditText)findViewById(R.id.editText13);
        c31 =(EditText)findViewById(R.id.editText21);
        c32 =(EditText)findViewById(R.id.editText22);
        c33 =(EditText)findViewById(R.id.editText23);
        ang = new TextView(this);
        ang = (TextView)findViewById(R.id.angles);
        speed = new TextView(this);
        speed = (TextView)findViewById(R.id.motorSpeeds);
        mHandler.postDelayed(mWaitRunnable, 100);
        calibButton = (Button)findViewById(R.id.calbutton);
        calibButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View vw) {
                mainController.posRotSensors.setCurrentStateAsZero();
            }
        });
        constButton = (Button)findViewById(constants);
        constButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View vw) {

                kp = Float.parseFloat(c11.getText().toString());
                ki = Float.parseFloat(c12.getText().toString());
                kd = Float.parseFloat(c13.getText().toString());
                 mainController.rollRegulator.setCoefficients(kp, ki, kd);
                kp = Float.parseFloat(c21.getText().toString());
                ki = Float.parseFloat(c22.getText().toString());
                kd = Float.parseFloat(c23.getText().toString());
                 mainController.pitchRegulator.setCoefficients(kp, ki, kd);
                kp = Float.parseFloat(c31.getText().toString());
                ki = Float.parseFloat(c32.getText().toString());
                kd = Float.parseFloat(c33.getText().toString());
                 mainController.yawRegulator.setCoefficients(kp, ki, kd);
            }
        });
        sendButton = (Button)findViewById(R.id.send);
        sendButton.setOnClickListener(connectListener);
        ipAddr = (EditText)findViewById(R.id.ipaddress);
        ipAddr.setText(serverIpAddress);
    }
    private Button.OnClickListener connectListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!connected) {
                if (!serverIpAddress.equals("")) {
                    sendButton.setText("Stop Streaming");
                    Thread cThread = new Thread(new ClientThread());
                    cThread.start();
                }
            }
            else{
                sendButton.setText("Start Streaming");
                connected = false;
            }
        }
    };

    public class ClientThread implements Runnable {
        public void run() {
            try {
                serverIpAddress = ipAddr.getText().toString();
                InetAddress serverAddress = InetAddress.getByName(serverIpAddress);
                socket = new Socket(serverAddress, PORT);
                connected = true;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                while (connected) {
                    powers = mainController.getMotorsPowers();
                    PosRotSensors.HeliState temp= mainController.getSensorsData();
                    out.printf("%d %d %d %d %f %f %f\n", powers.nw , powers.ne , powers.se , powers.sw,temp.roll,temp.pitch,temp.yaw);
                    out.flush();
                    Thread.sleep(150);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    connected = false;
                    sendButton.setText("Start Streaming");
                    //out.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }


        setup();
    }

    private void setup() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
            }

            pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    BluetoothDevice device =
                            (BluetoothDevice) parent.getItemAtPosition(position);
                    Toast.makeText(MainActivity.this,
                            "Name: " + device.getName() + "\n"
                                    + "Address: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();

                    textStatus.setText("start ThreadConnectBTdevice");
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                setup();
            }else{
                Toast.makeText(this,
                        "BlueTooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                startThreadConnected(bluetoothSocket);
            }else{
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    public class MyServer extends NanoHTTPD {
        private final static int PORT = 8080;

        public MyServer() throws IOException {
            super(PORT);
            start();
            System.out.println( "\nRunning! Point your browers to http://localhost:8080/ \n" );
        }

        @Override
        public Response serve(IHTTPSession session) {
            //String msg ;
          //  msg = session.getUri() ;
            HashMap<String, String> inputReceived = (HashMap<String, String>) session.getParms();
            String msg = inputReceived.get("command");
            Log.d("Data Received", msg);
            String  newmsg = msg.substring(1,5);
            boolean isnum;
            int newthrust;
            isnum = isNumeric(newmsg);
            if(isnum == true){
                newthrust = Integer.parseInt(newmsg);
                mainController.meanThrust = newthrust;
            }

            if(msg.equals("constants"))
            {
                 kp = Float.valueOf(inputReceived.get("kp"));
                 ki = Float.valueOf(inputReceived.get("ki"));
                 kd = Float.valueOf(inputReceived.get("kd"));
                mainController.rollRegulator.setCoefficients(kp, ki, kd);
                mainController.pitchRegulator.setCoefficients(kp, ki, kd);
                kp = Float.valueOf(inputReceived.get("ykp"));
                ki = Float.valueOf(inputReceived.get("yki"));
                kd = Float.valueOf(inputReceived.get("ykd"));
                mainController.yawRegulator.setCoefficients(kp, ki, kd);
            }

            if(msg.equals("reset"))
            {
                kp = 0.6f;
                ki = 0.0f;
                kd = 0.15f;
                mainController.yawRegulator.setCoefficients(kp, ki, kd);
                mainController.rollRegulator.setCoefficients(kp, ki, kd);
                mainController.pitchRegulator.setCoefficients(kp, ki, kd);
            }

            if(msg.equals("start"))

            {
                mainController.meanThrust = 30.0f;
               // mHandler.postDelayed(mWaitRunnable, 100);


                try {
                    mainController.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return newFixedLengthResponse("Start" + "</body><html>\n");
            }
            else if(msg.equals("stop"))
            {
                mainController.emergencyStop();
                return newFixedLengthResponse("Stop" + "</body></html>\n" );
            }
            if(msg.equals(" zero")){
                mainController.posRotSensors.setCurrentStateAsZero();
                return newFixedLengthResponse("Zero" + "</body><html>\n");
            }

//            else {
            {   return newFixedLengthResponse(msg + "</body></html>\n");
            }

        }
        public boolean isNumeric(String str)
        {
            try
            {
                double d = Double.parseDouble(str);
            }
            catch(NumberFormatException nfe)
            {
                return false;
            }
            return true;
        }

    }



    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    String strReceived = new String(buffer, 0, bytes);
                    final String msgReceived = String.valueOf(bytes) +
                            " bytes received:\n"
                            + strReceived;

                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            textStatus.setText(msgReceived);
                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            textStatus.setText(msgConnectionLost);
                        }});
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onResume() {
        super.onResume();
        try {
            server = new MyServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mainController.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mainController.emergencyStop();

        if(server != null) {
            server.stop();
        }
    }

    private MainController mainController;
}