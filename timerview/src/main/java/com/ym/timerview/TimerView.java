package com.ym.timerview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by ym on 16-9-1.
 */
public class TimerView extends View {

    // attributes

    private enum STATE {
        FREE,
        COUNTDOWN,
        SETTING,
    }
    private enum COLOR {
        RED,
        BLUE,
    }

    private Paint p;

    private WanderPoint wanderPoint = new WanderPoint();
    private LittleStickGroup littleStickGroup = new LittleStickGroup();

    private boolean isTouching = false;
    private float touchX = 0;
    private float touchY = 0;

    private double finalSettingAngle = Math.PI*2/60;
    private double protentialSettingAngle = Math.PI*2/60;

    private boolean isTouchingPoint = false;
    private long startTouchPointMillis = 0;
    private boolean isWaitForUp = false;

    private STATE state = STATE.FREE;

    private long startMillis = 0;
    private long countDownInMillis = 60000;

    private float width = 0;
    private float height = 0;
    private float minInWidthAndHeight = 0;

    private RectF oval = new RectF();

    private Context mContext;

    private SensorManager sensorManager;
    private Sensor gravitySensor;

    private ShakeJudger shakeJudgerWithTouch = new ShakeJudger(12, 300, 1);

    private float maxShakeX = 0;
    private float maxShakeY = 0;
    private int shakeEndLock = 0;

    private boolean countDownEnd = false;

    private OnStartCountDownListener onStartCountDownListener;
    private OnEndCountDownListener onEndCountDownListener;
    private OnCancelCountDownListener onCancelCountDownListener;
    private OnBallClickListener onBallClickListener;
    private OnBallLongClickListener onBallLongClickListener;
    private OnStartInteractiveSettingListener onStartInteractiveSettingListener;
    private OnConfirmInteractiveSettingListener onConfirmInteractiveSettingListener;
    private OnCancelInteractiveSettingListener onCancelInteractiveSettingListener;

    private boolean gravityEnable = false;
    private boolean shakeEnable = false;
    private boolean ballClickEnable = false;
    private boolean defaultStartSettingEnable = false;
    private boolean defaultStartCountDownEnable = false;

    // constructors

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        p = new Paint();
        p.setColor(0xFF1E90FF);// 设置颜色
        p.setAntiAlias(true);

        mContext = context;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // getters and setters

    public long startMillis() { return startMillis; }
    public long countDownInMillis() { return countDownInMillis; }

    // public methods

    public void startCountDown(long millis) {
        startMillis = System.currentTimeMillis();
        countDownInMillis = millis;

        state = STATE.COUNTDOWN;

        if (onStartCountDownListener != null) {
            onStartCountDownListener.onStartCountDown(this);
        }
    }
    public void cancelCountDown() {
        state = STATE.FREE;

        if (onCancelCountDownListener != null) {
            onCancelCountDownListener.onCancelCountDown(this);
        }
    }

    public void setCountDownInMillis(long countDownInMillis) {
        this.countDownInMillis = countDownInMillis;
    }

    public void setGravityEnable(boolean enable) {
        gravityEnable = enable;
    }
    public void setShakeEnable(boolean shakeEnable) {
        this.shakeEnable = shakeEnable;
    }
    public void setBallClickEnable(boolean enable) {
        ballClickEnable = enable;
    }
    public void setDefaultStartSettingEnable(boolean enable) {
        defaultStartSettingEnable = enable;
    }
    public void setDefaultStartCountDownEnable(boolean enable) {
        defaultStartCountDownEnable = enable;
    }

