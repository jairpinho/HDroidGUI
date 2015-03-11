package su.harbour.hDroidGUI;

import android.os.Environment;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import android.content.Context;
import android.app.Activity;
import android.view.Menu;
import android.content.Intent;
import android.os.Handler;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.view.KeyEvent;
import android.widget.CheckBox;

import android.widget.LinearLayout.LayoutParams;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.util.Log;
import android.widget.Toast;

import android.app.Notification;
import android.app.NotificationManager;

import android.content.res.Resources;


public class Harbour {

    private static final String TAG = "Harbour";
    private static final String MAINHRB = "main.hrb";

    private static boolean bHrb;
    private static View mainView;
    private static Harbour hbobj;
    public static Context context;
    private static String sPackage = null;
    private static Resources resources = null;
    public static Class dopClass = null;
    public static String cHomePath;
    public static String sMenu = null;
    private static String sActivity = null;

    public Harbour( Context cont ) {
       context = cont;
       cHomePath = context.getFilesDir() + "/";
       setHomePath( cHomePath );
       hbobj = this;
       sPackage = context.getPackageName();
       resources = context.getResources();
    }

    public native void vmInit();
    public native void vmQuit();
    public native void setHomePath( String js );
    public native void setHrb( String js );
    public native String hrbOpen( String js );
    public native String hrbCall( String jsModName, String jsParam );

    public void Init( boolean bHrb ) {
       this.bHrb = bHrb;
       vmInit();
       if( bHrb )
          CopyFromAsset( MAINHRB );
    }

    public static void setDopClass( Class dclass ) {
       dopClass = dclass;
    }

    public static void setContext( Context cont, View view ) {
       context = cont;
       mainView = view;
    }

    public View createAct( Context cont, String sAct ) {

       String sMain;

       context = cont;
       if( bHrb )
          hrbOpen( MAINHRB );

       if( sAct != null )
          sMain = sAct;
       else {
          hrbCall( "HD_MAIN", bHrb? "1" : "2" );
          sMain = sActivity;
       }
       //Log.i(TAG, "hrbmain-1");
       if( sMain != null )
          mainView = CreateActivity( (Activity)context, sMain);
       else {
          sMain = "null";
          mainView = null;
       }
       //Log.i(TAG, "hrbmain-2");
       
       if( mainView == null ) {
       
          LinearLayout ll = new LinearLayout(context);
          ll.setOrientation(LinearLayout.VERTICAL);

          TextView textview = new TextView(context);
          textview.setText(sMain);
          ll.addView(textview);

          return ll;
       }
       else
          return mainView;

    }

    public static void closeAct( String id ) {

       hbobj.hrbCall( "HD_CLOSEACT", id );

    }

    public static void SetMenu( Menu menu ) {

       if( sMenu == null )
          return;

       int nPos1 = 2, nPos2, nPosEnd = sMenu.indexOf( ")]" );
       int nIndex = 1;

       do {
          nPos2 = sMenu.indexOf( ",,",nPos1 );
          if( nPos2 <= 0 || nPos2 > nPosEnd )
             nPos2 = nPosEnd;
          menu.add( Menu.NONE, nIndex, Menu.NONE, sMenu.substring( nPos1,nPos2 ) );
          nIndex ++;
          nPos1 = nPos2 + 2;
       } while( nPos1 < nPosEnd );

       sMenu = null;

    }

    public static void onMenuSel( int id ) {
       hbobj.hrbCall( "EVENT_MENU", "/" + id );
    }

    private View CreateActivity( Activity act, String sContent ) {

       View rootView;
       if( !sContent.substring(0,4).equals("act:") )
          return null;
       int nPos1 = sContent.indexOf(",,",4), nPosNext;

       //sActId = sContent.substring(4,nPos1);
       nPosNext = sContent.indexOf(",,/",5);
       String [][] aParams = GetParamsList( sContent.substring(nPos1,nPosNext) );
       int iArr = 0;
       while( aParams[iArr][0] != null ) {

          if( aParams[iArr][0].equals("t") ) {
             act.setTitle( getStr(aParams[iArr][1]) );
          }
          iArr ++;
       }

       sContent = sContent.substring(nPosNext+3);

       if( sContent.substring(0,4).equals("menu") ) {
          nPosNext = sContent.indexOf(",,/",5);
          sMenu = sContent.substring(4,nPosNext);
          sContent = sContent.substring(nPosNext+3);
       }
       if( sContent.substring(0,3).equals("lay") )
          rootView = CreateGroupView(sContent);
       else
          rootView = CreateView(sContent);
            
       return rootView;
    }

