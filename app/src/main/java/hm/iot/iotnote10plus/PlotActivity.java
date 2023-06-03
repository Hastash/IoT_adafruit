package hm.iot.iotnote10plus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class PlotActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    Button btnBack;
    ImageView startDateBtn, endDateBtn, startTimeBtn, endTimeBtn;
    EditText startDate, endDate, startTime, endTime;
    CheckBox cbIntensity, cbTemperature, cbHumidity;
    // Get the chart view from the layout
    LineChart lineChartIntensity;
    Button generate;
    String username = "trongthuyen";
    ArrayList<String> feedKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        startDate = findViewById(R.id.startDate);
        startDateBtn = findViewById(R.id.startDateBtn);
        endDate = findViewById(R.id.endDate);
        endDateBtn = findViewById(R.id.endDateBtn);

        startTime = findViewById(R.id.startTime);
        startTimeBtn = findViewById(R.id.startTimeBtn);
        endTime = findViewById(R.id.endTime);
        endTimeBtn = findViewById(R.id.endTimeBtn);

        cbIntensity = findViewById(R.id.intensityADC);

        generate = findViewById(R.id.generate);
        generate.setOnClickListener(v -> getChart());

        lineChartIntensity = findViewById(R.id.lineChartIntensity);

        lineChartIntensity.setVisibility(View.GONE);

        DateUpdate(startDate);
        DateUpdate(endDate);
        startTime.setText("00:00:00");
        TimeUpdate(endTime);

        cbIntensity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    feedKeys.add("bbc-intensity");
                    lineChartIntensity.setVisibility(View.VISIBLE);
                } else {
                    feedKeys.remove("bbc-intensity");
                    lineChartIntensity.setVisibility(View.GONE);
                }
                getChart();
            }
        });

        cbIntensity.setChecked(getIntent().getBooleanExtra("cbIntensity",false));

        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PlotActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
        startDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePicker(startDate);
            }
        });
        endDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePicker(endDate);
            }
        });
        startTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePicker(startTime);
            }
        });
        endTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePicker(endTime);
            }
        });
        startMQTT();
    }

    private void DatePicker(final EditText textview){
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(PlotActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                //i: year - i1: month - i2: dayOfMonth
                calendar.set(i, i1, i2);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                textview.setText(simpleDateFormat.format(calendar.getTime()));
            }
        },year,month,dayOfMonth);
        datePickerDialog.setTitle("SELECT DATE");
        datePickerDialog.show();
    }
    private void DateUpdate(final EditText textview){
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, dayOfMonth);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        textview.setText(simpleDateFormat.format(calendar.getTime()));
    }
    private  void TimePicker(final EditText textview){
        int style = AlertDialog.THEME_HOLO_DARK;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(PlotActivity.this, style, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                calendar.set(0,0,0, i, i1,0);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                textview.setText(simpleDateFormat.format(calendar.getTime()));
            }
        }, hour, minute, true);
        timePickerDialog.setTitle("SELECT TIME");
        timePickerDialog.show();
    }
    private void TimeUpdate(final EditText textview){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.set(0,0,0, hour, minute,0);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        textview.setText(simpleDateFormat.format(calendar.getTime()));
    }

    public void getChart(){
        RequestQueue queue = Volley.newRequestQueue(PlotActivity.this);
        lineChartIntensity.clear();
        for (String feedKey : feedKeys) {
            String baseUrl = "https://io.adafruit.com/api/v2/{username}/feeds/{feedKey}/data/chart";
            String url = baseUrl.replace("{username}", username).replace("{feedKey}", feedKey);
            url += "?start_time=" + modifyAdafruitDate(startDate) + "T" + startTime.getText().toString() + "Z";
            url += "&" + "end_time=" + modifyAdafruitDate(endDate) + "T" + endTime.getText().toString() + "Z";
            String finalUrl = url;
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    JSONArray data;
                    List<String> dates = new ArrayList<>();
                    List<String> values = new ArrayList<>();
                    try {
                        data = response.getJSONArray("data");
                        int n = data.length();
                        String date, value;
                        for (int i = 0; i < n; i++) {
                            JSONArray innerData = data.getJSONArray(i);
                            date = innerData.getString(0);
                            value = innerData.getString(1);
                            dates.add(date);
                            values.add(value);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    switch (feedKey) {
                        case "bbc-intensity":
                            drawLineChart(dates, values, lineChartIntensity);
                            break;
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO: Handle error
                    Toast.makeText(PlotActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
            queue.add(request);
        }
    }
    private void drawLineChart(List<String> dates, List<String> values, LineChart chart){
        // Create a list of data entries
        List<Entry> entries = new ArrayList<>();
        try {
            for (int i = 0; i < dates.size(); i++) {
                String dateString = dates.get(i);
                float value = Float.parseFloat(values.get(i));
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Date date = inputFormat.parse(dateString);
                long timestamp = date.getTime();
                entries.add(new Entry(timestamp, value));
            }

            // Create a data set from the data entries
            LineDataSet dataSet = new LineDataSet(entries, "Label");

            // Format the x-axis to show dates
            chart.getXAxis().setValueFormatter(new AutoScaleXAxisValueFormatter());

            // Add the data set to the chart and customize as desired
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.getDescription().setEnabled(false);
            chart.getLegend().setEnabled(false);
            chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            chart.getXAxis().setLabelRotationAngle(60);
            chart.invalidate();

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    // Define a custom value formatter for the x-axis to show dates
    public static class AutoScaleXAxisValueFormatter extends ValueFormatter {
        private final SimpleDateFormat secondFormat = new SimpleDateFormat("mm:ss");
        private final SimpleDateFormat minuteFormat = new SimpleDateFormat("HH:mm:ss");
        private final SimpleDateFormat hourFormat = new SimpleDateFormat("dd/MM HH:mm");
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        private float previousLowestVisibleX = Float.MIN_VALUE;
        private float previousHighestVisibleX = Float.MAX_VALUE;
        private float previousVisibleXRange = 0;

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            if (axis instanceof XAxis) {
                XAxis xAxis = (XAxis) axis;
                float visibleXRange = xAxis.mAxisRange / (1000 * 60);

                if (visibleXRange != previousVisibleXRange ||
                        xAxis.getAxisMinimum() < previousLowestVisibleX ||
                        xAxis.getAxisMaximum() > previousHighestVisibleX) {

                    previousVisibleXRange = visibleXRange;
                    previousLowestVisibleX = (long) xAxis.getAxisMaximum();
                    previousHighestVisibleX = (long) xAxis.getAxisMinimum();

                    if (visibleXRange < 1) {
                        // Show seconds when zoomed in to 1 minute or less
                        return secondFormat.format(new Date((long) value));
                    } else if (visibleXRange >= 1 && visibleXRange < 60) {
                        // Show hours when zoomed out to 1 hour or less
                        return minuteFormat.format(new Date((long) value));
                    } else if (visibleXRange >= 60 && visibleXRange < 1440) {
                        // Show date when zoomed out to more than 1 day
                        return hourFormat.format(new Date((long) value));
                    } else if (visibleXRange >= 1440 && visibleXRange < 40320) {
                        // Show date when zoomed out to more than 1 day
                        return dateFormat.format(new Date((long) value));
                    }
                }
            }
            return super.getAxisLabel(value, axis);
        }

    }

    private String modifyAdafruitDate(EditText texView) {
        String adafruitDate = null;
        // perform some modifications on the input string
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = inputFormat.parse(texView.getText().toString());
            adafruitDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return adafruitDate;
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
                    getChart();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

}
