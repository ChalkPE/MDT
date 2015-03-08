package com.mcpekorea.mdt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    public static final File ROOT_DIRECTORY = new File(Environment.getExternalStorageDirectory(), "MDT");
    public static final File PROJECTS_DIRECTORY = new File(ROOT_DIRECTORY, "projects");
    public static final File EXPORT_DIRECTORY = new File(ROOT_DIRECTORY, "export");

    private ListView listView;
	private WorkspaceAdapter adapter;
    private ArrayList<Project> projects;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean succeed = false;

        if(!ROOT_DIRECTORY.exists()) {
            succeed = ROOT_DIRECTORY.mkdirs();
        }

        if(!PROJECTS_DIRECTORY.exists()) {
            succeed = succeed || PROJECTS_DIRECTORY.mkdirs();
        }

        if(!EXPORT_DIRECTORY.exists()) {
            succeed = succeed || EXPORT_DIRECTORY.mkdirs();
        }

        try {
            //.mod isn't video file
            succeed = succeed || new File(ROOT_DIRECTORY, ".nomedia").createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }

        if(succeed){
            Log.d(getText(R.string.app_name).toString(), "Directories are created!");
        }

        findViewById(R.id.main_fab_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), CreateProjectActivity.class), 0);
            }
        });

        listView = (ListView) findViewById(R.id.main_list);

        File[] projectFiles = PROJECTS_DIRECTORY.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.toLowerCase().endsWith(".json");
            }
        });

        if(projectFiles == null || projectFiles.length == 0){
            projects = new ArrayList<>();
        }else{
            projects = new ArrayList<>(projectFiles.length);
            for(File file : projectFiles){
                try{
                    projects.add(Project.createFromJSON(new FileInputStream(file)));
                }catch(FileNotFoundException e){
                    e.printStackTrace();
                }
            }
        }

	    adapter = new WorkspaceAdapter(this, projects);
        listView.setAdapter(adapter);

	    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    @Override
		    public void onItemClick(AdapterView<?> a, View v, final int position, long l) {
				final Project project = projects.get(position);
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(project.getName())
				.setNegativeButton(android.R.string.cancel, null)
				.setNeutralButton(R.string.delete_project, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface d, int i){
						new AlertDialog.Builder(MainActivity.this)
						.setTitle(R.string.confirm_delete_title)
						.setMessage(String.format(getResources().getString(R.string.confirm_delete_message), project.getName()))
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface d, int i){
								projects.remove(position);
								adapter.notifyDataSetChanged();
								File file = new File(PROJECTS_DIRECTORY, project.getName()+".json");
								if(file.exists()){
									file.delete();
								}
								Toast.makeText(MainActivity.this, String.format(getResources().getString(R.string.deleted_message), project.getName()), Toast.LENGTH_LONG).show();
							}
						})
						.setNegativeButton(android.R.string.cancel, null).show();
					}
				})
				.setPositiveButton(R.string.open_project, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface d, int i){
						Intent intent = new Intent(MainActivity.this, PatchEditorActivity.class);
						intent.putExtra("project", project);
						startActivity(intent);
					}
				}).show();
		    }
	    });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == R.id.menu_settings){
            Intent i = new Intent(this, SettingActivity.class);
	        startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && resultCode == RESULT_OK){
            Project project = new Project(data.getStringExtra("projectName"), data.getStringExtra("authorName"));

            adapter.addProject(project);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(Project project : projects){
            File projectFile = new File(PROJECTS_DIRECTORY, project.getName() + ".json");
            try{
                BufferedWriter bw = new BufferedWriter(new FileWriter(projectFile));
                bw.write(project.toJSON().toString());
	            bw.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}