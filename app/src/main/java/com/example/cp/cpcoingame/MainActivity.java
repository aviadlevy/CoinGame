package com.example.cp.cpcoingame;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // views
    private ImageView coin;
    private ImageView player;
    private GestureDetectorCompat mDetector;
    public GridLayout controlLayout = null;
    private int controlLayoutHeight;
    // movement
    private final int MOVE_DELTA = 25;
    private static final int PLAYER_X = 350;
    private static final int PLAYER_Y = 500;
    private static final int COIN_X = 100;
    private static final int COIN_Y = 100;
    private int moveX = MOVE_DELTA;
    private int moveY = 0;
    // timer
    public int gameTimerStartValue = 15000;
    public final int GAME_TIMER_TICK_VALUE = 50;
    public final int NEW_GAME_DELAY = 300;
    private Timer timer = null;
    // display on screen
    private float timeRemaining;
    private int score;
    // settings
    boolean enableControls = false;
    boolean isSoundAllowed = false;
    private static final int CAMERA_PERMISSION = 1;
    private static final int GPS_PERMISSION = 2;
    // sounds
    private static final int LONG_VIBRATION = 500;
    private static final int SHORT_VIBRATION = 50;
    private boolean isSoundReady = false;
    private int soundID;
    private float maxVolume;
    private SoundPool soundPool;
    private MediaPlayer mp;
    private boolean isGameRunning;
    // camera
    private Camera mCamera = null;
    private ImageView snap = null;
    private int rotation = 0;
    // gps
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private String currentLocation;
    // animation
    private int mLongAnimationDuration = 0;

    /////////////////////////////////////////////////
    //                 init                        //
    /////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        player = findViewById(R.id.robot);
        coin = findViewById(R.id.coin);
        currentLocation = getString(R.string.default_current_location);
        RelativeLayout layout = findViewById(R.id.layoutGame);
        controlLayout = findViewById(R.id.layoutControl);
        controlLayout.post(() -> {
            getHeightControls();
            enableControlsLayout(enableControls);
        });
        layout.post(this::startNewGame);
        mDetector = new GestureDetectorCompat(this, new MySimpleGestureListener(this));
        initializeSoundPool();
        prepareGPSLocation();
        requestGPSLocation();
        mLongAnimationDuration = getResources().getInteger(android.R.integer.config_longAnimTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestGPSLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private void getHeightControls() {
        ViewTreeObserver vto = controlLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if (controlLayout.getMeasuredHeight() > 0) {

                    controlLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    controlLayoutHeight = controlLayout.getMeasuredHeight();
                }
            }
        });
    }

    private void initializeSoundPool() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // Load the sound
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener((soundPool1, sampleId, status) -> isSoundReady = true);
        soundID = soundPool.load(this, R.raw.coinsound, 1);
    }

    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    public Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (gameTimerStartValue != -1) {
                timeRemaining -= GAME_TIMER_TICK_VALUE;
                displayTimeRemaining(String.valueOf((timeRemaining / 1000f)));
            } else {
                displayTimeRemaining("Unlimited");
            }
            // if time to finish the game
            if (timeRemaining <= 0) {
                gameEnded();
            }
            move();
            return true;
        }
    });

    private void displayTimeRemaining(String timeRemaining) {
        TextView timeView = findViewById(R.id.txtTime);
        String message = "Time: " + timeRemaining;
        timeView.setText(message);
    }

    private boolean handlePermissions(String permissionsToCheck, int requestCode) {
        // check if we have needed permission
        int checkResult = ContextCompat.checkSelfPermission(this, permissionsToCheck);
        if (checkResult == PackageManager.PERMISSION_DENIED) {
            // Do we need an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permissionsToCheck)) {
                // Show window with rationale
                Toast.makeText(this, "I need permission", Toast.LENGTH_SHORT).show();
            }
            // Now ask for permission
            ActivityCompat.requestPermissions(this, new String[]{permissionsToCheck},
                    requestCode);
            return false; // we still don't have required permission. Try again later
        }
        return true; // we have required permission, can continue
    }

    /////////////////////////////////////////////////
    //             movement                        //
    /////////////////////////////////////////////////

    public void moveUp(View view) {
        setMoveDirection(0, -MOVE_DELTA);
    }

    public void moveDown(View view) {
        setMoveDirection(0, MOVE_DELTA);
    }

    public void moveLeft(View view) {
        setMoveDirection(-MOVE_DELTA, 0);
        flipPlayer(-1);
    }

    public void moveRight(View view) {
        setMoveDirection(MOVE_DELTA, 0);
        flipPlayer(1);
    }

    public void onSwipeRight() {
        setMoveDirection(MOVE_DELTA, 0);
        flipPlayer(1);
    }

    public void onSwipeLeft() {
        setMoveDirection(-MOVE_DELTA, 0);
        flipPlayer(-1);
    }

    public void onSwipeBottom() {
        setMoveDirection(0, MOVE_DELTA);
    }

    public void onSwipeTop() {
        setMoveDirection(0, -MOVE_DELTA);
    }

    private void setMoveDirection(int x, int y) {
        moveX = x;
        moveY = y;
    }

    public void move() {
        player.setX(player.getX() + moveX);
        player.setY(player.getY() + moveY);
        checkCollision();
        checkOutOfScreen();
    }

    public void moveY(int y) {
        player.setY(player.getY() + y);
        checkCollision();
    }

    private void flipPlayer(int direction) {
        player.setScaleX(direction);
    }

    private void checkCollision() {
        int[] location = new int[2];
        player.getLocationInWindow(location);
        Rect rectPlayer = new Rect(location[0],
                location[1], location[0] + player.getWidth(), location[1] +
                player.getHeight());
        coin.getLocationInWindow(location);
        Rect rectCoin = new Rect(location[0],
                location[1], location[0] + coin.getWidth(), location[1] +
                coin.getHeight());
        // collision is detected
        if (Rect.intersects(rectPlayer, rectCoin)) {
            // do collision action
            vibrate(SHORT_VIBRATION);
            performSound();
            addAnimatedCoin();
            moveCoin();
            updateScore();
        }
    }

    private void checkOutOfScreen() {
        View gameField = findViewById(R.id.layoutGame);
        int maxHeight = gameField.getHeight();
        int maxWidth = gameField.getWidth();
        if (player.getX() > maxWidth)
            player.setX(-MOVE_DELTA);
        else if (player.getX() < 0)
            player.setX(maxWidth + MOVE_DELTA);
        if (player.getY() > maxHeight)
            player.setY(-MOVE_DELTA);
        else if (player.getY() < 0)
            player.setY(maxHeight + MOVE_DELTA);
    }

    private void moveCoin() {
        Random randomGenerator = new Random();
        View gameField = findViewById(R.id.layoutGame);
        int maxHeight = gameField.getHeight();
        int maxWidth = gameField.getWidth();
        int x = randomGenerator.nextInt(maxWidth - coin.getWidth());
        int y = randomGenerator.nextInt(maxHeight - coin.getHeight());
        coin.setX(x);
        coin.setY(y);

    }

    /////////////////////////////////////////////////
    //             sounds                          //
    /////////////////////////////////////////////////

    private void performSound() {
        // Is the sound loaded
        if (isSoundReady && isSoundAllowed) {
            soundPool.play(soundID, maxVolume, maxVolume, 1, 0, 1.0f);
            soundID = soundPool.load(this, R.raw.coinsound, 1); //reload file
        }
    }

    private void performStartMusic() {
        performStopMusic();
        mp = MediaPlayer.create(this, R.raw.backgournd);
        // play in a loop
        mp.setOnCompletionListener(MediaPlayer::start);
        if (isGameRunning && isSoundAllowed) {
            mp.start();
        }
    }

    private void performStopMusic() {
        if (mp != null && mp.isPlaying())
            mp.stop();
    }

    private void vibrate(int length) {
        // Get Vibrator from the current Context
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(length);
        }
    }

    /////////////////////////////////////////////////
    //             settings/menu                   //
    /////////////////////////////////////////////////

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuNewGame:
                startNewGame();
                return true;
            case R.id.menuStopGame:
                gameEnded();
                return true;
            case R.id.menuShowAbout:
                showAboutScreen();
                return true;
            case R.id.menuShowWebPage:
                showWebPageScreen();
                return true;
            case R.id.menuToggleControls:
                toggleControls(item);
                return true;
            case R.id.menuToggleSound:
                toggleSound(item);
                return true;
            case R.id.menuTime5:
                return setGameTime(5000);
            case R.id.menuTime15:
                return setGameTime(15000);
            case R.id.menuTime30:
                return setGameTime(30000);
            case R.id.menuTime60:
                return setGameTime(60000);
            case R.id.menuTimeUnlimit:
                gameTimerStartValue = -1;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean setGameTime(int gameTimerStartValue) {
        if (!isGameRunning)
            this.gameTimerStartValue = gameTimerStartValue;
        else
            Toast.makeText(this, "Can't change time while game is running", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void toggleSound(MenuItem item) {
        isSoundAllowed = !isSoundAllowed;
        if (isSoundAllowed && isGameRunning) {
            performStartMusic();
        } else {
            performStopMusic();
        }
        item.setChecked(isSoundAllowed);
    }

    private void toggleControls(MenuItem item) {
        enableControls = !enableControls;
        enableControlsLayout(enableControls);
        item.setChecked(enableControls);
    }

    private void enableControlsLayout(boolean checked) {
        if (checked) {
            controlLayout.setVisibility(GridLayout.VISIBLE);
            moveY(controlLayoutHeight);
        } else {
            controlLayout.setVisibility(GridLayout.GONE);
            moveY(-controlLayoutHeight);
        }
    }

    private void showWebPageScreen() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://google.com/"));
        startActivity(intent);
    }

    private void showAboutScreen() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    /////////////////////////////////////////////////
    //             game logic                      //
    /////////////////////////////////////////////////

    private void startNewGame() {
        isGameRunning = true;
        player.setX(PLAYER_X);
        player.setY(PLAYER_Y);
        coin.setX(COIN_X);
        coin.setY(COIN_Y);
        score = -5;
        switchButtonEnable(true);
        if (timer != null) {
            timer.cancel();
        }
        if (gameTimerStartValue != -1) {
            timeRemaining = gameTimerStartValue;
        } else {
            timeRemaining = 1;
        }
        timer = new Timer();


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, NEW_GAME_DELAY, GAME_TIMER_TICK_VALUE);

        if (snap != null) {
            RelativeLayout layoutGame = findViewById(R.id.layoutGame);
            snap.setImageBitmap(null);
            layoutGame.removeView(snap);
            snap = null;
        }

        updateScore();
        performStartMusic();
    }

    private void updateScore() {
        score += 5;
        TextView textView = findViewById(R.id.txt_score);
        textView.setText(getString(R.string.txt_score) + " " + score);
    }

    private void gameEnded() {
        if (gameTimerStartValue != -1) {
            timer.cancel();
        }
        switchButtonEnable(false);
        Toast.makeText(this, R.string.gameEnded, Toast.LENGTH_SHORT).show();
        // Get Vibrator from the current Context
        vibrate(LONG_VIBRATION);
        performStopMusic();
        rotatePlayer();
        if (handlePermissions(Manifest.permission.CAMERA, CAMERA_PERMISSION)) {
            try {
                takeCameraPicture();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to take picture", Toast.LENGTH_SHORT).show();
            }
        }
        isGameRunning = false;
    }

    private void switchButtonEnable(boolean enable) {
        Button upBtn = findViewById(R.id.btnUp);
        Button downBtn = findViewById(R.id.btnDown);
        Button leftBtn = findViewById(R.id.btnLeft);
        Button rightBtn = findViewById(R.id.btnRight);

        upBtn.setEnabled(enable);
        downBtn.setEnabled(enable);
        leftBtn.setEnabled(enable);
        rightBtn.setEnabled(enable);
    }

    /////////////////////////////////////////////////
    //               camera                        //
    /////////////////////////////////////////////////

    private void takeCameraPicture() throws IOException {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            rotation = cameraInfo.orientation;
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                RelativeLayout layoutGame = findViewById(R.id.layoutGame);
                snap = new ImageView(this);
                layoutGame.addView(snap);
                mCamera = Camera.open(i); // get a Camera instance
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPictureSizes();
                Camera.Size size = sizes.get(0);
                for (int j = 0; j < sizes.size(); j++) {
                    if (sizes.get(j).width > 300 && sizes.get(j).width < 500) {
                        size = sizes.get(j);
                        break;
                    }
                }
                params.setPictureSize(size.width, size.height);
                mCamera.setParameters(params);
                SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
                mCamera.setPreviewTexture(st);
                mCamera.startPreview();
                mCamera.takePicture(null, null, mPicture);
                return;
            }
        }
    }

    private Camera.PictureCallback mPicture = (data, camera) -> {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0, data.length);
        // rotate image
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix(); // create a matrix for the manipulation
        matrix.postRotate(rotation);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width,
                height, matrix, true);
        // Set ImageView to contain picture taken
        snap.setImageBitmap(rotatedBitmap);
        // release objects to free memory
        data = null;
        mCamera.release();
        mCamera = null;
    };


    /////////////////////////////////////////////////
    //                 gps                         //
    /////////////////////////////////////////////////

    private void prepareGPSLocation() {
        locationManager = (LocationManager)
                this.getSystemService(Context.LOCATION_SERVICE);
        // Listener to handle incoming location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Executed when a location update is received
                newLocationReceived(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }

    private void newLocationReceived(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        StringBuilder builder = new StringBuilder();
        List<Address> address = null;
        try {
            address = geoCoder.getFromLocation(lat, lng, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (address != null && !address.isEmpty()) {
            int maxLines = address.get(0).getMaxAddressLineIndex();
            for (int i = 0; i < maxLines; i++) {
                String addressStr = address.get(0).getAddressLine(i);
                builder.append(addressStr);
                builder.append(" ");
            }
            currentLocation = builder.toString();
            updateLocationDisplay();
        }
    }

    private void updateLocationDisplay() {
        TextView txtLocation = findViewById(R.id.txtLocation);
        String message = getString(R.string.txt_location) + currentLocation;
        txtLocation.setText(message);
    }


    private void requestGPSLocation() {
        if (handlePermissions(Manifest.permission.ACCESS_FINE_LOCATION, GPS_PERMISSION)) {
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0, 0, locationListener);
            } catch (SecurityException e) {
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] results) {
        if ((results.length > 0) && requestCode == GPS_PERMISSION &&
                (results[0] == PackageManager.PERMISSION_GRANTED)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 0, locationListener);
            } catch (SecurityException e) {
            }
        }
    }

    /////////////////////////////////////////////////
    //               animations                    //
    /////////////////////////////////////////////////

    private void addAnimatedCoin() {
        RelativeLayout layoutGame = findViewById(R.id.layoutGame);
        ImageView coin = findViewById(R.id.coin);
        ImageView animatedCoin = new ImageView(this);
        animatedCoin.setImageResource(R.drawable.coin);
        animatedCoin.setX(coin.getX());
        animatedCoin.setY(coin.getY());
        layoutGame.addView(animatedCoin);
        animatedCoin.animate()
                .alpha(0f)
                .setDuration(mLongAnimationDuration)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) { }
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutGame.removeView(animatedCoin);
                    }
                    @Override
                    public void onAnimationCancel(Animator animation) { }
                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                });
    }

    private void rotatePlayer() {
        player.animate()
                .rotationBy(360f)
                .setDuration(mLongAnimationDuration)
                .setListener(null);
    }
}
