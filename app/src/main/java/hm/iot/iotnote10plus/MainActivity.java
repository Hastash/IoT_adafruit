package hm.iot.iotnote10plus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    MQTTHelper  mqttHelper;
    TextView    txtIntensity;
    LabeledSwitch btnLED;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtIntensity = findViewById(R.id.txtIntensity);
        btnLED = findViewById(R.id.btnLED);

        txtIntensity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,PlotActivity.class);
                intent.putExtra("cbIntensity", true);
                startActivity(intent);
            }
        });

        btnLED.setOnToggledListener((toggleableView, isOn) -> {
            btnLED.setEnabled(false);
            if(isOn){
                sendDataMQTT("trongthuyen/feeds/bbc-led", "1",3);
            } else{
                sendDataMQTT("trongthuyen/feeds/bbc-led", "0",3);
            }
        });

        startMQTT();
    }
    public void sendDataMQTT(String topic, String value, int numberOfRetries){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(false);
        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        msg.setPayload(b);


        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        if(topic.equals("trongthuyen/feeds/bbc-led")){
            Timer timer = new Timer();
            int remainingRetries = numberOfRetries - 1;
            boolean remainingValue = Integer.parseInt(value) != 0;
            // Check if the button is still disabled (meaning the ACK payload was not received)
            if (!btnLED.isEnabled()) {
                // Wait for the ACK payload for up to 3 seconds
                timer.schedule(new TimerTask() {
                    // Decrement the retry count
                    public void run() {
                        // If there are remaining retries, resend the message
                        if (remainingRetries > 0) {
                            sendDataMQTT("trongthuyen/feeds/bbc-led", remainingValue ? "1" : "0", remainingRetries);
                        }
                        else {
                            // If there are no more retries, revert the button state and re-enable it
                            btnLED.setOn(!btnLED.isOn());
                            btnLED.setEnabled(true);
                            //Testcase: When ACK have not been returned, revert the button on server (In case of virtual device)
//                            sendDataMQTT("trongthuyen/feeds/bbc-led", !remainingValue ? "1" : "0", remainingRetries);
                        }
                    }
                },3000);
            } else {
                timer.cancel();
            }
        }
    }

    public void startMQTT(){
        mqttHelper = new MQTTHelper(this);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d("TEST", topic + "***" + message.toString());
                if (topic.contains("bbc-intensity")) {
                    String text = message.toString() + "Lux";
                    txtIntensity.setText(text);
                }
                else if (topic.contains("bbc-ack")){
                    if (!btnLED.isEnabled()) {
                        if (message.toString().equals("L1")) {
                            btnLED.setEnabled(true);
                        } else {
                            btnLED.setOn(!btnLED.isOn());
                        }
                        btnLED.setEnabled(true);
                    }
                }
                else if (topic.contains("trongthuyen/feeds/bbc-led")){
                    if(message.toString().equals("1")){
                        btnLED.setOn(true);
                    } else {
                        btnLED.setOn(false);
                    }
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}