    public void setOnStartCountDownListener(OnStartCountDownListener listener) {
        onStartCountDownListener = listener;
    }
    public void setOnEndCountDownListener(OnEndCountDownListener listener) {
        onEndCountDownListener = listener;
    }
    public void setOnCancelCountDownListener(OnCancelCountDownListener listener) {
        onCancelCountDownListener = listener;
    }
    public void setOnBallClickListener(OnBallClickListener listener) {
        onBallClickListener = listener;
    }
    public void setOnBallLongClickListener(OnBallLongClickListener listener) {
        onBallLongClickListener = listener;
    }
    public void setOnStartInteractiveSettingListener(OnStartInteractiveSettingListener listener) {
        this.onStartInteractiveSettingListener = listener;
    }
    public void setOnConfirmInteractiveSettingListener(OnConfirmInteractiveSettingListener listener) {
        this.onConfirmInteractiveSettingListener = listener;
    }
    public void setOnCancelInteractiveSettingListener(OnCancelInteractiveSettingListener listener) {
        this.onCancelInteractiveSettingListener = listener;
    }

    public void registerGravitySensor() {
        sensorManager.registerListener(sensorEventListener, gravitySensor, SensorManager.SENSOR_DELAY_UI);
    }
    public void unregisterGravitySensor() {
        sensorManager.unregisterListener(sensorEventListener, gravitySensor);
    }

