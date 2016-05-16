package com.example.myobotcontrol;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

public class MainActivity extends Activity {
	// Control buttons
	Button cnctButton;
	Button squareButton;
	Button circleButton;
	ImageButton topArrow;
	ImageButton bottomArrow;
	ImageButton leftArrow;
	ImageButton rightArrow;
	ImageButton playButton;
	SeekBar powerBar; 
	
	// NTX control
	static final byte DIRECT_CMD_RES = 0x00;
	static final byte CTRL_MOTORS = 0x04;
	static final byte PLAY_TONE = 0x03;
	static final byte GET_BATTERY_LEVEL = 0x0B;
	static final byte A_PORT = 0x00;
	static final byte B_PORT = 0x01;
	static final byte C_PORT = 0x02;
	static final byte TURN_ON_MOTORS = 0x01;
	static final byte NO_REG = 0x00;
	static final byte SPEED_REG = 0x01;
	static final byte SYNC_REG = 0x02;
	static final byte NO_TURNING = 0x00;
	static final byte RUNNING_STATE = 0x20;
	static final byte RUN_FOREVER = 0x00;
	
	// State text view
	TextView stateView;
	TextView battView;
	
	// Use Blutooth class
	BT_Comm bt;
	
	boolean action = false;
	int speed = 60; // Initial setting of speed
	private Activity activity = this;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		cnctButton = (Button) findViewById(R.id.cnctButton);
		squareButton = (Button) findViewById(R.id.squareButton);
		circleButton = (Button) findViewById(R.id.circleButton);
		topArrow = (ImageButton) findViewById(R.id.topArrow);
		bottomArrow = (ImageButton) findViewById(R.id.bottomArrow);
		leftArrow = (ImageButton) findViewById(R.id.leftArrow);
		rightArrow = (ImageButton) findViewById(R.id.rightArrow);
		playButton = (ImageButton) findViewById(R.id.playButton);
		stateView = (TextView) findViewById(R.id.stateView);
		battView = (TextView) findViewById(R.id.battView);
		powerBar = (SeekBar) findViewById(R.id.powerBar);
		
		enableCtrlButtons(false);
		
		powerBar.setProgress(speed);
		powerBar.incrementProgressBy(1);
		powerBar.setMax(100);
		
		powerBar.setOnSeekBarChangeListener(powerBarChangeListener);
		playButton.setOnClickListener(playButtonClickListener);
		cnctButton.setOnClickListener(cnctButtonClickListener);
		topArrow.setOnTouchListener(topArrowTouchListener);
		topArrow.setOnClickListener(topArrowClickListener);
		bottomArrow.setOnTouchListener(bottomArrowTouchListener);
		bottomArrow.setOnClickListener(bottomArrowClickListener);
		leftArrow.setOnTouchListener(leftArrowTouchListener);
		leftArrow.setOnClickListener(leftArrowClickListener);
		rightArrow.setOnTouchListener(rightArrowTouchListener);
		rightArrow.setOnClickListener(rightArrowClickListener);
		squareButton.setOnClickListener(squareButtonListener);
		circleButton.setOnClickListener(circleButtonListener);
		