    private View CreateGroupView( String sContent ) {

       int nPos = sContent.indexOf("[(");
       int nPos1 = sContent.indexOf(",,");
       String sObjName;

       LinearLayout ll = new LinearLayout(context);

       //Log.i(TAG, "CreateG-1/"+sContent);
       if( nPos1 >= 0 && nPos1 < nPos ) {
          String [][] aParams = GetParamsList( sContent.substring(nPos1,nPos) );
          int iArr = 0;
          while( aParams[iArr][0] != null ) {

             //Log.i(TAG, "CreateG-2 "+aParams[iArr][0]+"/"+aParams[iArr][1]);
             if( aParams[iArr][0].equals("o") ) {
                if( aParams[iArr][1].equals("v") )
                   ll.setOrientation(LinearLayout.VERTICAL);
                else
                   ll.setOrientation(LinearLayout.HORIZONTAL);
             } else if( aParams[iArr][0].equals("cb") ) {
                ll.setBackgroundColor(parseColor(aParams[iArr][1]));
             }
             iArr ++;
          }
          SetSize( (View)ll, aParams );
          sObjName = sContent.substring(4,nPos1);

       }  else
          sObjName = sContent.substring(4,nPos);

       if( !sObjName.isEmpty() )
          ll.setTag( sObjName );
          
       // scan layout items
       sContent = sContent.substring(nPos+2);
       //Log.i(TAG, "CreateG-3/"+sContent);
       nPos1 = 0;
       View mView;
       int nPos2;
       int nLast = sContent.length() - 2;
       do {
          nPos2 = sContent.indexOf(")]",nPos1);
          //Log.i(TAG, "CreateG4a "+nPos+" "+nPos2+" "+nPos1+" "+nLast+" "+sContent.substring(nPos1));
          if( sContent.substring(nPos1,nPos1+3).equals("lay") ) {
             nPos = nPos1;
             int i1 = -1;
             int i2 = 1;
             while( true ) {
                while( nPos < nPos2 && nPos >= 0 ) {
                   i1 ++;
                   nPos = sContent.indexOf("[(",nPos+2);
                }
                if( i1 <= i2 )
                   break;
                else {
                   nPos2 = sContent.indexOf(")]",nPos2+2);
                   if( nPos2 < 0 )
                      return null;
                   i2 ++;
                }
             }
             nPos2 += 2;
             //Log.i(TAG, "CreateG4b "+nPos+" "+nPos2+" "+nPos1);
             mView = CreateGroupView(sContent.substring(nPos1,nPos2));
             if( sContent.substring(nPos2,nPos2+3).equals(",,/") )
                nPos2 +=3;
             nPos = nPos1 = nPos2;
          }
          else {
             nPos = sContent.indexOf(",,/",nPos1);
             if( nPos < 0 ) //|| nPos > nPos2 )
                nPos = nLast;
             mView = CreateView(sContent.substring(nPos1,nPos));
             nPos1 = nPos + 3;
          }
          if( mView == null )
             return null;

          ll.addView(mView);
          //Log.i(TAG, "CreateG5 "+(String)ll.getTag()+":"+(String)mView.getTag()+"/"+nPos+" "+nPos2+" "+nPos1);
       } while( nPos < nLast);
       
       return ll;
    }

