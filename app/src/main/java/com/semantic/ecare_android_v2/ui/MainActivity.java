package com.semantic.ecare_android_v2.ui;


import java.util.ArrayList;

import com.semantic.ecare_android_v2.Fragments.AproposFragment;
import com.semantic.ecare_android_v2.Fragments.DashboardFragment;
import com.semantic.ecare_android_v2.Fragments.PatientsFragment;
import com.semantic.ecare_android_v2.R;
import com.semantic.ecare_android_v2.core.BuilderPatientList;
import com.semantic.ecare_android_v2.object.CompoundMeasure;
import com.semantic.ecare_android_v2.object.ConfigurationList;
import com.semantic.ecare_android_v2.object.Patient;
import com.semantic.ecare_android_v2.ui.common.activity.GenericDisconnectedActivity;
import com.semantic.ecare_android_v2.ui.common.adapter.PatientListAdapter;
import com.semantic.ecare_android_v2.util.Constants;
import com.semantic.ecare_android_v2.util.Functions;

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import net.newel.android.Log;

import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends GenericDisconnectedActivity {
	private final int DIALOG_EXIT=1;
	
	//private Spinner spinner;
	private String CLASSNAME=this.getClass().getName();
	private AlertDialog dialog=null;
	
	private BuilderPatientList patientListBuilder=null;
	private static ListView listview;
	private static boolean firstTime = true;
	private static PatientListAdapter patientListAdapter;
	
    public boolean isFirstTime() {
		return firstTime;
	}

	public void setFirstTime(boolean firstTime) {
		this.firstTime = firstTime;
	}


	FragmentManager fragmentManager;
	FragmentTransaction fragmentTransaction;


	private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
			= new BottomNavigationView.OnNavigationItemSelectedListener() {

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.nav_dashboard:
					fragmentTransaction = getSupportFragmentManager().beginTransaction();
					fragmentTransaction.replace(R.id.drawer_layout, new DashboardFragment(), "Dashboard");
					fragmentTransaction.commit();
					return true;
				case R.id.nav_patients:
					fragmentTransaction = getSupportFragmentManager().beginTransaction();
					fragmentTransaction.replace(R.id.drawer_layout, new PatientsFragment(), "Patients");
					fragmentTransaction.commit();
					return true;
				case R.id.rootElement:
					Intent intentAbout = new Intent(MainActivity.this, AboutActivity.class);
					startActivity(intentAbout);
					finish();
					return true;
			}
			return false;
		}

	};
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(Constants.TAG, CLASSNAME+" Oncreate Class rv");

		BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
		// initiation du fragment dashboard par defaut au demarrage de l'application
		fragmentManager = getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		DashboardFragment dash = new DashboardFragment();
		fragmentTransaction.add(R.id.drawer_layout, dash, "Dashboard");
		fragmentTransaction.commit();
		// end
        //Button button=(Button) findViewById(R.id.button1);   
    }
	
	@Override
	protected void affichage_before_binding() {
        setContentView(R.layout.activity_main);
	}
    
    @Override
    protected void affichage(){
    	super.affichage();
    	Log.i(Constants.TAG, CLASSNAME +  "affichage appelleeee");
		if(isFirstTime())
		{
			setFirstTime(false);
        	Log.i(Constants.TAG, CLASSNAME +  "first time main activity");
		}
		else
		{
			if(mBoundService.isNeedToUpdate())
			{
			//	mBoundService.installUpdate();
				mBoundService.setNeedToUpdate(false);
				Log.i(Constants.TAG, CLASSNAME + "Need to install the app");
			}
			else
			{
				Log.i(Constants.TAG, CLASSNAME + "No need to install the app");
			//	mBoundService.checkUpdate();
			}
		}

    	//FIXME : Attention : si l'appli ne peut pas récupérer d'infos,
    	//et qu'on met une autre appli à la place, 
    	//et qu'on réaffiche l'appli en utilisant l'icone de notification
    	// => ALORS BUG ICI "config" est NUL !!!
    	if(mBoundService.getLaunchComplete()!=false){
	    	TextView tvInfo = (TextView) findViewById(R.id.infos);
	    	if(mBoundService!=null){
		    	ConfigurationList config = mBoundService.getConfigurationList();
		    	
		    		try {
		    			tvInfo.setText(Html.fromHtml(config.getBlocInfo()));
		    		} catch (Exception e) {
						e.printStackTrace();
					}
		    	
	    	}else{
	    		tvInfo.setVisibility(View.GONE);
	    	}
	
	    	
		    //Check if patient is already "connected" and go to patient panel
		    Patient selectedPatient = mBoundService.getSelectedPatient();
		    if(selectedPatient!=null){
				Intent i = new Intent(getApplicationContext(), WaitActivity.class);
				startActivity(i);
				finish();
		    }else{		    
		
		    	
		    	//Getting patient list from the model (= ServiceEcare) once binded
		    	Log.i(Constants.TAG,CLASSNAME+" display "+mBoundService.getPatientList().size()+" patient(s)");
		    		    	
		    	listview = (ListView) findViewById(R.id.listViewPatients);

		    	listview.setAdapter(new PatientListAdapter(getApplicationContext(), mBoundService.getPatientList()));
		        listview.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
						//selecting Patient
						int selectedIndex = position;
						Patient selectedPatient = mBoundService.getPatientList().get(selectedIndex);
						mBoundService.setSelectedPatient(selectedPatient);
						Log.i(Constants.TAG, CLASSNAME + "selectedPatient : " +selectedPatient.getUid());
						
					    Intent intent = new Intent(getApplicationContext(), WaitActivity.class);
					    startActivity(intent);
					    finish();
					}
				});
		    }
    	}else{
    		//Chargement de l'application pas effectuée !
    		//renvoi vers splashscreen
			Intent i = new Intent(getApplicationContext(), SplashScreen.class);
			startActivity(i);
			finish();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem itemTest = menu.findItem(R.id.mode_test);
		if(mBoundService.isSaveMeasure())
		{
			itemTest.setIcon(null);
		}
		else
		{
			itemTest.setIcon(R.drawable.check_mode_test);
		}
		
		return true;
	}
    
    
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mode_test:
			if(mBoundService.isSaveMeasure())
			{
				mBoundService.setSaveMeasure(false);
				item.setIcon(R.drawable.check_mode_test);
			}
			else
			{
				mBoundService.setSaveMeasure(true);
				item.setIcon(null);
			}
			break;
		    
		case R.id.menu_about:
		    Intent intentAbout = new Intent(this, AboutActivity.class);
		    startActivity(intentAbout);
		    finish();
		    break;
		case R.id.view_all_notes:
			viewAllNotesDialog();
			break;
		}	
		return true;
	}


	public void viewAllNotesDialog(){
		Intent intent = new Intent(this, AllNotesActivity.class);
		startActivity(intent);
	}
	
	@Override
	protected void newMeasureReceived(ArrayList<CompoundMeasure> mesures){
		//new measure comes without selected patient
		Log.i(Constants.TAG, CLASSNAME+" Reception d'une mesure");
		Intent i = null;
		if(mBoundService.isSaveMeasure())
		{
			i = new Intent(getApplicationContext(), MeasureSetPatientActivity.class);
			i.putExtra("mesures", mesures);
			startActivity(i);
			this.finish();
		}
		else
		{
			Log.i(Constants.TAG, CLASSNAME +  "ok");
			i = new Intent(getApplicationContext(), MeasureActivity.class);
			i.putExtra("mesures", mesures);
			startActivity(i);
			this.finish();
		}
	}
	
    
	
	private void reboot(){
		Log.i(Constants.TAG, CLASSNAME+" Stop app and reboot");
		
		//FIXME fonctionne pas
		Functions.execCommand("reboot");
		       
	}


	@Override
	protected void onKeyBack() {
		Intent iw = null;
		iw = new Intent(getApplicationContext(), MainActivity.class);
		startActivity(iw);
		finish();
	}


	
	@Override
	protected Dialog onCreateDialog(final int id) {
		switch(id){
			case DIALOG_EXIT:
			    Builder builder = new AlertDialog.Builder(this);
			    builder.setTitle(getResources().getString(R.string.dialog_reboot_title));
			    builder.setMessage(getResources().getString(R.string.dialog_reboot_content));
			    builder.setCancelable(true);
			    builder.setPositiveButton(getResources().getString(R.string.dialog_reboot_yes), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						reboot();
					}
			    	  
			    });
			    builder.setNegativeButton(getResources().getString(R.string.dialog_reboot_no), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {}
			    });
			    dialog = builder.create();
			    dialog.show();
				break;
				
		}
		return null;
	}
	
	@Override
	public void onResume(){
		Log.i(Constants.TAG,CLASSNAME+" OnResume");		
		super.onResume();
		if(mBoundService!=null)
		{
	        Log.i(Constants.TAG, CLASSNAME +  "bound");
			mBoundService.addListener(this);
		}
	}
	


}