		// Bluetooth com
		bt = new BT_Comm();
	}
	
	private void moveForward() {
		if (!action) {
			runMotors(speed, SYNC_REG, speed, SYNC_REG);
			action = true;
		}
	}
	
	private void moveBackward() {
		if (!action) {
			runMotors(-speed, SYNC_REG, -speed, SYNC_REG);
			action = true;
		}
	}
	
	private void turnLeft() {
		if (!action) {
			runMotors(-(int)(speed/2), SPEED_REG, (int)(speed/2), SPEED_REG);
			action = true;
		}
	}
	
	private void turnRight() {
		if (!action) {
			runMotors((int)(speed/2), SPEED_REG, -(int)(speed/2), SPEED_REG);
			action = true;
		}
	}
	
	private void stop() {
		runMotors(0, NO_REG, 0, NO_REG);
		action = false;
	}
	
	private void playSound() {
		byte buffer[] = {0x06, 0x00, DIRECT_CMD_RES, PLAY_TONE, 0x40, 0x02, 0x64, 0x00};
		byte buffer2[] = {0x06, 0x00, DIRECT_CMD_RES, PLAY_TONE, 0x70, 0x02, 0x64, 0x00};
		try {
			byte motor3[] = {0x0C, 0x00, DIRECT_CMD_RES, CTRL_MOTORS, A_PORT, (byte) 100, TURN_ON_MOTORS, NO_REG, NO_TURNING, RUNNING_STATE, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER};
			
			try {
				bt.writeMessage(motor3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			bt.writeMessage(buffer);
			checkAnswer(bt.readMessage(5));
			Thread.sleep(100);
			
			bt.writeMessage(buffer);
			checkAnswer(bt.readMessage(5));
			Thread.sleep(100);
			
			bt.writeMessage(buffer);
			checkAnswer(bt.readMessage(5));
			Thread.sleep(100);
			
			bt.writeMessage(buffer2);
			checkAnswer(bt.readMessage(5));
			Thread.sleep(300);
			
			bt.writeMessage(buffer);
			checkAnswer(bt.readMessage(5));
			Thread.sleep(100);
			
			bt.writeMessage(buffer2);
			checkAnswer(bt.readMessage(5));
			
			byte motor3_stop[] = {0x0C, 0x00, DIRECT_CMD_RES, CTRL_MOTORS, A_PORT, (byte) 0, TURN_ON_MOTORS, NO_REG, NO_TURNING, RUNNING_STATE, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER};
			
			try {
				bt.writeMessage(motor3_stop);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean checkAnswer(byte buffer[]) {
		boolean res = true;
		
		if (buffer[0] == 0) { // Connection lost
			res = false;

			cnctButton.setEnabled(true);

			enableCtrlButtons(false);
			
			stateView.setTextColor(Color.RED);
			stateView.setText("État : robot déconnecté");
		}
		
		return res;
	}

	public void enableCtrlButtons(boolean state) {
		topArrow.setEnabled(state);
		bottomArrow.setEnabled(state);
		leftArrow.setEnabled(state);
		rightArrow.setEnabled(state);
		playButton.setEnabled(state);
		squareButton.setEnabled(state);
		circleButton.setEnabled(state);
	}
	
	public void runMotors(int speed1, byte reg1, int speed2, byte reg2) {
		byte motor1[] = {0x0C, 0x00, DIRECT_CMD_RES, CTRL_MOTORS, B_PORT, (byte) speed1, TURN_ON_MOTORS, reg1, NO_TURNING, RUNNING_STATE, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER};
		byte motor2[] = {0x0C, 0x00, DIRECT_CMD_RES, CTRL_MOTORS, C_PORT, (byte) speed2, TURN_ON_MOTORS, reg2, NO_TURNING, RUNNING_STATE, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER, RUN_FOREVER};
		
		try {
			bt.writeMessage(motor1);

			if (checkAnswer(bt.readMessage(5))) { // Continue only if there is an answer
				bt.writeMessage(motor2);
				checkAnswer(bt.readMessage(5));
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private OnSeekBarChangeListener powerBarChangeListener = new OnSeekBarChangeListener() {

	    @Override       
	    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {     
	        speed = powerBar.getProgress();
	    }

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}       
	};
	
	private OnClickListener cnctButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : connexion au robot...");
			
			new Handler().postDelayed(new Runnable() {
				public void run() {
					/* Connexion au NXT */

					bt.enableBT();

					if (bt.connectToNXT()) {
						stateView.setTextColor(Color.GREEN);
						stateView.setText("État : robot connecté");
						
						new Handler().postDelayed(new Runnable() {
							public void run() {
								/* Connexion au MYO */
								
								stateView.setTextColor(Color.rgb(40, 146, 194));
								stateView.setText("État : connexion au Myo...");
								
								Hub hub = Hub.getInstance();
					
								// Initialisation du HUB de gestion du Myo
								if (!hub.init(activity)) {
									stateView.setTextColor(Color.RED);
									stateView.setText("État : échec d'initialisation du hub");
								    finish();
								    return;
								} else {
									hub.addListener(mListener);
									
									// Connexion au premier MYO trouvé
									hub.attachToAdjacentMyo();
								}
				
								cnctButton.setEnabled(false);
				
								enableCtrlButtons(true);
								
								/* Récupération et affichage de la tension de la batterie du robot */
								
								byte buffer[] = {0x02, 0x00, DIRECT_CMD_RES, GET_BATTERY_LEVEL};
				
								try {
									bt.writeMessage(buffer);
									buffer = new byte[7];
									buffer = bt.readMessage(7);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
								battView.setTextColor(Color.rgb(40, 146, 194));
								battView.setText(String.format( "Ubat : %.2fV", (((float) (buffer[5] + (buffer[6] << 8))) / 1000)));
							}
						}, 1000);
		
					} else { // Échec de connexion au NXT
						stateView.setTextColor(Color.RED);
						stateView.setText("État : robot introuvable");
					}
				}
			}, 1);
		}
	};
			
	private OnTouchListener topArrowTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->avancer");

			moveForward();
			return false;
		}
	};

	private OnClickListener topArrowClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->arrêter");

			stop();
		}
	};

	private OnTouchListener bottomArrowTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->reculer");

			moveBackward();
			return false;
		}
	};
	
	private OnClickListener bottomArrowClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->arrêter");

			stop();
		}
	};

	private OnTouchListener leftArrowTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->tourner_gauche");

			turnLeft();
			return false;
		}
	};

	private OnClickListener leftArrowClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->arrêter");

			stop();
		}
	};

	private OnTouchListener rightArrowTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->tourner_droite");

			turnRight();
			return false;
		}
	};

	private OnClickListener rightArrowClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->arrêter");

			stop();
		}
	};
	

	OnClickListener playButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stateView.setTextColor(Color.rgb(40, 146, 194));
			stateView.setText("État : commande->jouer_son");

			playSound();
		}
	};

	private OnClickListener squareButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			
			try {
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : commande->carré");
				
				runMotors(40, SYNC_REG, 40, SYNC_REG);
				
				Thread.sleep(2000);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(20, SPEED_REG, -20, SPEED_REG);
				
				Thread.sleep(900);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(40, SYNC_REG, 40, SYNC_REG);
				
				Thread.sleep(2000);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(20, SPEED_REG, -20, SPEED_REG);
				
				Thread.sleep(900);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(40, SYNC_REG, 40, SYNC_REG);
				
				Thread.sleep(2000);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(20, SPEED_REG, -20, SPEED_REG);
				
				Thread.sleep(900);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(40, SYNC_REG, 40, SYNC_REG);
				
				Thread.sleep(2000);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
				Thread.sleep(100);
				
				runMotors(20, SPEED_REG, -20, SPEED_REG);
				
				Thread.sleep(900);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			action = false;
		}
	};

	private OnClickListener circleButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			
			try {
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : commande->cercle");
				
				runMotors(50, SYNC_REG, 30, SYNC_REG);
				
				Thread.sleep(7800);
				
				runMotors(0, NO_REG, 0, NO_REG);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			action = false;
		}
	};
	
	private DeviceListener mListener = new AbstractDeviceListener() {
	    @Override
	    public void onConnect(Myo myo, long timestamp) {
        	stateView.setTextColor(Color.GREEN);
	        stateView.setText("État : Myo connecté");
	    }

	    @Override
	    public void onDisconnect(Myo myo, long timestamp) {
	    	stateView.setTextColor(Color.RED);
	    	stateView.setText("État : Myo déconnecté");
	    }

		@Override
		public void onPose(Myo myo, long timestamp, Pose pose) {
			switch (pose) {
			case FINGERS_SPREAD:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->FINGERS_SPREAD->avancer");
				
				moveForward();
				break;
			case FIST:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->FIST->reculer");
				
				moveBackward();
				break;
			case REST:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->REST->arrêter");
				
				stop();
				break;
			case THUMB_TO_PINKY:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->THUMB_TO_PINKY->jouer_son");
				break;
			case UNKNOWN:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->UNKNOWN->attente_synchro");
				break;
			case WAVE_IN:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->WAVE_IN->tourner_gauche");
				
				turnLeft();
				break;
			case WAVE_OUT:
				stateView.setTextColor(Color.rgb(40, 146, 194));
				stateView.setText("État : myo->WAVE_OUT->tourner_droite");
				
				turnRight();
				break;
			default:
				stop();
				break;
			}
		}
	};
}