    public void storeState() {

        SharedPreferences sharedPreferences = mContext.getSharedPreferences("restore", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (state() == TimerView.STATE.COUNTDOWN) {
            editor.putBoolean("isNeed", true);
            editor.putLong("startMillis", startMillis());
        }
        else {
            editor.putBoolean("isNeed", false);
        }

        editor.putLong("countDownInMillis", countDownInMillis());
        editor.putString("finalSettingAngle", finalSettingAngle()+"");

        editor.commit();

    }
    public void restoreState() {

        SharedPreferences sharedPreferences = mContext.getSharedPreferences("restore", Context.MODE_PRIVATE);
        boolean isNeed = sharedPreferences.getBoolean("isNeed", false);
        if (isNeed) {
            long startMillis = sharedPreferences.getLong("startMillis", -1);
            if (startMillis != -1) {
                restore(startMillis, TimerView.STATE.COUNTDOWN);
            }
        }

        countDownInMillis = (sharedPreferences.getLong("countDownInMillis", 60000));
        finalSettingAngle = Double.parseDouble(sharedPreferences.getString("finalSettingAngle", finalSettingAngle()+""));
    }

    public void cancelInteractiveSetting() {
        state = STATE.FREE;

        if (onCancelInteractiveSettingListener != null) {
            onCancelInteractiveSettingListener.onCancelInteractiveSetting(this);
        }
    }

    public boolean isSettingState() {
        return state == STATE.SETTING;
    }

    // private methods

    private void startInteractiveSetting() {
        state = STATE.SETTING;
        wanderPoint.x = width/2;
        wanderPoint.y = height/2;

        protentialSettingAngle = finalSettingAngle;

        if (onStartInteractiveSettingListener != null) {
            onStartInteractiveSettingListener.onStartInteractiveSetting(this);
        }
    }

    private void confirmInteractiveSetting(long millis) {
        Log.i("minute", millis/1000/60+"");
        state = STATE.FREE;
        countDownInMillis = millis;

        if (onConfirmInteractiveSettingListener != null) {
            onConfirmInteractiveSettingListener.onConfirmInteractiveSetting(this);
        }
    }

    private void setAcc(float x, float y) {
        wanderPoint.setAcc(x, y);
    }
    private void rushXY(float x, float y) {
        wanderPoint.rushXY(x, y);
    }

    private void restore(long startMillis, STATE state) {
        this.startMillis = startMillis;
        this.state = state;
    }

    private double finalSettingAngle() { return finalSettingAngle; }
    private STATE state() { return state; }
    private boolean isTouching() { return isTouching; }

    // override methods

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (width == 0 || height == 0) {
            width = canvas.getWidth();
            height = canvas.getHeight();

            minInWidthAndHeight = width < height ? width : height;

            wanderPoint.x = width / 2;
            wanderPoint.y = height / 2;
            p.setTextSize(minInWidthAndHeight/4);
            p.setTextAlign(Paint.Align.CENTER);
        } else {
            width = canvas.getWidth();
            height = canvas.getHeight();
        }

        wanderPoint.adaptWindow(canvas.getWidth(), canvas.getHeight());
        wanderPoint.drawSelf(canvas, p, state);
        wanderPoint.moveInPhysics();

        littleStickGroup.adaptWindow(canvas.getWidth(), canvas.getHeight());
        littleStickGroup.affectByWanderPoint(wanderPoint, state);
        littleStickGroup.drawSelf(canvas, p);

        if (isTouchingPoint) {
            long current = System.currentTimeMillis();
            if (current - startTouchPointMillis > 1500) {
                if (!isWaitForUp) {

                    Log.i("ss", "LC");
                    if (onBallLongClickListener != null) {
                        onBallLongClickListener.onBallLongClick();
                    }

                    if (defaultStartSettingEnable) {
                        if (state == STATE.FREE) {
                            startInteractiveSetting();
                        }
                        else if (state == STATE.SETTING) {
                            Log.i("finalSettingAngle", (finalSettingAngle / (2 * Math.PI) * 360) + "");
                            finalSettingAngle = protentialSettingAngle;
                            confirmInteractiveSetting((long) (((finalSettingAngle / (2 * Math.PI)) * 360 / 6) * 60 * 1000));
                        }
                    }
                    isWaitForUp = true;
                }
            }
        }

        if (state == STATE.SETTING) {

            if (isTouching) {

                float x = touchX;
                float y = touchY;

                if (Math.sqrt((x-wanderPoint.x)*(x-wanderPoint.x)+(y-wanderPoint.y)*(y-wanderPoint.y)) > 4*wanderPoint.radius) {

                    int quadrant = 0;
                    if (x - width / 2 >= 0 && y - height / 2 >= 0) {
                        quadrant = 1;
                    } else if (x - width / 2 <= 0 && y - height / 2 >= 0) {
                        quadrant = 2;
                    } else if (x - width / 2 >= 0 && y - height / 2 <= 0) {
                        quadrant = 4;
                    } else if (x - width / 2 <= 0 && y - height / 2 <= 0) {
                        quadrant = 3;
                    }

                    double angle = Math.atan((y - height / 2) / (x - width / 2));
//                Log.i("angle", angle+"");
                    double angle_in_2PI = angle;
                    if (quadrant == 2) {
                        angle_in_2PI = angle + Math.PI;
                    } else if (quadrant == 3) {
                        angle_in_2PI = angle + Math.PI;
                    } else if (quadrant == 4) {
                        angle_in_2PI = angle + 2 * Math.PI;
                    }
//                Log.i("angle_in_2PI", angle_in_2PI+"");
                    protentialSettingAngle = angle_in_2PI + Math.PI / 2;
//                Log.i("settingAngle", settingAngle+"");
                    if (protentialSettingAngle > 2 * Math.PI) {
                        protentialSettingAngle -= 2 * Math.PI;
                    }
                }
            }

            if (protentialSettingAngle >= 0 && protentialSettingAngle < 2 * Math.PI / 60) {
                protentialSettingAngle = 2 * Math.PI;
            }

            float ovalR = (float) Math.sqrt((width/2)*(width/2)+(height/2)*(height/2));
            float offsetX = ovalR - canvas.getWidth()/2;
            float offsetY = ovalR - canvas.getHeight()/2;

            oval.set(0 - offsetX, 0 - offsetY, ovalR * 2 - offsetX, ovalR * 2 - offsetY);

            float xPos = (canvas.getWidth() / 2);
            float yPos = (height / 2) - ((p.descent() + p.ascent()) / 2);
            float textWidth = p.measureText((int) ((protentialSettingAngle / (2 * Math.PI))*360/6) + "");
            float charWidth = p.measureText("8");
            p.setColor(0x55000000);
            canvas.drawArc(oval, -90, (float) (protentialSettingAngle / (2 * Math.PI) * 360), true, p);
            p.setColor(0xFF1E90FF);
            p.setColor(0x99FF0000);
            canvas.drawText((int) ((protentialSettingAngle / (2 * Math.PI)) * 360 / 6) + "", xPos, yPos, p);
            canvas.drawText("'", xPos + textWidth / 2 + charWidth / 2, yPos, p);
            p.setColor(0xFF1E90FF);
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            isTouching = true;
            touchX = event.getX();
            touchY = event.getY();

            if (ballClickEnable) {
                if (Math.sqrt((event.getX() - wanderPoint.x) * (event.getX() - wanderPoint.x) + (event.getY() - wanderPoint.y) * (event.getY() - wanderPoint.y)) < 2 * wanderPoint.radius) {

                    if (onBallClickListener != null) {
                        onBallClickListener.onBallClick();
                    }

                    wanderPoint.scaleOnce();

                    isTouchingPoint = true;
                    startTouchPointMillis = System.currentTimeMillis();
                }
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            isWaitForUp = false;
            isTouchingPoint = false;

            isTouching = false;

            wanderPoint.enableSmall();

            Log.i("UP", "ACTION_UP");
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            touchX = event.getX();
            touchY = event.getY();
        }

        return true;
    }

    // private classes

    private class WanderPoint {
        private float boundWidth = 0;
        private float boundHeight = 0;
        private float minInWidthAndHeight = 0;

        private float radius = 0;

        private float scale = 1;
        private boolean scaleLock = false;
        private boolean scaleLarge = true;
        private boolean canSmall = false;

        private float x = 0;
        private float y = 0;
        private float v_x = 0;
        private float v_y = 0;
        private float acc_x = 0;
        private float acc_y = 0;
        private float f = 0.1f;

        public void setAcc(float x, float y) {
            acc_x = x;
            acc_y = y;
        }
        public void rushXY(float x, float y) {
            v_x = x;
            v_y = y;
        }
        public void moveInPhysics() {

            if (state == STATE.FREE) {

                if (x < radius) {
                    x = radius;
                    v_x = -v_x;
                } else if (x > boundWidth - radius) {
                    x = boundWidth - radius;
                    v_x = -v_x;
                }
                if (y < radius) {
                    y = radius;
                    v_y = -v_y;
                } else if (y > boundHeight - radius) {
                    y = boundHeight - radius;
                    v_y = -v_y;
                }

                x += v_x;
                y += v_y;

                v_x += acc_x;
                v_y += acc_y;

                double V = Math.sqrt(v_x * v_x + v_y * v_y);
                if (V >= 5 * f) {
                    double scaler = (V - f) / V;
                    v_x *= scaler;
                    v_y *= scaler;
                }

                scaling();

            }
            else if (state == STATE.COUNTDOWN) {

                long currentMillis = System.currentTimeMillis();
                double angle = 2*Math.PI*(currentMillis-startMillis)/(countDownInMillis);

                float inner_radius = minInWidthAndHeight / 3;
                if (angle < 2*Math.PI) {
                    x = (float) (boundWidth / 2 + Math.cos(angle - Math.PI/2) * (inner_radius - 2 * radius));
                    y = (float) (boundHeight / 2 + Math.sin(angle - Math.PI/2) * (inner_radius - 2 * radius));

                    countDownEnd = false;
                }
                else {
                    x = (float) (boundWidth / 2 + Math.cos(2*Math.PI - Math.PI/2) * (inner_radius - 2 * radius));
                    y = (float) (boundHeight / 2 + Math.sin(2*Math.PI - Math.PI/2) * (inner_radius - 2 * radius));

                    if (!countDownEnd) {
                        countDownEnd = true;

                        if (onEndCountDownListener != null){
                            onEndCountDownListener.onEndCountDown(TimerView.this);
                        }
                    }
                }

            }
            else if (state == STATE.SETTING) {

                scaling();

            }

        }
        public void scaleOnce() {
            scaleLock = true;
        }
        public void enableSmall() {
            canSmall = true;
        }

        private void scaling() {
//            Log.i("canSmall", canSmall+"");
            if (scaleLock) {
                if (scaleLarge) {
                    if (scale < 1.5) {
                        scale += 0.1;
                    }
                    else {
                        scaleLarge = false;
                    }
                }
                else {
                    if (canSmall) {
                        if (scale > 1) {
                            scale -= 0.1;
                        } else {
                            scale = 1;
                            scaleLock = false;
                            scaleLarge = true;
                            canSmall = false;
                        }
                    }
                }
            }
            else {
                scale = 1;
                scaleLock = false;
                scaleLarge = true;
                canSmall = false;

            }
        }
        private void drawSelf(Canvas canvas, Paint p, STATE state) {
            if (state == STATE.FREE) {
                canvas.drawCircle(x, y, radius*scale, p);// 小圆
            }
            else if (state == STATE.COUNTDOWN) {
                p.setColor(Color.RED);
                canvas.drawCircle(x, y, radius, p);// 小圆
                p.setColor(0xFF1E90FF);
            }
            else if (state == STATE.SETTING) {
                canvas.drawCircle(x, y, radius*scale, p);// 小圆
            }
        }

        public void adaptWindow(float windowWidth, float windowHeight) {
            this.boundWidth = windowWidth;
            this.boundHeight = windowHeight;
            this.minInWidthAndHeight = windowWidth < windowHeight ? windowWidth : windowHeight;

            this.f = minInWidthAndHeight/4000;
            this.radius = minInWidthAndHeight/25;
        }
    }

    private class LittleStick {
        private COLOR color = COLOR.BLUE;
        private float sx = 0;
        private float sy = 0;
        private float ex = 0;
        private float ey = 0;
        private LittleStick() {}
        private LittleStick(float sx, float sy, float ex, float ey) {
            this.sx = sx;
            this.sy = sy;
            this.ex = ex;
            this.ey = ey;
        }
        private void drawSelf(Canvas canvas, Paint p) {
            if (color == COLOR.BLUE) {
                canvas.drawLine(sx, sy, ex, ey, p);
            }
            else {
                p.setColor(Color.RED);// 设置红色
                canvas.drawLine(sx, sy, ex, ey, p);
                p.setColor(0xFF1E90FF);
            }
        }
    }

    private class LittleStickGroup {
        private float boundWidth = 0;
        private float boundHeight = 0;
        private float minInWidthAndHeight = 0;

        private LittleStick [] sticks = new LittleStick[120];

        public LittleStickGroup() {
            for (int i = 0; i < sticks.length; ++i) {
                sticks[i] = new LittleStick();
            }
        }
        public void affectByWanderPoint(WanderPoint wanderPoint, STATE state) {
            float inner_radius = minInWidthAndHeight/3;
            float outer_radius = inner_radius+minInWidthAndHeight/20;
            double unit_angle = 2*Math.PI/sticks.length;

            float x = wanderPoint.x;
            float y = wanderPoint.y;

            int quadrant = 0;
            if (x-boundWidth/2 >= 0 && y-boundHeight/2 >= 0) {
                quadrant = 1;
            }
            else if (x-boundWidth/2 <= 0 && y-boundHeight/2 >= 0) {
                quadrant = 2;
            }
            else if (x-boundWidth/2 >= 0 && y-boundHeight/2 <= 0) {
                quadrant = 4;
            }
            else if (x-boundWidth/2 <= 0 && y-boundHeight/2 <= 0) {
                quadrant = 3;
            }

            double angle = Math.atan((y-boundHeight/2)/(x-boundWidth/2));
            double angle_in_2PI = angle;
            if (quadrant == 2) {
                angle_in_2PI = angle + Math.PI;
            }
            else if (quadrant == 3) {
                angle_in_2PI = angle + Math.PI;
            }
            else if (quadrant == 4) {
                angle_in_2PI = angle + 2*Math.PI;
            }

            int affect_count = 12;
            for (int i = 0; i < sticks.length; ++i) {
                sticks[i].sx = (float) (Math.cos(unit_angle * i) * inner_radius + boundWidth / 2);
                sticks[i].sy = (float) (Math.sin(unit_angle * i) * inner_radius + boundHeight / 2);
                sticks[i].ex = (float) (Math.cos(unit_angle * i) * outer_radius + boundWidth / 2);
                sticks[i].ey = (float) (Math.sin(unit_angle * i) * outer_radius + boundHeight / 2);

                if (unit_angle*i - angle_in_2PI <= unit_angle*affect_count && unit_angle*i - angle_in_2PI >= -unit_angle*affect_count
                        || unit_angle*i - angle_in_2PI >= unit_angle*(sticks.length-affect_count)
                        || unit_angle*i - angle_in_2PI <= -unit_angle*(sticks.length-affect_count)) {

                    double dis = Math.abs(angle_in_2PI - unit_angle*i);

                    if (unit_angle*i - angle_in_2PI >= unit_angle*(sticks.length-affect_count)) {
                        dis = Math.abs(unit_angle*i - angle_in_2PI - 2*Math.PI);
                    }
                    else if (unit_angle*i - angle_in_2PI <= -unit_angle*(sticks.length-affect_count)) {
                        dis = Math.abs(unit_angle*i - angle_in_2PI + 2*Math.PI);
                    }

                    if (state == STATE.FREE) {
                        sticks[i].color = COLOR.BLUE;
                    }

                    double factor = -dis/(affect_count*unit_angle)+2;
                    sticks[i].ex = (float) (sticks[i].sx + (sticks[i].ex - sticks[i].sx) * factor);
                    sticks[i].ey = (float) (sticks[i].sy + (sticks[i].ey - sticks[i].sy) * factor);
                }

                if (state == STATE.COUNTDOWN) {
                    if (angle_in_2PI > 3*Math.PI/2 && angle_in_2PI <= 2 * Math.PI) {
                        if (unit_angle * i <= angle_in_2PI && unit_angle * i >= 3*Math.PI/2) {
                            sticks[i].color = COLOR.RED;
                        }
                    } else {
                        if (unit_angle * i <= angle_in_2PI || unit_angle * i >= 3*Math.PI/2) {
                            sticks[i].color = COLOR.RED;
                        }
                    }
                }

            }
        }
        public void drawSelf(Canvas canvas, Paint p) {
            p.setStrokeWidth(minInWidthAndHeight/250);
            for (LittleStick stick : sticks) {
                stick.drawSelf(canvas, p);
            }
            p.setStrokeWidth(0);
        }
        public void adaptWindow(float windowWidth, float windowHeight) {
            this.boundWidth = windowWidth;
            this.boundHeight = windowHeight;
            this.minInWidthAndHeight = windowWidth < windowHeight ? windowWidth : windowHeight;
        }
    }

    // 摇晃判断器
    private class ShakeJudger {

        private int mode = 0;  // 1 for X 2 for Y and 0 for both

        private float shakeAccThreshold = 0;
        private float shakeTimeThreshold = 0;

        private long startShakeMills = 0;

        private boolean duringShakeFlag = false;
        private int duringShakeLock = 0;

        private boolean haveShake = false;

        public ShakeJudger(float shakeAccThreshold, float shakeTimeThreshold, int mode) {
            this.shakeAccThreshold = shakeAccThreshold;
            this.shakeTimeThreshold = shakeTimeThreshold;
            this.mode = mode;
        }
        public boolean haveShake() { return haveShake; }
        public void judging(float absAccX, float absAccY) {
            if (haveShake) return;

//            Log.i("ShakeJudger", "judging");
            if (mode == 1) {
                if (absAccX > shakeAccThreshold) {
                    duringShakeLock = 0;
                    if (!duringShakeFlag) {
                        startShake();
                    }
                    else {
                        if (System.currentTimeMillis() - startShakeMills > shakeTimeThreshold) {
                            confirmShake();
                        }
                    }
                } else {
                    duringShakeLock++;
                    if (duringShakeLock > 10) {
                        reset();
                    }
                }
            }
            else {
                // no implementation
            }
        }

        private void reset() {
            startShakeMills = 0;

            duringShakeFlag = false;
            duringShakeLock = 0;

            haveShake = false;
        }

        private void confirmShake() {
            Log.i("ShakeJudger", "confirmShake");
            haveShake = true;
        }

        private void startShake() {
            Log.i("ShakeJudger", "startShake");
            startShakeMills = System.currentTimeMillis();
            duringShakeFlag = true;
        }
    }

    // eventListeners

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }

