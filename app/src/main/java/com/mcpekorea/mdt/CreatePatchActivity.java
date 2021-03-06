package com.mcpekorea.mdt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mcpekorea.hangul.Hangul;
import com.mcpekorea.peanalyzer.Line;
import com.mcpekorea.peanalyzer.UnsignedInteger;
import com.mcpekorea.ptpatch.Offset;
import com.mcpekorea.ptpatch.Value;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class CreatePatchActivity extends ActionBarActivity implements TextWatcher {
    public static Value DEFAULT_VALUE;

    private int patchIndex;
    private EditText offsetArea, valueAreaHex, valueAreaString, memoArea;
    private CheckBox isExcludedBox;
    private TextView informationText;

    private String oldOffsetString = "", oldValueString = "", oldMemo = "";
    private boolean oldIsExcluded = false;

    private int currentValueType = R.id.create_patch_value_type_hex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_patch);

	    DEFAULT_VALUE = new Value(PreferenceManager.getDefaultSharedPreferences(this).getString("defaultValue", "7047"));

        offsetArea = (EditText) findViewById(R.id.create_patch_offset);
        valueAreaHex = (EditText) findViewById(R.id.create_patch_value_hex);
        valueAreaString = (EditText) findViewById(R.id.create_patch_value_string);
        memoArea = (EditText) findViewById(R.id.create_patch_memo);
        isExcludedBox = (CheckBox) findViewById(R.id.create_patch_is_excluded);
        informationText = (TextView) findViewById(R.id.create_patch_information);

        offsetArea.setTypeface(WorkspaceActivity.inconsolata);
        valueAreaHex.setTypeface(WorkspaceActivity.inconsolata);
        valueAreaString.setTypeface(WorkspaceActivity.inconsolata);
        informationText.setTypeface(WorkspaceActivity.inconsolata);

        offsetArea.addTextChangedListener(this);

        ((RadioGroup) findViewById(R.id.create_patch_value_type_group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(currentValueType != checkedId){
                    switch (checkedId){
                        case R.id.create_patch_value_type_hex:
                            try{
                                valueAreaHex.setText(new Value(valueAreaString.getText().toString().getBytes("UTF-8")).toString());
                            }catch(UnsupportedEncodingException e){
                                e.printStackTrace();
                            }
                            valueAreaHex.setVisibility(View.VISIBLE);
                            valueAreaString.setVisibility(View.INVISIBLE);
                            break;
                        case R.id.create_patch_value_type_unicode:
                            try{
                                valueAreaString.setText(new String(new Value(valueAreaHex.getText().toString()).getBytes(), "UTF-8"));
                            }catch(UnsupportedEncodingException e){
                                e.printStackTrace();
                            }
                            valueAreaString.setVisibility(View.VISIBLE);
                            valueAreaHex.setVisibility(View.INVISIBLE);
                            break;
                    }
                    currentValueType = checkedId;
                }
            }
        });

        Bundle bundle = getIntent().getExtras();
        patchIndex = bundle.getInt("patchIndex", -1);

        if(patchIndex >= 0){ //Edit mode
            setTitle(R.string.title_activity_edit_patch);

            oldOffsetString = bundle.getString("offsetString");
            offsetArea.setText(oldOffsetString);

            oldValueString = bundle.getString("valueString");
            valueAreaHex.setText(oldValueString);

            oldMemo = bundle.getString("memo");
            memoArea.setText(oldMemo);

            oldIsExcluded = bundle.getBoolean("isExcluded", false);
            isExcludedBox.setChecked(oldIsExcluded);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate((patchIndex >= 0) ? R.menu.menu_edit_patch : R.menu.menu_create_patch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_save:
                String offsetString = offsetArea.getText().toString();
                String memo = memoArea.getText().toString();

                if(offsetString == null || offsetString.equals("")){
                    offsetArea.setError(Hangul.format(getText(R.string.error_empty).toString(), getText(R.string.create_patch_offset).toString()));
                    return true;
                }

                if(memo == null || memo.equals("")){
                    memo = "";
                }

                Value value = getValue();
                if(value == null || Arrays.equals(value.getBytes(), Value.BLANK)){
                    value = DEFAULT_VALUE;
                }

                Offset offset = new Offset(offsetString);

                Bundle bundle = new Bundle();
                bundle.putInt("patchIndex", patchIndex);
                bundle.putByteArray("offsetBytes", offset.getBytes());
                bundle.putByteArray("valueBytes", value.getBytes());
                bundle.putString("memo", memo.trim());
                bundle.putBoolean("isExcluded", isExcludedBox.isChecked());
                bundle.putBoolean("deleted", false);

                Intent intent = new Intent();
                intent.putExtras(bundle);

                setResult(RESULT_OK, intent);
                finish();
                return true;

            case R.id.menu_delete:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_confirm_delete)
                        .setMessage(R.string.dialog_message_confirm_patch_delete)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int i) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("patchIndex", patchIndex);
                                bundle.putBoolean("deleted", true);

                                Intent intent = new Intent();
                                intent.putExtras(bundle);

                                setResult(RESULT_OK, intent);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;

            case R.id.menu_cancel:
	            showCancelDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {
			showCancelDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public Value getValue(){
        switch(currentValueType){
            case R.id.create_patch_value_type_hex:
                return new Value(valueAreaHex.getText().toString());
            case R.id.create_patch_value_type_unicode:
                try{
                    return new Value(valueAreaString.getText().toString().getBytes("UTF-8"));
                }catch (UnsupportedEncodingException e){
                    e.printStackTrace();
                    return null;
                }
            default:
                return null;
        }
    }

    public EditText getCurrentValueArea(){
        switch(currentValueType){
            case R.id.create_patch_value_type_hex:
                return valueAreaHex;
            case R.id.create_patch_value_type_unicode:
                return valueAreaString;
            default:
                return null;
        }
    }

    public void showCancelDialog(){
        if(oldOffsetString.equalsIgnoreCase(offsetArea.getText().toString()) &&
                oldValueString.equalsIgnoreCase(getCurrentValueArea().getText().toString()) &&
                oldMemo.equals(memoArea.getText().toString()) &&
                oldIsExcluded == isExcludedBox.isChecked()){
            setResult(RESULT_CANCELED);
            finish();
        }else{
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_close)
                    .setIcon(R.drawable.ic_clear_black_48dp)
                    .setMessage(R.string.dialog_message_close_without_saving)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after){

    }

    @Override
    public void afterTextChanged(Editable s){

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count){
        String offset = offsetArea.getText().toString();
        if(offset == null || offset.equals("")){
           return;
        }

        Line line = WorkspaceActivity.analyzer.get(new UnsignedInteger(offset));
        if(line == null){
            informationText.setText(R.string.create_patch_no_information);
        }else{
            informationText.setText(line.toString());
        }
    }
}