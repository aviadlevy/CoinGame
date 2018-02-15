package com.example.cp.cpcoingame;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final int MOVE_DELTA = 50;

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
}
