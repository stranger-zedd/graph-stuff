package com.godai.graphstuff;

import java.util.Date;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.joda.time.DateTime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.godai.graphstuff.data.AppData;
import com.godai.graphstuff.data.Person;
import com.godai.graphstuff.data.repositories.PersonRepository;
import com.godai.graphstuff.data.repositories.SMSRepository;

public class MainActivity extends Activity {

	private static final int CONTACT_PICKER_RESULT = 1001; 
	
	TextView text;
	SMSRepository repository;
	GraphicalView chart;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_main);
        
        AppData.intialise((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE), getResources());
                
        text = (TextView) findViewById(R.id.text_field_one);
        getResources();
        
        text.setText("Please select a contact");
        
        repository = new SMSRepository(getContentResolver());
        
    }
    
    public void launchContactPicker(View view) {
    	
    	Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
    	startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	if(resultCode == RESULT_OK) {
    		if(requestCode == CONTACT_PICKER_RESULT) {
    			int id = Integer.parseInt(data.getData().getLastPathSegment());
    			Person person = PersonRepository.getContactByID(getContentResolver(), id);
    			
    			if(person == null || person.phone() == null)
    				text.setText("No phone number recorded");
    			else {
    				
    				repository.getAllMessagesFromAndToContact(person);
    		        LinearLayout view = (LinearLayout) findViewById(R.id.scroll_view_one);
    		        Map<Date, Integer> messagesPerDay = repository.getMessageCountsForDates(person);
    		        TimeSeries series = new TimeSeries("Amount of Messages vs date");
    		        
    		        TimeSeries aveSeries = new TimeSeries("Rolling average of Messages vs date");
    		        int RollingMessageSum =0;
    		        int RollingMessageCount =0;
    		        
    		        DateTime today = DateTime.now();
    		        DateTime date = new DateTime(messagesPerDay.keySet().iterator().next());
    		        
    		        
    		        while(date.compareTo(today) < 0) {
    		        	if(messagesPerDay.containsKey(date.toDate())){
    		        		series.add(date.toDate(), messagesPerDay.get(date.toDate()));
    		        		RollingMessageSum +=  messagesPerDay.get(date.toDate());  
    		        	}else{
    		        		series.add(date.toDate(), 0);
    		        	}
    		        	
    		        	RollingMessageCount++;
    		        	aveSeries.add(date.toDate(), (RollingMessageSum/RollingMessageCount));
    		        	
    		        	date = date.plusDays(1);
    		        	Log.d("DATE", date.toString());
    		        }	
    		        
    		        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    		        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    		        
    		        dataset.addSeries(series);		        
    		        dataset.addSeries(aveSeries);
    		        
    		        renderer.addSeriesRenderer(new XYSeriesRenderer());
    		        
    		        XYSeriesRenderer x = new XYSeriesRenderer();
    		        x.setColor(Color.RED);
    		        renderer.addSeriesRenderer(x);
    		        
    		        
					chart = ChartFactory.getTimeChartView(this, dataset, renderer, "dd/MM/yyyy");
					
					view.removeAllViews();
					view.addView(chart, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
   		        
    			}
    		}
    	}
    	else {
    		Log.e("CPICKER", "Contact picker did not return OK");
    	}
    	
    }

    public void onResume() {    	
    	
    	if(chart != null)
    		chart.repaint();
    	
    	super.onResume();

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