            //获取加速度数值，以下三个值为重力分量在设备坐标的分量大小
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

//            Log.e("msg", " -x= "+(-x)+" y= "+ y +" z= "+z);

            float absx = Math.abs(x);
            float absy = Math.abs(y);
            float absz = Math.abs(z);

            float convertedX = -x;
            float convertedY = y;
            float convertedZ = z;

            if (absx < 1) {
                convertedX = 0;
            }
            if (absy < 1) {
                convertedY = 0;
            }

            float factor = getWidth() / 20;

            // 在重力影响下运动
            if (gravityEnable) {
                setAcc(convertedX / (1000 / factor), convertedY / (1000 / factor));
            }

            // 触屏状态下摇晃启动/关闭计时
            if (defaultStartCountDownEnable) {
                if (isTouching()) {
                    shakeJudgerWithTouch.judging(absx, absy);
                    if (shakeJudgerWithTouch.haveShake()) {

                        if (state() == TimerView.STATE.FREE) {
                            startCountDown(countDownInMillis);
                        } else if (state() == TimerView.STATE.COUNTDOWN) {
                            cancelCountDown();
                        }

                        shakeJudgerWithTouch.reset();
                    }
                } else {
                    shakeJudgerWithTouch.reset();
                }
            }

            if (state() != TimerView.STATE.FREE) {
                return;
            }

