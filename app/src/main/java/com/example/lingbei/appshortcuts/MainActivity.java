package com.example.lingbei.appshortcuts;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ListActivity implements OnClickListener {

    static final String TAG = "ShortcutSample";
    private static final String ID_ADD_WEBSITE = "add_website";
    private static final String ACTION_ADD_WEBSITE="com.example.lingbei.appshortcuts.ADD_WEBSITE";

    private static final List<ShortcutInfo> EMPTY_LIST = new ArrayList<>();
    private MyAdapter myAdapter;
    private ShortcutHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHelper = new ShortcutHelper(this);
        mHelper.maybeRestoreAllDynamicShortcuts();
        mHelper.refreshShortcuts(false);
        if(ACTION_ADD_WEBSITE.equals(getIntent().getAction())){
            addWebSite();
        }
        myAdapter = new MyAdapter(this.getApplicationContext());
        setListAdapter(myAdapter);


    }


    @Override
    protected  void onResume(){
        super.onResume();
        refreshList();
    }

    public void onAddPressed(View v){addWebSite();}


    private void addWebSite(){
        Log.i(TAG,"addWebSite");
        mHelper.reportShortcutUsed(ID_ADD_WEBSITE);

        final EditText editUri = new EditText(this);
        editUri.setHint("http://www.android.com/");
        editUri.setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI);

        new AlertDialog.Builder(this)
                .setTitle("Add new website")
                .setMessage("Type URL of a website")
                .setView(editUri)
                .setPositiveButton("Add",(dialog,whichButton)->{
                    final String url = editUri.getText().toString().trim();
                    if (url.length()>0){
                        addUriAsync(url);
                    }
                }).show();
    }

    public void addWebSiteShortcut(String urlAsString){
        final String uriFinal = urlAsString;

    }


    private void addUriAsync(String uri){
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params){
                mHelper.addWebSiteShortcut(uri);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid){refreshList();}
        }.execute();
    }

    private void  refreshList(){
        myAdapter.setShortCuts(mHelper.getShortcuts());
    }


    @Override
    public void onClick(View v){
        //
    }




    private String getType(ShortcutInfo shortcut) {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        if (shortcut.isDynamic()) {
            sb.append(sep);
            sb.append("Dynamic");
            sep = ", ";
        }
        if (shortcut.isPinned()) {
            sb.append(sep);
            sb.append("Pinned");
            sep = ", ";
        }
        if (!shortcut.isEnabled()) {
            sb.append(sep);
            sb.append("Disabled");
            sep = ", ";
        }
        return sb.toString();
    }


    private class MyAdapter extends BaseAdapter{
        private final Context mContext;
        private final LayoutInflater mInflater;

        private List<ShortcutInfo> mList= EMPTY_LIST;


        public MyAdapter(Context context){
            mContext = context;
            mInflater = mContext.getSystemService(LayoutInflater.class);
        }

        @Override
        public int getCount(){return mList.size();}

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        public void setShortCuts(List<ShortcutInfo> list){
            mList = list;
            notifyDataSetChanged();

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            final View view;
            if(convertView!=null){
                view = convertView;
            }else {
                view = mInflater.inflate(R.layout.list_item,null);
            }

            bindView(view,position,mList.get(position));
            return view;
        }


        public void bindView(View view,int position,ShortcutInfo shortcutInfo){
            view.setTag(shortcutInfo);

            final TextView line1 =  view.findViewById(R.id.line1);
            final TextView line2 =  view.findViewById(R.id.line2);

            line1.setText(shortcutInfo.getLongLabel());
            line2.setText(getType(shortcutInfo));


            final Button remove =  view.findViewById(R.id.remove);
            final Button disable = view.findViewById(R.id.disable);

            disable.setText(shortcutInfo.isEnabled()?R.string.disable_shortcut:R.string.enable_shortcut);

            remove.setOnClickListener(MainActivity.this);
            disable.setOnClickListener(MainActivity.this);

        }
    }
}
