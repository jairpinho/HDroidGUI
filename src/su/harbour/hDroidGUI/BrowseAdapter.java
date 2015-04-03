package su.harbour.hDroidGUI;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.AbsListView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;

import android.widget.LinearLayout.LayoutParams;
import android.view.Gravity;

import org.json.JSONException;


public class BrowseAdapter extends BaseAdapter {

   static final int TEXT = 2;
   Context ctx;
   String stag = null;
   int [] aColumns = null;
   int [] aColHeadAlign = null;
   int iRowHeight = 0, iRowtColor = -10;
   String sRowStyle = null;

   BrowseAdapter( Context context, String tag, JSONArray jaRow, JSONArray jaCol ) {
     ctx = context;
     stag = tag;

     int i, nPos, ilen;
     String sItem, sName;

     if( jaRow != null ) {
        ilen = jaRow.length();
        try {
           for( i = 0; i<ilen; i++ ) {
              sItem = jaRow.getString(i);
              nPos = sItem.indexOf( ":" );
              sName = sItem.substring( 0,nPos );
              if( sName.equals("h") )
                 iRowHeight = Integer.parseInt( sItem.substring(nPos+1) );
              else if( sName.equals("stl") )
                 sRowStyle = sItem.substring(nPos+1);
              else if( sName.equals("ct") )
                 iRowtColor = CreateUI.parseColor( sItem.substring(nPos+1) );
           }
        }   
        catch (JSONException e) {
           Harbour.hlog("jaRow error");
           return;
        }
     }
     ilen = jaCol.length();
     aColumns = new int [ilen];
     aColHeadAlign = new int [ilen];
     try {
        JSONArray ja;
        int j, ilen1;
        for( i = 0; i<ilen; i++ ) {
           ja = jaCol.getJSONArray(i);
           ilen1 = ja.length();
           for( j = 0; j<ilen1; j++ ) {
              sItem = ja.getString(j);
              nPos = sItem.indexOf( ":" );
              sName = sItem.substring( 0,nPos );
              if( sName.equals("w") )
                 aColumns[i] = Integer.parseInt( sItem.substring( nPos+1 ) );
              else if( sName.equals("ali") )
                 aColHeadAlign[i] = Integer.parseInt( sItem.substring( nPos+1 ) );
           }
        }
     }   
     catch (JSONException e) {
        Harbour.hlog("jaRow error");
        return;
     }

   }

   @Override
   public int getCount() {

      return Integer.parseInt( Harbour.hbobj.hrbCall( "CB_BROWSE","cou:" + stag + ":" ) );
   }

   @Override
   public Object getItem( int position ) {
      return null;
   }

   @Override
   public long getItemId( int position ) {
      return position;
   }

   @Override
   public View getView( int position, View convertView, ViewGroup parent ) {

     View view = convertView;
     String sRow = Harbour.hbobj.hrbCall( "CB_BROWSE","row:" + stag + ":" + position );
     int nWidth, i, iLength = aColumns.length;
     LinearLayout ll;
     TextView tv;

     if(view == null) {    
        // Create row as a View
        LinearLayout.LayoutParams parms;
        ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.HORIZONTAL);

        if( iRowHeight > 0 ) {
           AbsListView.LayoutParams params = new AbsListView.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, iRowHeight );
           ll.setLayoutParams(params);
        }
        if( sRowStyle != null ) {
           try {
              CreateUI.UIStyle.setDrawable( ll, sRowStyle );
           }
           catch (JSONException e) {
              Harbour.hlog("getview: json error");
           }
        }

        for( i=0; i<iLength; i++ ) {
           nWidth = aColumns[i];
           tv = new TextView(ctx);
           if( nWidth == 0 )
              parms = new LinearLayout.LayoutParams(  nWidth, LinearLayout.LayoutParams.MATCH_PARENT, 1 );
           else
              parms = new LinearLayout.LayoutParams(  nWidth, LinearLayout.LayoutParams.MATCH_PARENT );
           tv.setLayoutParams(parms);
           if( aColHeadAlign[i] > 0 )
              CreateUI.SetAlign( tv, aColHeadAlign[i], TEXT );
           else
              tv.setGravity( Gravity.CENTER_VERTICAL );
           if( iRowtColor != -10 )
              tv.setTextColor(iRowtColor);
           ll.addView( tv );
        }

        view = ll;
     }

     // Fill the row (View) with values
     JSONArray jArray = null;
     try {
        jArray = new JSONArray(sRow);
     }
     catch (JSONException e){
        Harbour.hlog("getView: jArray - error");
     }
     for( i=0; i<iLength; i++ ) {
        tv = (TextView) ((ViewGroup)view).getChildAt(i);
        try {
           tv.setText( jArray.getString(i) );
        }
        catch (JSONException e){
           Harbour.hlog("getView: jArray.get - error ("+i+"/"+iLength+")");
        }
     }

     return view;
   }

}
