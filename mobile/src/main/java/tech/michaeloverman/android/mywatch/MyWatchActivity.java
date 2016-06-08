package tech.michaeloverman.android.mywatch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MyWatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mywatch_config);
    }


    void hourButtonClicked(View v) {
        Toast.makeText(MyWatchActivity.this, "Hour Button Clicked", Toast.LENGTH_SHORT).show();
    }
}
