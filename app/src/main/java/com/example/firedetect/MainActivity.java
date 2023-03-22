package com.example.firedetect;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;

import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import com.example.firedetect.util.AcceptThread;
import java.util.ArrayList;
import java.util.List;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity {
    TextView textView_temperature;
    TextView textView_smoke;
    Button btn_confirm;
    LinearLayout bluetooth_devices;
    TextView alertText;
    Switch switch_bluetooth;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();
    BluetoothDevice selectDevice = null;
    boolean isOnAlert = false;
    boolean connected = false;
    BluetoothSocket socket = null;
    AcceptThread bluetoothThread = null;
    public EditText editText_out;
    public Handler editText_out_handler;
    public Handler link_establish_handler;
    AnyChartView anyChartView;
    ArrayList<DataEntry> data = new ArrayList<>();
    com.anychart.data.Set dataSet;
    int pointCount = 1;
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

    }

    private void init () {
        mp = MediaPlayer.create(MainActivity.this, R.raw.sound);
        this.textView_temperature = this.findViewById(R.id.textView_temp);
        this.textView_smoke = this.findViewById(R.id.textView_smoke);
        this.btn_confirm = this.findViewById(R.id.btn_confirm);
        this.alertText = this.findViewById(R.id.alertText);
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View view) {

                cancelAlert();
                if (mp != null ){
                    mp.pause();
                    mp.seekTo(0);
                }


            }
        });
        this.initBluetooth();
        this.editText_out = this.findViewById(R.id.textMultiLine_out);
        Pattern pt = Pattern.compile("T[0-9]+.[0-9]+T");
        Pattern ps = Pattern.compile("S[0-9]+.[0-9]+S");

        this.editText_out_handler = new Handler(){
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message message){
                String obj = (String)(message.obj);
                //editText_out.append(obj);
                Matcher mt = pt.matcher(obj);
                boolean t_found = false;
                boolean s_found = false;
                if (mt.find()) {
                    String str = mt.group(0).substring(1, mt.group().length() - 1);
                    MainActivity.this.textView_temperature.setText("当前温度：" + str);
                    t_found = true;
                }
                Matcher ms = ps.matcher((String)(message.obj));
                if (ms.find()) {
                    String str = ms.group(0).substring(1, ms.group().length() - 1);
                    MainActivity.this.textView_smoke.setText("烟雾浓度：" + str);
                    s_found = true;
                }
                if (obj.contains("AT")) {
                    MainActivity.this.openAlert("温度过高");

                    mp.start();

                } else if (obj.contains("AS")) {
                    MainActivity.this.openAlert("烟雾浓度过高");

                    mp.start();

                } else if (obj.contains("AP")) {
                    MainActivity.this.openAlert("非法入侵");

                    mp.start();

                }
                else if (isOnAlert){
                    MainActivity.this.cancelAlert();
                    if (mp != null){
                        mp.pause();
                        mp.seekTo(0);
                    }


                }
                if (t_found && s_found) {
                    if (data.size() >= 100) {
                        data.remove(0);

                    }
                    //data.add(new CustomDataEntry("1", 10.5, 2.5));
                    data.add(new CustomDataEntry(new Integer(pointCount).toString(), Float.parseFloat(mt.group(0).substring(1, mt.group(0).length() - 1)), Float.parseFloat(ms.group(0).substring(1, ms.group().length() - 1))/20.0));
                    pointCount++;
                    //refreshChart(data);
                    dataSet.data(data);
                    //Toast.makeText(MainActivity.this, "触发" + new Integer(data.size()).toString(), Toast.LENGTH_SHORT).show();
                }

            }
        };
        this.link_establish_handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Toast.makeText(MainActivity.this, "连接建立", Toast.LENGTH_SHORT).show();
            }
        };

        data.add(new CustomDataEntry("0", 0, 0));


        dataSet = com.anychart.data.Set.instantiate();

        this.anyChartView = findViewById(R.id.any_chart_view);

        refreshChart(data);

        //data.add(new CustomDataEntry("1", 10.5, 2.5));
        //data.add(new CustomDataEntry("2", 18.5, 55.5));
        //dataSet.data(data);

    }

    private void initBluetooth() {

//        if (!bluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, RequestCode.BlUETOOTH);
//        }

        this.switch_bluetooth = this.findViewById(R.id.switch_bluetooth);
        this.switch_bluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    MainActivity.this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    Log.d("蓝牙设备", compoundButton.getText().toString());
                    if (bluetoothAdapter == null) {
                        Toast.makeText(MainActivity.this, "本机未找到蓝牙功能", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    bluetoothAdapter.enable();
//                    BluetoothDevice device = null;
//                    for (BluetoothDevice d:
//                            bluetoothAdapter.getBondedDevices()
//                         ) {
//                        if (d.getName().equals("VELVET")){
//                            device = d;
//                        }
//                    }


                    showDevices();
                } else {
                    //bluetoothAdapter.disable();
                    //MainActivity.this.devices.clear();
                    MainActivity.this.bluetooth_devices.removeAllViews();
                    MainActivity.this.bluetoothThread.cancel();

                }
            }
        });

        this.bluetooth_devices = (LinearLayout) findViewById(R.id.devices);
    }


    private void showDevices() {

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Set<BluetoothDevice> pair = bluetoothAdapter.getBondedDevices();
        Log.d("设备", Integer.toString(pair.size()));
        Log.d("蓝牙", Integer.toString(pairedDevices.size()));
        //ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item);

        if (pairedDevices.size() > 0) {
            Log.d("Device", Integer.toString(pairedDevices.size()));

            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                this.devices.add(device);

                //for (int i = 0; i < 4; i++) {
                    TextView textView =(TextView)getLayoutInflater().inflate(R.layout.bluetooth_name, null);
                    textView.setText(deviceName);

                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(MainActivity.this, deviceName + ":" + deviceHardwareAddress, Toast.LENGTH_SHORT).show();
                            MainActivity.this.bluetooth_devices.removeAllViews();
                            selectDevice = device;
                            MainActivity.this.bluetoothThread = new AcceptThread(MainActivity.this, selectDevice);
                            MainActivity.this.bluetoothThread.start();

                        }
                    });
                    //spinnerArrayAdapter.add(deviceName);
                    bluetooth_devices.addView(textView);
                //s}

            }

            //spinnerArrayAdapter.notifyDataSetChanged();


        }
    }



    @SuppressLint("SetTextI18n")
    private void openAlert(String str) {
       alertText.setText(str + "警报");
       alertText.setTextColor(Color.parseColor("#F3216A"));
       isOnAlert = true;
       btn_confirm.setEnabled(true);
    }
    private void cancelAlert() {
        isOnAlert = false;
        Toast.makeText(MainActivity.this, "已经取消警报", Toast.LENGTH_SHORT).show();
        alertText.setText("当前无警报");
        alertText.setTextColor(Color.parseColor("#2196F3"));
        btn_confirm.setEnabled(false);
    }


    private void refreshChart(List<DataEntry> data){
//        anyChartView.clear();
        //anyChartView.setProgressBar(findViewById(R.id.progress_bar));

        //anyChartView.setProgressBar(findViewById(R.id.progress_bar));

        Cartesian cartesian = AnyChart.line();

        //cartesian.animation(true);

        cartesian.padding(10d, 20d, 5d, 20d);

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                // TODO ystroke
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        //cartesian.title("变化图");

        //cartesian.yAxis(0).title("值");
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        List<DataEntry> seriesData = new ArrayList<>();
        Log.d("fine", Integer.toString(data.size()));
        int size = data.size();
//        for (int i = 0; i < size; i++) {
//            //seriesData.add(new CustomDataEntry(Integer.toString(i + 1000),11.2, 12.0+i));
//            seriesData.add(new CustomDataEntry("2000" + i, 14.8, 13.5, 5.4));
//        }
        for (DataEntry e:
                data
             ) {
            seriesData.add(e);
        }


        dataSet.data(seriesData);
        Mapping series1Mapping = dataSet.mapAs("{ x: 'x', value: 'value' }");
        Mapping series2Mapping = dataSet.mapAs("{ x: 'x', value: 'value2' }");


        Line series1 = cartesian.line(series1Mapping);
        series1.name("温度");
        series1.color("#F3216A");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line series2 = cartesian.line(series2Mapping);
        series2.name("烟雾");
        series2.hovered().markers().enabled(true);
        series2.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series2.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);


        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);


        anyChartView.setChart(cartesian);

    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);
            setValue("value2", value2);
            setValue("value3", value3);
        }
        CustomDataEntry(String x, Number value, Number value2) {
            super(x, value);
            setValue("value2", value2);
        }

    }
}