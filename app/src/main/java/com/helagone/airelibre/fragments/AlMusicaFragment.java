package com.helagone.airelibre.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.helagone.airelibre.R;
import com.helagone.airelibre.activity.ScheduleActivity;
import com.helagone.airelibre.connectivity.Connectivity;
import com.helagone.airelibre.datafetch.CurrentMetadataFetcher;
import com.helagone.airelibre.datamodel.TrackModel;
import com.helagone.airelibre.service.RadioManager;
import com.helagone.airelibre.utility.Shoutcast;
import com.helagone.airelibre.utility.ShoutcastHelper;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlMusicaFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlMusicaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlMusicaFragment extends Fragment {


    ImageButton trigger;
    ImageView coverart;

    Button gotoPlaylist;
    ToggleButton likeEmpty;
    TextView artistName;
    TextView time_remain;
    TextView time_duration;
    TextView spacer_pipe;

    String coverUrl = "http://lisa.mx/airelibre/al_imagenes/art-00.jpg";
    String streamURL;
    String cover_endpoint;

    //FETCH METADATA ///////////////////////////
    String lametadata;
    String[] meta_parts;

    String artist_name;
    String track_name;
    String album_name;
    String duration;
    String start_time;

    int loquefalta;
    int duration_in_millis;
    int tiempo_transcurrido;
    private Handler handler = new Handler();

    private List<Shoutcast> shoutcasts = new ArrayList<>();

    CircularProgressBar mProgressBar;

    Date d_current_date;
    SimpleDateFormat thedateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    public int limit;

    Boolean isready = false;
    Boolean addTo = false;

    public ArrayList<TrackModel> oneTrackModels = new ArrayList<>();

    Typeface custom_font;
    Typeface ws_semibold;

    RadioManager radioManager;
    Drawable menu_night;
    Toolbar toolbar;

    SharedPreferences shPreferences;
    SharedPreferences.Editor editor;
    String sharedString;

    String _the_time;
    String tracksStr;

    int transc_to_cent;
    Timer timer;

    private OnFragmentInteractionListener mListener;

    int linkSpeed;

    public AlMusicaFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AlMusicaFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlMusicaFragment newInstance() {
        return new AlMusicaFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //get arguments
        }

        shoutcasts = ShoutcastHelper.retrieveShoutcasts(getActivity());
        radioManager = new RadioManager(getActivity());

        timer = new Timer();
        timer.schedule(new timerUpdTask(), 0, 1000);

        checkConnectivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_al_musica, container, false);

        trigger = fragmentView.findViewById(R.id.id_trigger);
        coverart = fragmentView.findViewById(R.id.imgAlbum);
        mProgressBar = fragmentView.findViewById(R.id.circularprogressBar);

        gotoPlaylist = fragmentView.findViewById(R.id.btn_to_playlist);
        artistName = fragmentView.findViewById(R.id.lbl_artistName);
        time_remain = fragmentView.findViewById(R.id.lbl_timeremain);
        time_duration = fragmentView.findViewById(R.id.lbl_trDur);
        spacer_pipe = fragmentView.findViewById(R.id.lbl_item_separador);
        likeEmpty = fragmentView.findViewById(R.id.likeEmpty);

        gotoPlaylist.setTypeface(ws_semibold);
        artistName.setTypeface(custom_font);
        time_remain.setTypeface(custom_font);
        time_duration.setTypeface(custom_font);
        spacer_pipe.setTypeface(custom_font);

        //SharedPreferences sh_prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        //handler.postDelayed(progressbar_update, 1000);
        //handler.postDelayed(runnable, sh_prefs.getInt("loquefalta", 2000));

        toolbar = getActivity().findViewById(R.id.toolbar);
        menu_night = getResources().getDrawable(R.drawable.ic_menu_night);

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            final View v = toolbar.getChildAt(i);

            if (v instanceof ImageButton) {
                ((ImageButton) v).setImageDrawable(menu_night);
            }
        }

        timeOfDay(fragmentView);

        SharedPreferences dw_sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        int sh_transcurrido = dw_sharedPrefs.getInt("transcurrido", 2000);
        int sh_duracion = dw_sharedPrefs.getInt("duracion", 2000);
        transc_to_cent = sh_transcurrido * 100 / sh_duracion;
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(radioManager != null && Connectivity.isConnected(getActivity())){
                    if(radioManager.isPlaying()){
                        trigger.setBackground(getResources().getDrawable(R.drawable.ic_album_inside_circle));
                        trigger.setImageResource(R.drawable.ic_play_arrow_black);
                    }else{
                        trigger.setBackground(getResources().getDrawable(R.color.transparente));
                        trigger.setImageResource(R.color.transparente);
                    }
                    //TODO: SET STREAM TO PROD. 0 -> prod, 1 -> dev1, 2 -> dev2 (myradiostream), 3 -> dev3 (mediastream demo), 4 -> dev4 (streamguys demo)
                    shoutcasts = ShoutcastHelper.retrieveShoutcasts(getActivity());
                    //artistName.setText(shoutcasts.get(0).getName());
                    streamURL = shoutcasts.get(4).getUrl();
                    //SENDING STRING URL TO ACTIVITY @ radioManager -> playOrPause
                    mListener.onFragmentInteraction(streamURL);
                }
                //////////////////////////////////////////////////////////////////////////////////////////////////////////
            }
        });

        gotoPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), ScheduleActivity.class));
            }
        });


        /*if(getActivity() != null){
            if(getActivity() != null){
                RequestOptions options = new RequestOptions();
                options.circleCrop();
                Glide.with(getActivity()).load(coverUrl).apply(options).into(coverart);
            }
        }*/


        /*
         *  CLICK TO UP VOTE
         */
        likeEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //VARIABLES
                String _trackArtist = (String) artistName.getText();
                String _trackName = (String) gotoPlaylist.getText();
                String _trackAlbumName = (String) gotoPlaylist.getText();
                String _trackDuration = (String) time_duration.getText();

                shPreferences = getActivity().getSharedPreferences("tracksSet", Context.MODE_PRIVATE);
                sharedString = shPreferences.getString("trackSet", null);
                editor = shPreferences.edit();

                //ANALYTICS
                /*Bundle bundle4 = new Bundle();
                bundle4.putString(FirebaseAnalytics.Param.ITEM_NAME, _trackName);
                bundle4.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "music_like");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle4);*/
                //END VARIABLES

                if( shPreferences.getAll().isEmpty() ){
                    JSONObject jsonObjectTrack =  new JSONObject();
                    try{
                        jsonObjectTrack.put("_artistName", _trackArtist);
                        jsonObjectTrack.put("_trackName", _trackName);
                        jsonObjectTrack.put("_trackDur", _trackDuration);
                        tracksStr += jsonObjectTrack.toString();
                        String cleanToUp = tracksStr.replace("null", "");
                        editor.putString("trackSet", cleanToUp).apply();

                        String retTrackSet = shPreferences.getString("trackSet", null);

                    }catch(JSONException jsex){
                        jsex.printStackTrace();
                    }
                }else{
                    String trackSet = shPreferences.getString( "trackSet", null);
                    String replaceTrackSet = trackSet.replace("null", "");
                    String[] arr_str = replaceTrackSet.split("([}])");
                    for(String s: arr_str){
                        String cleanStr = s.replace(",{", "{");
                        try {
                            JSONObject oneObj = new JSONObject(cleanStr+"}");
                            if(oneObj.getString("_artistName").equals(_trackArtist) && oneObj.getString("_trackName").equals(_trackName)){
                                addTo = false;
                            }else{
                                addTo = true;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    JSONObject newjsonObjectTrack =  new JSONObject();
                    try{
                        newjsonObjectTrack.put("_artistName", _trackArtist);
                        newjsonObjectTrack.put("_trackName", _trackName);
                        newjsonObjectTrack.put("_trackDur", _trackDuration);
                        if(addTo){
                            replaceTrackSet += ","+newjsonObjectTrack;
                            editor.putString("trackSet", replaceTrackSet).apply();
                        }
                    }catch(JSONException jsex){
                        jsex.printStackTrace();
                    }
                }//END SHARED PREFERENCES TEST
            }
        });//END ON CLICK LISTENER LIKE EMPTY
        // Inflate the layout for this fragment
        return fragmentView;
    }//END ON CREATE



    /**
     * RUNNABLE UPDATE METADATA IN UI
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.d("Runnable", "---------------RUN------------");
            if(getActivity() !=  null){
                SharedPreferences cr_sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
                int cr_loquefalta =  cr_sharedPreferences.getInt("loquefalta", 2000);
                //new MetadataFetcher().execute();
                handler.postDelayed(runnable, cr_loquefalta);
                Log.d("Loquefalta > 1", String.valueOf( cr_loquefalta ));
                Log.d("Runnable", "---------------RUN------------");
            }
        }
    };


    private class timerUpdTask extends TimerTask{
        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    time_remain.setText(updTimer());
                }
            });
        }
    }

    private String updTimer (){
        tiempo_transcurrido=tiempo_transcurrido+1000;
        int minutos = tiempo_transcurrido/1000/60;
        int segundos = tiempo_transcurrido/1000 % 60;
        if(tiempo_transcurrido >= duration_in_millis){
            tiempo_transcurrido = 0;
            runMetadataFetcher();
            putCover();
        }
        String upd_timer = String.format(Locale.getDefault(),"%02d:%02d", minutos, segundos );
        return upd_timer;
    }

    private void runMetadataFetcher(){
        if(Connectivity.isConnected(getActivity()) && Connectivity.isConnectedFast(getActivity())){
            new MetadataFetcher().execute();
        }else{
            Toast.makeText(getActivity(), "No tienes connexión", Toast.LENGTH_SHORT).show();
        }
    }


    private void putCover(){
        Handler coverHandler = new Handler();
        coverHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //new CoverFetcher().FetchCover("http://lisa.mx/airelibre/al_imagenes/art-00.jpg");
                RequestOptions options = new RequestOptions();
                options.circleCrop();
                if(getActivity() != null){
                    if(Connectivity.isConnected(getActivity())){
                        //TODO: CHANGE URL TO PROD URL FOR IMAGE COVER
                        //PROD
                        Glide.with(getActivity()).load("http://lisa.mx/airelibre/al_imagenes/art-00.jpg?"+System.currentTimeMillis()).apply(options).into(coverart);
                        //DEV
                        //Glide.with(getActivity()).load("http://lisa.mx/airelibre/hk-images/art-00.jpg?"+System.currentTimeMillis()).apply(options).into(coverart);
                    }else{
                        Glide.with(getActivity()).load(R.drawable.program_header_bg).apply(options).into(coverart);
                    }
                }
            }
        }, 2000);
    }

    /*private int LinkSpeed(){
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Metering Link Speed over WIFI in RSSI min -127, max -30
        try {
            linkSpeed = wifiManager.getConnectionInfo().getRssi();
            Log.d("strength", String.valueOf(linkSpeed));
        } catch (NullPointerException nulex) {
            nulex.printStackTrace();
        }
        return 0;
    }*/



    /*private  void connectionState(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if ( ( connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED && linkSpeed > -65 ) ||
                (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED) ) {
            connected = true;
            if( connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED && linkSpeed > -65 ){

            }else if( connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED && linkSpeed < -90){
                Toast.makeText(getActivity(), "Tu conexión GSM es buena", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(getActivity(), "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            connected = false;
            trigger.setBackground(getResources().getDrawable(R.drawable.ic_album_inside_circle));
            trigger.setImageResource(R.drawable.ic_play_arrow_black);
        }
    }*/


    private void checkConnectivity(){
        Boolean connected = Connectivity.isConnected(getActivity());
        Boolean wifiConn = Connectivity.isConnectedWifi(getActivity());
        Boolean mobileConn = Connectivity.isConnectedMobile(getActivity());
        Boolean fastConn = Connectivity.isConnectedFast(getActivity());

        if(wifiConn && mobileConn && fastConn && connected){
            Toast.makeText(getActivity(), "Tu conexión es buena", Toast.LENGTH_SHORT).show();
        }else if(!wifiConn && mobileConn && fastConn && connected){
            Toast.makeText(getActivity(), "Tu conexión GSM es buena", Toast.LENGTH_SHORT).show();
        }else if(wifiConn && ! mobileConn && fastConn && connected){
            Toast.makeText(getActivity(), "Tu conexión WIFI es buena", Toast.LENGTH_SHORT).show();
        }else if(wifiConn || mobileConn && fastConn && connected){
            Toast.makeText(getActivity(), "Tu conexión es buena", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getActivity(), "No tienes conexión", Toast.LENGTH_SHORT).show();
        }
    }

    private void timeOfDay(View view){
        //TODAY CALC
        d_current_date = new Date();
        _the_time = thedateFormat.format(d_current_date);
        String twentyfour = _the_time.replace(":", "");
        limit =  Integer.parseInt(twentyfour);

        //HANDLING TIME OF THE DAY
        if (limit > 1800 || limit < 700) {
            //Log.d("limit", String.valueOf(limit));
            view.setBackground(getResources().getDrawable(R.color.dark));

            gotoPlaylist.setTextColor(getResources().getColor(R.color.blanco));
            artistName.setTextColor(getResources().getColor(R.color.cool_grey));
            time_duration.setTextColor(getResources().getColor(R.color.cool_grey));
            time_remain.setTextColor(getResources().getColor(R.color.cool_grey));
            spacer_pipe.setTextColor(getResources().getColor(R.color.cool_grey));

            menu_night = getResources().getDrawable(R.drawable.ic_menu_night);

            try {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.dark));
            } catch (NullPointerException nex) {
                nex.printStackTrace();
            }
        }

    }












    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;

            runMetadataFetcher();

            custom_font = Typeface.createFromAsset(getActivity().getAssets(),  "fonts/WorkSans-Regular.ttf");
            ws_semibold    = Typeface.createFromAsset(getActivity().getAssets(), "fonts/WorkSans-SemiBold.ttf");




        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        //handler.removeCallbacks(progressbar_update);
        handler.removeCallbacks(runnable);
        //timer.cancel();
        //timer.purge();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(radioManager != null){
            if( radioManager.isPlaying() ){
                trigger.setBackground(getResources().getDrawable(R.color.transparente));
                trigger.setImageResource(R.color.transparente);

            }else{
                trigger.setBackground(getResources().getDrawable(R.drawable.ic_album_inside_circle));
                trigger.setImageResource(R.drawable.ic_play_arrow_black);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        //handler.removeCallbacks(progressbar_update);
        handler.removeCallbacks(runnable);
        timer.cancel();
        timer.purge();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //handler.removeCallbacks(progressbar_update);
        handler.removeCallbacks(runnable);
        timer.cancel();
        timer.purge();
    }




    /**
     * METADATA FETCHER
     */
    private class MetadataFetcher extends AsyncTask<URL, Void, CurrentMetadataFetcher> {

        @Override
        protected CurrentMetadataFetcher doInBackground(URL... urls) {
            CurrentMetadataFetcher fetchMetadata = new CurrentMetadataFetcher();


            try {
                //GET CURRENT DATE
                Date currentDateTime = new Date();
                int current_in_millis = (int) currentDateTime.getTime();


                //SAVING TRACK MODEL
                /*
                oneTrackModels.add(new TrackModel(trackTitle, album, title_artist));
                StorageUtil storage = new StorageUtil( getActivity().getApplicationContext() );
                storage.storeAudio( oneTrackModels);
                */

                //TODO: cambiar ulr a producción
                //FETCHING METADATA FROM URL
                //PROD
                lametadata = fetchMetadata.run("http://ec2-18-144-70-48.us-west-1.compute.amazonaws.com:8000/currentsong?sid=1");
                //DEV
                //lametadata = fetchMetadata.run("http://s45.myradiostream.com:13114/currentsong?sid=1");


                if(lametadata.equals("")){
                    //Viene sin metadata, no hacer nada . . .
                    artist_name = "Aire Libre";
                    track_name = "Aire Libre";
                    album_name = "Aire Libre";
                    duration = "1";
                    start_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                }else{
                    //Sí tiene metadata. Ejecutar toda la operación
                    meta_parts = lametadata.split("_-_");
                    //Log.d("indices", String.valueOf( meta_parts.length ));
                    if(meta_parts.length == 5){
                        artist_name = meta_parts[0];
                        track_name = meta_parts[1];
                        album_name = meta_parts[2];
                        duration = meta_parts[3];
                        start_time = meta_parts[4];
                    }else{
                        artist_name = meta_parts[0];
                        track_name = meta_parts[1];
                        album_name = meta_parts[2];
                        duration = "1";
                        start_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    }
                }


                //Get Cover art url
                //TODO: CAMBIAR URL DE LA IMAGEN A PRODUCCÓN
                //PROD
                cover_endpoint = "http://lisa.mx/airelibre/al_imagenes/art-00.jpg?";
                //DEV
                //cover_endpoint = "http://lisa.mx/airelibre/hk-images/art-00.jpg?";
                //cover_endpoint = setupCoverEndpoint();
                coverUrl = fetchMetadata.FetchCovers(cover_endpoint);


                //DATE OPERATIONS TO GET TRACK REMAINING TIME
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date startTime_date = formatter.parse(start_time);
                int startTime_in_millis = (int) startTime_date.getTime();


                /*Log.d("current_in_millis", String.valueOf( Math.abs(current_in_millis) ));
                Log.d("current_in_millis", String.valueOf( Math.abs(startTime_in_millis) ));*/


                duration_in_millis = Integer.parseInt(duration)*1000;
                tiempo_transcurrido = current_in_millis - startTime_in_millis;
                loquefalta = duration_in_millis - tiempo_transcurrido;

                isready = tiempo_transcurrido != 0;

                //SAVING DURATION, TRANSCURRIDO, LOQUE FALTA TO SHARED PREFERENCES
                if(getActivity() != null){
                    SharedPreferences sharedPreferences_trackinfo = getActivity().getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences_trackinfo.edit();
                    //CLEARING SHARED PREFERENCES
                    editor.clear().apply();
                    //PUTTING INT DURATION, INT TRANSCURRIDO, INT LO QUE FALTA TO SHARED PREFERENCES
                    editor.putInt("transcurrido", tiempo_transcurrido);
                    editor.putInt("loquefalta", loquefalta);
                    editor.putInt("duracion", duration_in_millis);
                    editor.apply();
                }


                //LOGGING DATA TO CONSOLE
                /*
                Log.d("artist", artist_name);
                Log.d("track", track_name);
                Log.d("album", album_name);
                Log.d("duracion", String.valueOf(duration_in_millis));
                Log.d("start", start_time);
                Log.d("transcurrido",String.valueOf(tiempo_transcurrido));
                Log.d("loquefalta > ", String.valueOf(loquefalta));
                */

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

            return null;
        }//END DOING IN BACKGROUND


        @Override
        protected void onPostExecute(CurrentMetadataFetcher result) {

            if(getActivity() !=  null){
                SharedPreferences cr_sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
                int cr_loquefalta =  cr_sharedPreferences.getInt("loquefalta", 2000);
                int cr_transcurrido = cr_sharedPreferences.getInt("transcurrido", 2000);
                int cr_duracion = cr_sharedPreferences.getInt("duracion", 2000);

                int topercent = cr_transcurrido*100/cr_duracion;

                mProgressBar.setColor( ContextCompat.getColor(getActivity(), R.color.bright_teal) );
                mProgressBar.setProgressBarWidth(10);
                mProgressBar.setProgress(topercent);
                mProgressBar.setProgressWithAnimation(100, Math.abs(cr_loquefalta) );

                //Log.d("Loquefalta_aqui >>", String.valueOf(  (cr_loquefalta - cr_transcurrido)  ));
            }


            String lbl_trackDuration = String.format(Locale.getDefault(), "%02d:%02d", (duration_in_millis/1000) / 60, (duration_in_millis/1000) % 60);
            String lbl_trackremain = String.format(Locale.getDefault(), "%02d:%02d", (tiempo_transcurrido/1000) / 60, (tiempo_transcurrido/1000) % 60);

            artistName.setText(artist_name);
            gotoPlaylist.setText(track_name);
            time_duration.setText(lbl_trackDuration);

            //time_remain.setText(lbl_trackremain);

            if(getActivity() != null){
                RequestOptions options = new RequestOptions();
                options.circleCrop();
                Glide.with(getActivity()).load(coverUrl).apply(options).into(coverart);
            }

        }//END ON POST EXECUTE
    }//END ASYNC TASK














    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String str_url);
    }
}
