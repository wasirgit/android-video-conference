package my.video.stream;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.widget.Toast;
import android.content.SharedPreferences;

import android.view.View;


public class streamActivity extends Activity
{
    Button btnOk;
	EditText txtIp;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
		btnOk=(Button)findViewById(R.id.btnOk);
		txtIp=(EditText)findViewById(R.id.txtIp);
				
		final SharedPreferences sp= getSharedPreferences("StreamApp",MODE_PRIVATE);
		txtIp.setText(sp.getString("SERVER_IP",""));
						
		btnOk.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent i=new Intent(streamActivity.this,Stream.class);
				
				i.putExtra("SERVER_IP",txtIp.getText().toString());
				Toast toast = Toast.makeText(streamActivity.this, "Streaming to " + txtIp.getText().toString(), Toast.LENGTH_SHORT);
				toast.show();
				SharedPreferences.Editor e=sp.edit();
				e.putString("SERVER_IP",txtIp.getText().toString());
				e.commit();
				startActivity(i);
			}
		});
	}
	
}