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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

import static android.R.attr.port;
import static android.provider.ContactsContract.CommonDataKinds.Website.URL;
import com.example.dell.sensor_data_send.MainController;

public class MainActivity extends Activity
{

    private static final int REQUEST_ENABLE_BT = 1;


    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    TextView ang, speed;
    Button calibButton, constButton;

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
    private String text1, text2;
    float kp, ki, kd;
    EditText c11, c12, c13, c21, c22, c23, c31, c32, c33;

    private Handler mHandler = new Handler();

    private Runnable mWaitRunnable = new Runnable() {
        public void run() {
            text1 = PosRotSensors.text;
            ang.setText(text1);
            text2 = MainController.values;
            speed.setText(text2);

            try {
                byte[] b = text2.getBytes();
                if(b != null){
                    myThreadConnected.write(b);
                    Log.i("TExt2 bytes" , text2);
                   // Log.i(TAG,"SENT!!!");
                }else{
                    Log.e("NULL","NULL getBytes");
                }
            }catch( Exception e)
            {
                e.printStackTrace();
                Log.e("string","error");
            }finally{
               // Log.i(TAG,"DONE!!");
            }

            mHandler.postDelayed(mWaitRunnable, 100);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        ang.setText(text1);
        speed.setText(text2);
        mHandler.postDelayed(mWaitRunnable, 100);
        calibButton = (Button)findViewById(R.id.calbutton);
        calibButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View vw) {
                mainController.posRotSensors.setCurrentStateAsZero();
            }
        });
        constButton = (Button)findViewById(R.id.constants);
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
            String msg = "<html><body><h1>Command : </h1>\n";
            msg += "<p>" + session.getUri() + " !</p>";
            if(msg.equals("stop"))
            {
                return newFixedLengthResponse(msg + "stop" + "</body></html>\n" );
            }
            else {


                return newFixedLengthResponse(msg + "</body></html>\n");
            }
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
        mainController.stop();

        if(server != null) {
            server.stop();
        }
    }

    private MainController mainController;
}