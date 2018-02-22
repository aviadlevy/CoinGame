package com.example.cp.cpcoingame;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
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
    private final int MOVE_DELTA = 50;
    private static final int PLAYER_X = 350;
    private static final int PLAYER_Y = 500;
    private static final int COIN_X = 100;
    private static final int COIN_Y = 100;
    private int moveX = MOVE_DELTA;
    private int moveY = 0;
    // timer
    public final int GAME_TIMER_START_VALUE = 15000;
    public final int GAME_TIMER_TICK_VALUE = 100;
    public final int NEW_GAME_DELAY = 300;
    private Timer timer = null;
    // display on screen
    private float timeRemaining;
    private int score;
    // settings
    boolean enableControls = false;
    boolean isSoundAllowed = false;
    // sounds
    private static final int LONG_VIBRATION = 500;
    private static final int SHORT_VIBRATION = 50;
    private boolean isSoundReady = false;
    private int soundID;
    private float maxVolume;
    private SoundPool soundPool;
    private MediaPlayer mp;
    private boolean isGameRunning;

    /////////////////////////////////////////////////
    //                 init                        //
    /////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        player = findViewById(R.id.robot);
        coin = findViewById(R.id.coin);
        RelativeLayout layout = findViewById(R.id.layoutGame);
        controlLayout = findViewById(R.id.layoutControl);
        controlLayout.post(() -> {
            getHeightControls();
            enableControlsLayout(enableControls);
        });
        layout.post(this::startNewGame);
        mDetector = new GestureDetectorCompat(this, new MySimpleGestureListener(this));
        initializeSoundPool();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuNewGame:
                startNewGame();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            timeRemaining -= GAME_TIMER_TICK_VALUE;
            TextView timeView = findViewById(R.id.txtTime);
            String message = "Time: " + (timeRemaining / 1000f);
            timeView.setText(message);
            // if time to finish the game
            if (timeRemaining <= 0) {
                gameEnded();
            }

            move();
            return true;
        }
    });

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
        timeRemaining = GAME_TIMER_START_VALUE;
        timer = new Timer();


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, NEW_GAME_DELAY, GAME_TIMER_TICK_VALUE);
        updateScore();
        performStartMusic();
    }

    private void updateScore() {
        score += 5;
        TextView textView = findViewById(R.id.txt_score);
        textView.setText(getString(R.string.txt_score) + " " + score);
    }

    private void gameEnded() {
        timer.cancel();
        switchButtonEnable(false);
        Toast.makeText(this, R.string.gameEnded, Toast.LENGTH_SHORT).show();
        // Get Vibrator from the current Context
        vibrate(LONG_VIBRATION);
        performStopMusic();
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
}
