package com.radiodedios.gt;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import com.radiodedios.gt.manager.LanguageManager;
import com.radiodedios.gt.manager.ThemeManager;

public class PolicyActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        LanguageManager langMgr = new LanguageManager(newBase);
        super.attachBaseContext(langMgr.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new ThemeManager(this).applyTheme();
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_policy);
        
        TextView title = findViewById(R.id.policyTitle);
        TextView text = findViewById(R.id.policyText);
        android.widget.Button btnClose = findViewById(R.id.btnClose);
        
        title.setText(R.string.privacy_policy);
        text.setText(R.string.policy_text);
        
        // Make links clickable if any
        text.setMovementMethod(LinkMovementMethod.getInstance());
        
        btnClose.setOnClickListener(v -> finish());
        
        // Handle Email Click specifically if needed, but LinkMovementMethod handles auto-link
    }
}
