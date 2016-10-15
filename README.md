# TimerView
a timer which extends a View
## Screen Shoot

## Usage
copy the TimerView.java into your project. Then derectly use TimerView element in xml like this:
```
    <com.ym.littleshape.TimerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/timer_view"
        android:padding="0dp" />
```
where com.ym.littleshape is your package name

and then in java:
```
        timerView = (TimerView)findViewById(R.id.timer_view);
```
