package com.example.cp.cpcoingame;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final int MOVE_DELTA = 50;
    private final int PLAYER_X = 350;
    private final int PLAYER_Y = 350;
    private final int COIN_X = 50;
    private final int COIN_Y = 50;

    private int score;
    private ImageView coin;
    private ImageView player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        player = findViewById(R.id.robot);
        coin = findViewById(R.id.coin);
        score = 0;
    }

    public void moveUp(View view) {
        move(0, -MOVE_DELTA);
    }

    public void moveDown(View view) {
        move(0, MOVE_DELTA);
    }

    public void moveLeft(View view) {
        move(-MOVE_DELTA, 0);
        flipPlayer(-1);
    }

    public void moveRight(View view) {
        move(MOVE_DELTA, 0);
        flipPlayer(1);
    }

    private void flipPlayer(int direction) {
        player.setScaleX(direction);
    }

    public void move(int x, int y) {
        player.setX(player.getX() + x);
        player.setY(player.getY() + y);
        checkCollision();
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
            moveCoin();
            updateScore();
        }
    }

    private void updateScore() {
        score += 5;
        TextView textView = findViewById(R.id.txt_score);
        textView.setText(getString(R.string.txt_score) + " " + score);
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
            default:
                return super.onOptionsItemSelected(item);
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

    private void startNewGame() {
        player.setX(PLAYER_X);
        player.setY(PLAYER_Y);
        coin.setX(COIN_X);
        coin.setY(COIN_Y);
        score = -5;
        updateScore();
    }


}