    private View CreateView( String sContent ) {

       String sName;
       String sObjName = "";
       View mView;
       boolean bScroll = false;
       int nPos2 = sContent.indexOf(")]");
       int nPos = sContent.indexOf(",,/");

       //Log.i(TAG, "CreateT-1 "+sContent);
       if( nPos < 0 || nPos > nPos2 )
          nPos = nPos2;
       if( nPos >= 0 )
          sContent = sContent.substring(0,nPos);
       //Log.i(TAG, "CreateT-2/"+sContent);

       nPos = sContent.indexOf(",,");
       if( nPos < 0 )
          return null;

       String [][] aParams = GetParamsList( sContent.substring(nPos) );
       int iArr = 0;

       sName = sContent.substring(0,nPos);
       nPos = sName.indexOf(":");
       if( nPos > 0 ) {
          sObjName = sName.substring(nPos+1);
          sName = sName.substring(0,nPos);
       }
       if( sName.equals("txt") ) {

          TextView mtextview = new TextView(context);
          while( aParams[iArr][0] != null ) {

             if( aParams[iArr][0].equals("t") ) {
                mtextview.setText(getStr(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("ct") ) {
                mtextview.setTextColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("cb") ) {
                mtextview.setBackgroundColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("f") ) {
                setFont( mtextview, aParams[iArr][1] );
             } else if( aParams[iArr][0].equals("scroll") ) {
                bScroll = true;
             }
             iArr ++;
          }
          mView = mtextview;

       } else if( sName.equals("btn") ) {

          Button mButton = new Button(context);
          while( aParams[iArr][0] != null ) {

             if( aParams[iArr][0].equals("t") ) {
                mButton.setText(getStr(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("ct") ) {
                mButton.setTextColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("cb") ) {
                mButton.setBackgroundColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("f") ) {
                setFont( mButton, aParams[iArr][1] );
             } else if( aParams[iArr][0].equals("bcli") ) {
                if( !sObjName.isEmpty() )
                   mButton.setOnClickListener(new View.OnClickListener() {
                      public void onClick(View v) {
                         String sRes = hrbCall( "EVENT_BTNCLICK",(String)v.getTag() );
                      }
                   });
             }
             iArr ++;
          }
          mView = mButton;

       } else if( sName.equals("edi") ) {

          EditText medit = new EditText(context);
          while( aParams[iArr][0] != null ) {

             if( aParams[iArr][0].equals("t") ) {
                medit.setText(getStr(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("ct") ) {
                medit.setTextColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("cb") ) {
                medit.setBackgroundColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("hint") ) {
                medit.setHint(getStr(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("f") ) {
                setFont( medit, aParams[iArr][1] );
             } else if( aParams[iArr][0].equals("bkey") ) {
                if( !sObjName.isEmpty() ) {
                   medit.setOnKeyListener(new View.OnKeyListener() {
                      public boolean onKey(View v, int keyCode, KeyEvent event) {
                         if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            String sRes = hrbCall( "EVENT_KEYDOWN",(String)v.getTag()+":"+keyCode );
                            return sRes.equals( "1" )? true : false;
                         }
                         return false;
                      }
                   });
                }
             }
             iArr ++;
          }
          mView = medit;

       } else if( sName.equals("che") ) {

          CheckBox mche = new CheckBox(context);
          while( aParams[iArr][0] != null ) {

             if( aParams[iArr][0].equals("t") ) {
                mche.setText(getStr(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("ct") ) {
                mche.setTextColor(parseColor(aParams[iArr][1]));
             } else if( aParams[iArr][0].equals("cb") ) {
                mche.setBackgroundColor(parseColor(aParams[iArr][1]));
             }
             iArr ++;
          }
          mView = mche;
       }  else
          return null;

       if( !sObjName.isEmpty() )
          mView.setTag( sObjName );


       if( bScroll ) {

          ScrollView sv = new ScrollView(context);

          SetSize( sv, aParams );
          LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
          mView.setLayoutParams(parms);

          sv.addView(mView);
          return sv;

       } else {

          SetSize( mView, aParams );
          return mView;
       }
    }

    private static void setFont( TextView mView, String sFont ) {
       
       int nface = Integer.parseInt( sFont.substring(0,1) );
       int nstyle = Integer.parseInt( sFont.substring(2,3) );
       int nsize = Integer.parseInt( sFont.substring(4) );

       if( nface != 0 || nstyle != 0 ) {
          Typeface tface = null;
          switch( nface ) {
              case 0: 
                 tface = Typeface.DEFAULT;
                 break;
              case 1: 
                 tface = Typeface.SANS_SERIF;
                 break;
              case 2: 
                 tface = Typeface.SERIF;
                 break;
              case 3: 
                 tface = Typeface.MONOSPACE;
                 break;
          }
          mView.setTypeface( tface, nstyle );          
       }
       if( nsize != 0 )
           mView.setTextSize( TypedValue.COMPLEX_UNIT_DIP,nsize );
    }

    private static void SetSize( View mView, String [][] aParams ) {      

       int iArr = 0;
       int iHeight = -10, iWidth = -10;
       while( aParams[iArr][0] != null ) {

          if( aParams[iArr][0].equals("h") ) {
             iHeight = Integer.parseInt(aParams[iArr][1]);
          } else if( aParams[iArr][0].equals("w") ) {
             iWidth = Integer.parseInt(aParams[iArr][1]);
          }
          iArr ++;
       }
       if( iHeight != -10 || iWidth != -10 ) {
          LinearLayout.LayoutParams parms;
          if( iHeight == -10 )
             iHeight = LinearLayout.LayoutParams.MATCH_PARENT;
          if( iWidth == -10 )
             iWidth = LinearLayout.LayoutParams.MATCH_PARENT;
          if( iHeight == 0 || iWidth == 0 )
             parms = new LinearLayout.LayoutParams(iWidth,iHeight,1);
          else
             parms = new LinearLayout.LayoutParams(iWidth,iHeight);
          mView.setLayoutParams(parms);
       }

    }

    private static int parseColor( String sColor ) {

       int iColor;

       try {
          iColor = Color.parseColor( sColor );

       } catch (IllegalArgumentException e) {
          iColor = 0;
       }

       return iColor;
    }

    private static String[][] GetParamsList( String sParam ) {
    
       String [][] aParams = new String [8][2];
       aParams[0][0] = null;

       int nPos;
       int nPos1;
       int iArr = 0;
       String sP;
       if( sParam.substring(0,2).equals(",,") )
          nPos1 = 2;
       else
          nPos1 = 0;
       //Log.i(TAG, "getp-0/"+sParam);
       do {
          nPos = sParam.indexOf(",,", nPos1);
          if( nPos < 0 )
             sP = sParam.substring(nPos1);
          else
             sP = sParam.substring(nPos1,nPos);

          //Log.i(TAG, "getp-1 "+nPos+" "+sP);
          nPos1 = sP.indexOf(":");
          if( nPos1 > 0 ) {
             aParams[iArr][1] = sP.substring(nPos1+1);
             aParams[iArr][0] = sP.substring(0,nPos1);
             iArr ++;
             aParams[iArr][0] = null;
          } else
             break;

          nPos1 = nPos + 2;

       } while( nPos > 0 );

       return aParams;
    }

    private void CopyFromAsset( String hrbName ) {

       String sFile = cHomePath + hrbName;

       setHrb( hrbName );

       if( ! (new File(sFile).isFile()) ) {
          try {
               InputStream myInput = context.getAssets().open(hrbName);
               OutputStream myOutput = new FileOutputStream( sFile );

               byte[] buffer = new byte[myInput.available()];
               int read;
               while ((read = myInput.read(buffer)) != -1) {
                   myOutput.write(buffer, 0, read);
               }

               myOutput.flush();
               myOutput.close();
               myInput.close();

          } catch (IOException e) {
               // toast( "copyDataBase Error : " + e.getMessage() );
          }
       }
    }

    public static void toast( String message ) {

       Toast.makeText( context, message, Toast.LENGTH_SHORT ).show();

    }

    public static void hlog( String message ) {

       Log.i(TAG, message);
    }

    public static void jcb_sz_v( String message ) {

       String scmd, stag;
       TextView tview = null;
       int nPos = message.indexOf(":");
       int nPos1;
       
       if( nPos <= 0 )
          return;

       scmd = message.substring( 0,nPos );
       if( scmd.equals( "exit" ) ) {

          android.os.Process.killProcess(android.os.Process.myPid());
       } else {

          nPos1 = message.indexOf(":",nPos+1);
          if( nPos1 > 0 ) {
             stag = message.substring( nPos+1,nPos1 );
             tview = (TextView) mainView.findViewWithTag( stag );
          }

          if( scmd.equals( "settxt" ) ) {
             if( tview != null )
                tview.setText( getStr( message.substring( nPos1+1 ) ) );
          }
       }
    }

    public static String jcb_sz_sz( String message ) {

       String scmd, stag;
       TextView tview = null;
       int nPos = message.indexOf(":");
       int nPos1;
       
       if( nPos <= 0 )
          return "err";

       scmd = message.substring( 0,nPos );
       nPos1 = message.indexOf(":",nPos+1);
       if( nPos1 > 0 ) {
          stag = message.substring( nPos+1,nPos1 );
          tview = (TextView) mainView.findViewWithTag( stag );
       }

       if( scmd.equals( "gettxt" ) ) {
          if( tview != null )
             return (String) tview.getText().toString();
       }
       return "ok";
    }

    public static void activ( String sAct ) {

       if( !sAct.substring(0,4).equals("act:") )
          return;
       int nPos1 = sAct.indexOf(",,",4);
       String sId = sAct.substring(4,nPos1);

       if( sId.equals("0") )
          sActivity = sAct;
       else if( dopClass != null ) {
          Intent intent = new Intent( context, dopClass );

          intent.putExtra( "sact", sAct );
          intent.putExtra( "sid", sId );
          context.startActivity(intent);
       }
    }

    public static void adlg( String sDlg ) {

       if( !sDlg.substring(0,4).equals("dlg:") )
          return;
       int nPosNext, nPos1, nPos2, nPos3 = sDlg.indexOf(",,");
       int iBtns = 0;
       String [][] aParams;
       String sName, sObjName;
       String sText;
       String sId = sDlg.substring(4,nPos3);
       //DialogInterface.OnClickListener func;

       nPosNext = sDlg.indexOf(",,/",5);
       AlertDialog.Builder builder = new AlertDialog.Builder(context);

       aParams = GetParamsList( sDlg.substring(nPos3,nPosNext) );
       int iArr = 0;
       while( aParams[iArr][0] != null ) {

          if( aParams[iArr][0].equals("t") ) {
             builder.setTitle(aParams[iArr][1]);
          }
          iArr ++;
       }
       if( sDlg.indexOf(",,/btn") > 0 )
          builder.setCancelable(false);

       do {
          nPos1 = nPosNext + 3;
          nPosNext = sDlg.indexOf(",,/",nPos1);
          nPos2 = sDlg.indexOf(",,",nPos1);
          if( nPosNext < 0 )
             aParams = GetParamsList( sDlg.substring(nPos2) );
          else
             aParams = GetParamsList( sDlg.substring(nPos2,nPosNext) );

          iArr = 0;
          sText = "";
          while( aParams[iArr][0] != null ) {

             if( aParams[iArr][0].equals("t") ) {
                sText = aParams[iArr][1];
             } else if( aParams[iArr][0].equals("bcli") ) {
             }
             iArr ++;
          }

          nPos3 = sDlg.indexOf(":",nPos1);
          sName = sDlg.substring(nPos1,nPos3);
          sObjName = sDlg.substring(nPos3+1,nPos2);
          if( sName.equals("btn") ) {
             iBtns ++;
             switch( iBtns ) {
                case 1:
                   builder.setNegativeButton(sText,new BtnClickListener(sObjName));
                   break;
                case 2:
                   builder.setNeutralButton(sText,new BtnClickListener(sObjName));
                   break;
                case 3:
                   builder.setPositiveButton(sText,new BtnClickListener(sObjName));
                   break;
             }
          } else if( sName.equals("txt") ) {
             builder.setMessage(sText);
          }            
       } while( nPosNext > 0 );
    	
        AlertDialog alert = builder.create();
        alert.show();

    }

    private static class BtnClickListener implements DialogInterface.OnClickListener {
        String sBtnName;

        public BtnClickListener( String s ){
             super();
             sBtnName = s;
        }

        @Override
        public void onClick(DialogInterface dialog, int id) {
           dialog.cancel();
           //Log.i(TAG, "Dialog - click "+sBtnName);
           hbobj.hrbCall( "EVENT_BTNCLICK",sBtnName );
        }
    }

    static Handler tmHandler;
    static Runnable tmRunnable;
    static String [] aTimers = new String [12];
    static long [][] aTimeVal = new long [12][2];
    static int iTimers = 0;

    public static void timer( String sTimer ) {

       String sId;

       if( sTimer.substring(0,4).equals("set:") ) {
          int nPos = sTimer.indexOf( ":", 4 );
          sId = sTimer.substring( 4,nPos );

          aTimers[iTimers] = sId;
          aTimeVal[iTimers][0] = Integer.parseInt( sTimer.substring( nPos+1 ) );
          aTimeVal[iTimers][1] = System.currentTimeMillis();
          iTimers ++;

          if( tmHandler == null ) {
             tmHandler = new Handler();
             tmRunnable = new Runnable() {

                @Override
                public void run() {
                    long millis = System.currentTimeMillis();
                    long nVal = 100000;
                    int i;

                    for( i = 0; i < iTimers; i++ ) {
                       if( millis >= aTimeVal[i][1] ) {
                          aTimeVal[i][1] = millis + aTimeVal[i][0];
                          hbobj.hrbCall( "EVENT_TIMER",aTimers[i] );
                       }
                       if( nVal > aTimeVal[i][1] - millis )
                          nVal = aTimeVal[i][1] - millis;
                    }

                    tmHandler.postDelayed(this, nVal);
                }
             };
          }

          tmHandler.postDelayed(tmRunnable, 0);

       } else if( sTimer.substring(0,5).equals("kill:") ) {
          int i, j;
          sId = sTimer.substring( 5 );

          for( i = 0; i < iTimers; i++ ) {
             if( aTimers[i] == sId ) {
                for( j = i; j < iTimers-1; j++ ) {
                   aTimers[j] = aTimers[j+1];
                   aTimeVal[j][0] = aTimeVal[j+1][0];
                   aTimeVal[j][1] = aTimeVal[j+1][1];
                }
                break;
             }
          }

          iTimers--;
          if( iTimers == 0 )
             tmHandler.removeCallbacks(tmRunnable);
       }

    }

    public static void notify( String sNotify ) {

       Notification.Builder builder = new Notification.Builder(context);

       int notifyId, nDef = 0;
       int nPos = sNotify.indexOf( ",," ), nPos2;

       if( nPos > 0 ) {
          notifyId = Integer.parseInt( sNotify.substring( 0,nPos ) );
          //nPos2 = nPos + 2;
          //nPos = sNotify.indexOf( ",,",nPos2 );
          if( nPos > 0 && sNotify.substring( nPos+5,nPos+7 ).equals( ",," ) ) {
             if( sNotify.substring( nPos+2,nPos+3 ).equals( "y" ) )
                nDef |= Notification.DEFAULT_LIGHTS;
             if( sNotify.substring( nPos+3,nPos+4 ).equals( "y" ) )
                nDef |= Notification.DEFAULT_SOUND;
             if( sNotify.substring( nPos+4,nPos+5 ).equals( "y" ) )
                nDef |= Notification.DEFAULT_VIBRATE;
             builder.setDefaults( nDef );

             nPos2 = nPos + 7;
             nPos = sNotify.indexOf( ",,",nPos2 );
             if( nPos > 0 ) {
                builder.setContentTitle( sNotify.substring( nPos2,nPos ) );

                nPos2 = nPos + 2;
                nPos = sNotify.indexOf( ",,",nPos2 );
                if( nPos > 0 ) {
                   builder.setContentText( sNotify.substring( nPos2,nPos ) );

                   nPos2 = nPos + 2;
                   nPos = sNotify.indexOf( ",,",nPos2 );
                   if( nPos > 0 )
                      builder.setSubText( sNotify.substring( nPos2,nPos ) );
                }

                builder.setSmallIcon( android.R.drawable.arrow_down_float );
                NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notifMan.notify( notifyId, builder.build() );
             }
          }
       }

    }


    public static String getSysDir( String type ) {
       if( type.equals( "doc" ) )
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/";
       else if( type.equals( "pic" ) )
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/";
       else if( type.equals( "mus" ) )
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/";
       else if( type.equals( "mov" ) )
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/";
       else if( type.equals( "down" ) )
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
       else if( type.equals( "ring" ) )
          return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES ) + "/";
       else if( type.equals( "ext" ) )
          return Environment.getExternalStorageDirectory() + "/";
       else
          return "";
    }

    private static String getStr( String sRes ) {
       if( sRes.substring( 0,2 ).equals( "$$" ) ) {
          int id = resID( sRes.substring( 2 ), "string" );
          if( id == 0 )
             return "";
          else
             return context.getString( id );
       }
       else
          return sRes;
    }

    private static int resID( String sRes, String sType ) {

       int id = 0;

       try {
          id = resources.getIdentifier( sRes, sType, sPackage );
       }
       catch (Exception e) {
          id = 0;
       }
       
       return id;
    }

    static {
        System.loadLibrary("harbour");
        System.loadLibrary("h4droid");
    }

}
