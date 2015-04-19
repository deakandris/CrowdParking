package hu.bme.tmit.deakandras.crowdparking.gui;

import hu.bme.tmit.deakandras.crowdparking.R;
import hu.bme.tmit.deakandras.crowdparking.gui.map.MapFragment;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment navigationDrawerFragment;
	
	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence title;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(
				R.id.navigation_drawer);
		title = getTitle();
		
		// Set up the drawer.
		navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
	}
	
	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		switch (position) {
		case 0:
			fragmentManager.beginTransaction()
							.replace(R.id.container, new MapFragment())
							.commit();
			break;
		case 1:
			fragmentManager.beginTransaction()
							.replace(R.id.container, new SettingsFragment())
							.commit();
			break;
		}
	}
	
	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			title = getString(R.string.title_section1);
			break;
		case 2:
			title = getString(R.string.title_section2);
			break;
		case 3:
			title = getString(R.string.title_section3);
			break;
		}
	}
	
	@SuppressWarnings("deprecation")
	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(title);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!navigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		return super.onOptionsItemSelected(item);
	}
	
}