            float shakeThreshold = 13;

            // 摇晃使小球剧烈运动
            if (shakeEnable) {
                if (absx > shakeThreshold || absy > shakeThreshold) {
                    shakeEndLock = 0;

                    if (absx > shakeThreshold) {
                        if (absx > maxShakeX) {
                            maxShakeX = absx;
                        }

                        float rushV = (maxShakeX - shakeThreshold) * factor / 2;
                        if (rushV > (15 - shakeThreshold) * factor / 2) {
                            rushV = (15 - shakeThreshold) * factor / 2;
                        }

                        float rushX = convertedX > 0 ? rushV : -rushV;
                        float scaler = convertedY / convertedX;
                        float rushY = rushX * scaler;
                        rushXY(rushX, rushY);
                    } else if (absy > shakeThreshold) {
                        if (absy > maxShakeY) {
                            maxShakeY = absy;
                        }

                        float rushV = (maxShakeY - shakeThreshold) * factor / 2;
                        if (rushV > (15 - shakeThreshold) * factor / 2) {
                            rushV = (15 - shakeThreshold) * factor / 2;
                        }

                        float rushY = convertedY > 0 ? rushV : -rushV;
                        float scaler = convertedX / convertedY;
                        float rushX = rushY * scaler;
                        rushXY(rushX, rushY);
                    }
                } else {
                    shakeEndLock++;
                    if (shakeEndLock >= 5) {
                        maxShakeX = 0;
                        maxShakeY = 0;
                        shakeEndLock = 0;
//                        Log.i("shake", "after shake");
                    }
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    // interfaces

    public interface OnStartCountDownListener {
        void onStartCountDown(View v);
    }
    public interface OnEndCountDownListener {
        void onEndCountDown(View v);
    }
    public interface OnCancelCountDownListener {
        void onCancelCountDown(View v);
    }
    public interface OnBallClickListener {
        void onBallClick();
    }
    public interface OnBallLongClickListener {
        void onBallLongClick();
    }
    public interface OnStartInteractiveSettingListener {
        void onStartInteractiveSetting(View v);
    }
    public interface OnConfirmInteractiveSettingListener {
        void onConfirmInteractiveSetting(View v);
    }
    public interface OnCancelInteractiveSettingListener {
        void onCancelInteractiveSetting(View v);
    }

}
